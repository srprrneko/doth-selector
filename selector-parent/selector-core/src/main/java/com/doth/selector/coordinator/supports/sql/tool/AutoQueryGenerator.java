package com.doth.selector.coordinator.supports.sql.tool;

import com.doth.selector.anno.CycRel;
import com.doth.selector.anno.DependOn;
import com.doth.selector.anno.Join;
import com.doth.selector.anno.Pk;
import com.doth.selector.common.dto.DTOJoinInfo;
import com.doth.selector.common.dto.DTOJoinInfoFactory;
import com.doth.selector.common.dto.DTOSelectFieldsListFactory;
import com.doth.selector.common.dto.JoinDefInfo;
import com.doth.selector.common.exception.mapping.NonPrimaryKeyException;
import com.doth.selector.common.util.AnnoNamingConvertUtil;
import com.doth.selector.common.util.NamingConvertUtil;
import com.doth.selector.executor.supports.builder.ConditionBuilder;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * AutoQueryGenerator：自动生成 SQL 查询语句的核心工具类
 *
 * <p>
 * <strong>职责</strong>
 * <ol>
 *   <li>根据实体类和 ConditionBuilder 构建整套 sql 语句, 例 'SELECT + FROM + JOIN'</li>
 *   <li>支持 普通模式 与 DTO懒加载 模式的 SQL 生成</li>
 *   <li>利用咖啡因第三方库缓存 优化字段访问性能</li>
 *   <li>痛过预注册的 DTOJoinInfo, 实现普通模式和 dto模式join子句生成的 逻辑隔离</li>
 * </ol>
 *
 * <p>后续演进拓展设计模式, sql指纹</p>
 */
@Slf4j
public class AutoQueryGenerator {


    /**
     * 最大关联层级，防止无限递归
     */
    public static final int MAX_JOIN_LENGTH = 10;

    /**
     * 字段缓存：减少反射调用开销
     */
    private static final Cache<Class<?>, Field[]> FIELD_CACHE;

    static {
        FIELD_CACHE = Caffeine.newBuilder().maximumSize(200).expireAfterAccess(1, TimeUnit.HOURS).build();
    }


    // !!======================== 实例字段上下文 - 开始 =========================!!
    /**
     * 原始实体类或 DTO 关联的源实体
     */
    private final Class<?> prototype;

    /**
     * 是否为 DTO 模式
     */
    private final boolean isDtoMod;

    /**
     * DTO name
     */
    private final String dtoName;

    /**
     * DTO 查询列列表
     */
    private final List<String> dtoSelectList;

    /**
     * DTO 模式下的前缀集合 >> t0、t1..
     */
    private final Set<String> dtoPfx;

    /**
     * conditionBuilder 中提取的前缀集合, 来源于 extractJoinTablePrefixes() 方法, 同样作用于dto模式
     */
    private final Set<String> condPfx;


    /**
     * 最终的查询列列表
     */
    private final List<String> selectList = new ArrayList<>();

    /**
     * 最终 join 子句列表
     */
    private final List<String> joinClauses = new ArrayList<>();

    /**
     * 记录已处理实体, 防止循环依赖
     */
    private final Set<Class<?>> alreadyPrecessed = new HashSet<>();


    /**
     * 当前递归层级, 作用生成 t'N'.name
     */
    private int joinLevel = 1;

    /**
     * 主表别名
     */
    private static final String MAIN_ALIAS = "t0";
    // !!======================== 实例字段上下文 - 结束 =========================!!


    /**
     * API-简单工厂
     * <strong>1.无条件/或条件由外部控制</strong>
     *
     * @return 生成的 SQL
     */
    public static String generated(Class<?> targetClz) {
        return generated(targetClz, null);
    }

    /**
     * <strong>2.带条件</strong>
     *
     * @return 生成的 SQL
     */
    public static String generated(Class<?> targetClz, ConditionBuilder<?> cond) {
        return new AutoQueryGenerator(targetClz, cond).generate();
    }



    /**
     * 初始化
     * <strong>构造函数, 两分支</strong>
     * <p>>> 带有 @DependOn 注解区分DTO 模式，</p>
     * <ol>
     *     <li>dto模式下会懒加载join子句</li>
     *     <li>保留条件字段的join子句</li>
     * </ol>
     * <p>否则为普通模式</p>
     * <br>
     * <strong>主要逻辑</strong>
     * <p>1. 先加载原实体sql</p>
     * <p>2. 拦截非dto字段</p>
     * <p>3. 拦截不存在的层级</p>
     *
     * @param targetClz 实体类或标注 @DependOn 的 DTO 类
     * @param cond      条件构建器
     */
    private AutoQueryGenerator(Class<?> targetClz, ConditionBuilder<?> cond) {
        DependOn dep = targetClz.getAnnotation(DependOn.class);
        if (dep != null) {
            isDtoMod = true;
            try {
                // 获取原实体
                prototype = Class.forName(dep.clzPath());
                // 确保 DTO 类执行静态块
                Class.forName(targetClz.getName(), true, targetClz.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("加载类失败: " + e.getMessage(), e);
            }
            dtoName = Introspector.decapitalize(targetClz.getSimpleName());
            dtoSelectList = resolveDtoSelectList(prototype, dtoName, targetClz.getSimpleName());
            dtoPfx = extractPrefixes(dtoSelectList);
        } else {
            isDtoMod = false;
            prototype = targetClz;
            dtoName = null;
            dtoSelectList = Collections.emptyList();
            dtoPfx = Collections.emptySet();
        }
        condPfx = (cond != null) ? cond.extractJoinTablePrefixes() : Collections.emptySet();
    }


    //!!================  DTO模式辅助方法 - 开始 ================!!

    /**
     * <strong>解析 DTO Select 字段列列表</strong>
     * <p>从 DTOSelectFieldsListFactory 中获取已注册的 select 列</p>
     *
     * @param origin     源实体类
     * @param dtoId      工厂注册时使用的 dtoId 列名
     * @param simpleName 大写首字母形式
     * @return 已注册的 select 路径列表
     */
    private List<String> resolveDtoSelectList(Class<?> origin, String dtoId, String simpleName) {
        List<String> paths = DTOSelectFieldsListFactory.resolveSelectList(origin, dtoId);
        if (paths.isEmpty()) {
            paths = DTOSelectFieldsListFactory.resolveSelectList(origin, simpleName);
            if (!paths.isEmpty()) {
                log.error("仅找到大写首字母形式的注册列: {}", origin.getName() + "#" + simpleName);
            }
        }
        if (paths.isEmpty()) throw new RuntimeException("未找到 DTO 查询列: " + origin.getName() + "#" + dtoId + " 或 静态代码块未正确触发");
        return paths;
    }

    /**
     * <strong>提取tN 路径前缀</strong>
     *
     * @param paths Select 路径列表
     * @return 前缀集合
     */
    private Set<String> extractPrefixes(List<String> paths) {
        Set<String> set = new LinkedHashSet<>();
        for (String path : paths) {
            int dot = path.indexOf('.');
            if (dot > 0) {
                set.add(path.substring(0, dot));
            }
        }
        return set;
    }
    //!!================  DTO模式辅助方法 - 结束 ================!!


    // !!======================== 核心sql生成方法 - 开始=========================!!

    /**
     * <strong>核心：根据模式分支生成 SQL</strong>
     * <p>普通模式与 DTO 模式共用接口，内部根据条件前缀与预注册信息决定使用反射遍历还是预定义 JoinDefInfo。</p>
     *
     * @return 完整的 SELECT + FROM + JOIN 语句
     */
    private String generate() {
        // DTO 模式下先填充 selectList
        if (isDtoMod) {
            selectList.addAll(dtoSelectList);
        }

        DTOJoinInfo joinInfo = isDtoMod ? DTOJoinInfoFactory.getJoinInfo(prototype, dtoName) : null;

        System.out.println("joinInfo = " + joinInfo);
        // 是否需要走普通模式
        boolean needReflect = condPfx.stream().anyMatch(pfx -> !dtoPfx.contains(pfx));

        if (joinInfo != null && !needReflect) {
            // DTO 模式 & 已注册走预定义 JoinInfo
            for (JoinDefInfo jdf : joinInfo.getJoinDefInfos()) {
                log.info("使用 DTO 预注册关联: {}", jdf);
                joinClauses.add(String.format("join %s %s ON %s.%s = " + "%s.%s", jdf.getWhereTable(), jdf.getAlias(), jdf.getMainTId(), jdf.getFk(),

                        jdf.getAlias(), jdf.getPk()));
            }
        } else {
            log.warn("条件字段不存在dto查询列工厂!");
            // 普通模式或 条件字段不存在dto查询列工厂则 走反射遍历
            parseEntity(prototype, MAIN_ALIAS, Collections.emptySet());
        }

        // 返回收集后的结果
        return collectResult();
    }

    public String collectResult() {
        StringBuilder sb = new StringBuilder("select ");
        for (int i = 0; i < selectList.size(); i++) {
            if (i > 0) sb.append(", ");

            sb.append(selectList.get(i));
        }

        sb.append("\nfrom ").append(getTableName(prototype)).append(" ").append(MAIN_ALIAS);
        for (String clause : joinClauses) {
            sb.append("\n").append(clause);
        }
        return sb.append("\n").toString();
    }
    // !!======================== 核心sql生成方法 - 结束 =========================!!


    // !!======================== 实体反射遍历方法 =========================!!

    /**
     * <strong>递归解析实体字段</strong>
     * <p>遍历带 @Join 注解的字段生成对应的 JOIN 子句，并在普通模式下收集 select 列。</p>
     *
     * @param curClz        当前实体类
     * @param curAlias      当前别名（如 t0、t1、t2…）
     * @param ancestorJoins 上层已遍历的关联字段集合，用于循环检测
     */
    private void parseEntity(Class<?> curClz, String curAlias, Set<Field> ancestorJoins) {
        // 1. 嵌套层级防御
        if (joinLevel > MAX_JOIN_LENGTH) throw new RuntimeException("关联层级过深: " + curClz.getSimpleName());
        // 2. 循环检测依赖
        checkRefCycle(curClz, ancestorJoins);

        Field[] fields = getCachedFields(curClz);
        for (Field field : fields) {
            // 内联对象解析
            if (field.isAnnotationPresent(Join.class)) {
                handleJoin(field, curAlias, ancestorJoins);
            } else if (!isDtoMod) {
                String formatFName = NamingConvertUtil.camel2Snake(field.getName());
                selectList.add(curAlias + "." + formatFName);
            }
        }
        alreadyPrecessed.remove(curClz);
    }

    /**
     * 处理单个 @Join 字段的 JOIN 生成、字段收集与递归
     */
    private void handleJoin(Field field,
                            String curAlias,
                            Set<Field> ancestorJoins) {

        Class<?> target = field.getType(); // 获取从属实体clz

        // 拦截一对一场景下的join子句生成
        boolean isOneToOne = field.isAnnotationPresent(CycRel.class);
        if (alreadyPrecessed.contains(target) && isOneToOne) {
            return; // OneTo1Breaker 安全跳过
        }

        String nextAlias = "t" + joinLevel;

        Set<Field> newAncestors = new HashSet<>(ancestorJoins);
        newAncestors.add(field);

        // 非dto模式自动加入外键列查询
        if (!isDtoMod) {
            selectList.add(curAlias + "."
                    + NamingConvertUtil.camel2Snake(
                    field.getAnnotation(Join.class).fk()));
        }

        Field pk = getPk2Field(target);
        joinClauses.add(String.format(
                "join %s %s ON %s.%s = %s.%s",
                getTableName(target),
                nextAlias,
                curAlias,
                field.getAnnotation(Join.class).fk(),

                nextAlias,
                NamingConvertUtil.camel2Snake(pk.getName())
        ));

        joinLevel++;
        parseEntity(target, nextAlias, newAncestors);
    }

    /**
     * 循环依赖检测：
     * - 尝试把 curClz 加入到 alreadyPrecessed；
     * - 如果已存在且所有 ancestorJoins 上的字段都没标 @OneTo1Breaker，则视为非法循环，抛异常；
     * - 如果遇到 @OneTo1Breaker，则安全中断当前分支。
     */
    private void checkRefCycle(Class<?> curClz, Set<Field> ancestorJoins) {
        if (!alreadyPrecessed.add(curClz)) {
            for (Field f : ancestorJoins) {
                if (f.isAnnotationPresent(CycRel.class)) {
                    // OneTo1Breaker 情况下可安全终止
                    return;
                }
            }
            throw new RuntimeException("检测到未标注 @OneTo1Breaker 的循环引用: " + curClz.getSimpleName());
        }
    }


    // !!======================== 辅助工具方法 =========================!!

    /**
     * <strong>找并返回主键字段</strong>
     * <p>找类中带 @Pk 注解的字段</p>
     *
     * @param clazz 实体类
     * @return 带 @Pk 注解的字段
     * @throws NonPrimaryKeyException 若未找到主键注解
     */
    private Field getPk2Field(Class<?> clazz) {
        for (Field field : getCachedFields(clazz)) {
            if (field.isAnnotationPresent(Pk.class)) return field;
        }
        throw new NonPrimaryKeyException(clazz.getSimpleName() + " 缺少 @Pk 注解");
    }

    /**
     * <strong>获取缓存字段数组</strong>
     * <p>利用 Caffeine 缓存减少反射调用。</p>
     *
     * @param clazz 实体类
     * @return 字段数组
     */
    private Field[] getCachedFields(Class<?> clazz) {
        return FIELD_CACHE.get(clazz, clz -> {
            Field[] fs = clz.getDeclaredFields();
            for (Field f : fs) f.setAccessible(true);
            return fs;
        });
    }


    private String getTableName(Class<?> entity) {
        return AnnoNamingConvertUtil.camel2Snake(entity, entity.getSimpleName());
    }

}
