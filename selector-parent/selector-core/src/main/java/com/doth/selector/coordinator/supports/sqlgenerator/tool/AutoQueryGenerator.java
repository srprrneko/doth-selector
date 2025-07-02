package com.doth.selector.coordinator.supports.sqlgenerator.tool;

import com.doth.selector.anno.DependOn;
import com.doth.selector.anno.Id;
import com.doth.selector.anno.Join;
import com.doth.selector.anno.OneToOne;
import com.doth.selector.common.dto.DTOJoinInfo;
import com.doth.selector.common.dto.DTOJoinInfoFactory;
import com.doth.selector.common.dto.DTOSelectFieldsListFactory;
import com.doth.selector.common.dto.JoinDef;
import com.doth.selector.common.exception.NonPrimaryKeyException;
import com.doth.selector.common.util.AnnoNamingConvertUtil;
import com.doth.selector.common.util.NamingConvertUtil;
import com.doth.selector.executor.supports.builder.ConditionBuilder;
import com.doth.selector.supports.testbean.join2.Employee;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AutoQueryGenerator {

    private static final String MAIN_ALIAS = "t0";
    public static final int MAX_JOIN_LENGTH = 10;

    private static final Cache<Class<?>, Field[]> FIELD_CACHE;

    static {
        FIELD_CACHE = Caffeine.newBuilder()
                .maximumSize(200)               // 缓存上限
                .expireAfterAccess(1, TimeUnit.HOURS)
                .build();
    }

    private final Class<?> originalEntity;
    private final boolean dtoMode;
    private final String dtoName;                     // 新增：存储 DTO 名称（decap）
    private final List<String> dtoSelectPaths;
    private final Set<String> dtoPrefixes;
    private final Set<String> conditionPrefixes;
    private final List<String> selectList = new ArrayList<>();
    private final List<String> joinClauses = new ArrayList<>();
    private final Set<Class<?>> processedEntities = new HashSet<>();
    private int joinLevel = 1;

    public static String generated(Class<?> entityClass) {
        return generated(entityClass, null);
    }

    public static String generated(Class<?> entityClass, ConditionBuilder<?> conditionBuilder) {
        return new AutoQueryGenerator(entityClass, conditionBuilder).generate();
    }

    private AutoQueryGenerator(Class<?> entityClass, ConditionBuilder<?> conditionBuilder) {
        DependOn dep = entityClass.getAnnotation(DependOn.class);
        if (dep != null) {
            // DTO 模式
            dtoMode = true;
            try {
                originalEntity = Class.forName(dep.clzPath());
                log.info("dto model-origin entity: {}", originalEntity);
                // 确保 DTO 类能被加载
                Class.forName(entityClass.getName(), true, entityClass.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("加载类失败: " + e.getMessage(), e);
            }
            // decap 形式即 Factory 注册时使用的 key
            String decap = Introspector.decapitalize(entityClass.getSimpleName());
            this.dtoName = decap;
            dtoSelectPaths = resolveDtoSelectPaths(originalEntity, decap, entityClass.getSimpleName());
            dtoPrefixes = extractPrefixes(dtoSelectPaths);
        } else {
            // 普通模式
            dtoMode = false;
            originalEntity = entityClass;
            this.dtoName = null;
            dtoSelectPaths = Collections.emptyList();
            dtoPrefixes = Collections.emptySet();
        }
        conditionPrefixes = conditionBuilder != null
                ? conditionBuilder.extractJoinTablePrefixes()
                : Collections.emptySet();
    }

    /**
     * 从已注册的 DTOSelectFieldsListFactory 中解析 select 列。
     */
    private List<String> resolveDtoSelectPaths(Class<?> origin, String decap, String simpleName) {
        List<String> paths = DTOSelectFieldsListFactory.resolveSelectList(origin, decap);
        if (paths.isEmpty()) {
            paths = DTOSelectFieldsListFactory.resolveSelectList(origin, simpleName);
            if (!paths.isEmpty()) {
                System.err.println("仅找到大写首字母形式的注册列: " + origin.getName() + "#" + simpleName);
            }
        }
        if (paths.isEmpty()) {
            throw new RuntimeException("未找到 DTO 查询列: "
                    + origin.getName() + "#" + decap + " 或 " + simpleName);
        }
        return paths;
    }

    /**
     * 从 select 列中提取所有 tN 前缀，用于控制哪些关联要被遍历（DTO 模式下）。
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

    /**
     * 核心：根据模式分支，生成 SELECT + FROM + JOIN 语句。
     */
    private String generate() {
        // DTO 模式下先把 selectList 填好
        if (dtoMode) {
            selectList.addAll(dtoSelectPaths);
        }

        // 尝试获取预注册的 JoinInfo（只有 DTO 模式才会注册）
        DTOJoinInfo joinInfo = (dtoMode && dtoName != null)
                ? DTOJoinInfoFactory.getJoinInfo(originalEntity, dtoName)
                : null;


        if (joinInfo != null) {
            // DTO 模式 & 已注册：直接用预定义的 JoinDef 列表
            for (JoinDef jd : joinInfo.getJoinDefs()) {
                log.info("dto join-info: {}", joinInfo);
                log.info("joinDef info: {}", jd);

                // String correctAlias = "";
                // if (jd.get)
                joinClauses.add(String.format(
                        "join %s %s ON %s.%s = %s.%s",
                        jd.getRelationTable(),
                        jd.getAlias(),
                        jd.getParentId(),
                        jd.getForeignKeyColumn(),

                        jd.getAlias(),
                        jd.getPrimaryKeyColumn()
                ));

            }
        } else {
            // 普通模式或 DTO 模式下未注册：走原有的反射遍历逻辑
            parseEntity(originalEntity, MAIN_ALIAS, Collections.emptySet());
        }

        // 拼接最终 SQL
        StringBuilder sb = new StringBuilder("select ");
        for (int i = 0; i < selectList.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(selectList.get(i));
        }
        sb.append("\nfrom ").append(getTableName(originalEntity)).append(" ").append(MAIN_ALIAS);
        for (String clause : joinClauses) {
            sb.append("\n").append(clause);
        }
        return sb.append("\n").toString();
    }

    /**
     * 原有递归反射逻辑，不做任何修改。
     */
    private void parseEntity(Class<?> entity, String alias, Set<Field> ancestorJoins) {
        if (joinLevel > MAX_JOIN_LENGTH) {
            throw new RuntimeException("关联层级过深: " + entity.getSimpleName());
        }
        if (!processedEntities.add(entity)) {
            for (Field f : ancestorJoins) {
                if (f.isAnnotationPresent(OneToOne.class)) {
                    return;
                }
            }
            throw new RuntimeException("检测到未标注 @OneToOne 的循环引用: " + entity.getSimpleName());
        }

        Field[] fields = getCachedFields(entity);
        for (Field field : fields) {
            if (field.isAnnotationPresent(Join.class)) {
                Class<?> target = field.getType();
                boolean isOneToOne = field.isAnnotationPresent(OneToOne.class);
                if (processedEntities.contains(target) && isOneToOne) {
                    continue;
                }

                String nextAlias = "t" + joinLevel;
                boolean usedInDtoOrCond = dtoPrefixes.contains(nextAlias)
                        || conditionPrefixes.contains(nextAlias);
                if (!dtoMode || usedInDtoOrCond) {
                    Set<Field> newAncestors = new HashSet<>(ancestorJoins);
                    newAncestors.add(field);

                    if (!dtoMode) {
                        Join join = field.getAnnotation(Join.class);
                        selectList.add(alias + "." + NamingConvertUtil.camel2SnakeCase(join.fk()));
                    }

                    Field pk = findPrimaryKey(target);
                    joinClauses.add(String.format(
                            "join %s %s ON %s.%s = %s.%s",
                            getTableName(target), nextAlias,
                            alias, field.getAnnotation(Join.class).fk(),
                            nextAlias, NamingConvertUtil.camel2SnakeCase(pk.getName())
                    ));

                    joinLevel++;
                    parseEntity(target, nextAlias, newAncestors);
                }
            } else if (!dtoMode) {
                selectList.add(alias + "." + NamingConvertUtil.camel2SnakeCase(field.getName()));
            }
        }
        processedEntities.remove(entity);
    }

    private Field findPrimaryKey(Class<?> clazz) {
        Field[] fields = getCachedFields(clazz);
        for (Field field : fields) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        throw new NonPrimaryKeyException(clazz.getSimpleName() + " 缺少 @Id 注解");
    }

    private Field[] getCachedFields(Class<?> clazz) {
        return FIELD_CACHE.get(clazz, clz -> {
            Field[] fs = clz.getDeclaredFields();
            for (Field f : fs) {
                f.setAccessible(true);
            }
            return fs;
        });
    }

    private String getTableName(Class<?> entity) {
        return AnnoNamingConvertUtil.camel2Snake(entity, entity.getSimpleName());
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 50000; i++) {

            String generatedSql = generated(Employee.class);
        }
        // System.out.println("generated = " + generatedSql);
        long end = System.currentTimeMillis();
        System.out.println("(耗时 ms): " + (end - start));
    }
}
