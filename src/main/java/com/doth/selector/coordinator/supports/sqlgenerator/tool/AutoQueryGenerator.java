package com.doth.selector.coordinator.supports.sqlgenerator.tool;

import com.doth.selector.anno.Id;
import com.doth.selector.anno.Join;
import com.doth.selector.anno.OneToOne;
import com.doth.selector.anno.DependOn;
import com.doth.selector.common.exception.NonPrimaryKeyException;
import com.doth.selector.common.testbean.join.BaseEmpInfo;
import com.doth.selector.common.util.AnnoNamingConvertUtil;
import com.doth.selector.common.util.NamingConvertUtil;
import com.doth.selector.dto.DTOSelectFieldsListFactory;
import com.doth.selector.executor.supports.builder.ConditionBuilder;

import java.lang.reflect.Field;
import java.util.*;

public class AutoQueryGenerator {

    private static final String MAIN_ALIAS = "t0";
    public static final int MAX_JOIN_LENGTH = 5;

    private final Class<?> originalEntity;
    private final Set<String> dtoSelectPaths;  // 已包含别名前缀的完整路径，如 t0.id, t1.dep_name
    private final Set<String> conditionPrefixes;
    private final List<String> selectList = new ArrayList<>();
    private final List<String> joinClauses = new ArrayList<>();
    private final Set<Class<?>> processedEntities = new HashSet<>();
    private int joinLevel = 1;

    public static String generated(Class<?> entityClass) {
        return new AutoQueryGenerator(entityClass, null).generate();
    }

    public static String generated(Class<?> entityClass, ConditionBuilder<?> conditionBuilder) {
        return new AutoQueryGenerator(entityClass, conditionBuilder).generate();
    }

    /**
     * 构造时：
     * - 如果传入的 entityClass 标记了 @DependOn，则视为 DTO 模式
     *   * 从注解中获取原始实体类 class
     *   * 从 DTO 类名解析出 dtoId（SimpleName 中 $ 之后的部分）
     *   * 从工厂拿到带别名前缀的 select 字段路径列表，填充 dtoSelectPaths
     * - 否则正常模式，dtoSelectPaths 设为 null
     * - conditionPrefixes 一如既往由 ConditionBuilder 提供
     */
    private AutoQueryGenerator(Class<?> entityClass, ConditionBuilder<?> conditionBuilder) {
        Set<String> usedFieldPaths = conditionBuilder.getUsedFieldPaths();
        System.out.println("usedFieldPaths = " + usedFieldPaths);
        if (entityClass.isAnnotationPresent(DependOn.class)) {
            DependOn dep = entityClass.getAnnotation(DependOn.class);
            try {
                this.originalEntity = Class.forName(dep.clzPath());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("无法反射原始实体类: " + dep.clzPath(), e);
            }
            // 从 DTO 类名解析 dtoId: e.g., Employee$empSimpleDTO -> empSimpleDTO
            String simpleName = entityClass.getSimpleName();
            // 将首字母转小写
            simpleName = simpleName.substring(0, 1).toLowerCase(Locale.ROOT) + simpleName.substring(1);
            String dtoId = simpleName;
            // 强制加载 DTO 类，使其能够在静态代码块中注册 select 列
            try {
                Class.forName(entityClass.getName());
            } catch (ClassNotFoundException e) {
                // throw new RuntimeException("DTO 类未找到: " + dtoClassName, e);
            }
            System.out.println("dtoId = " + dtoId);
            // 从工厂取出完整的 select 列路径
            List<String> paths = DTOSelectFieldsListFactory.resolveSelectList(originalEntity, dtoId);
            if (paths.isEmpty()) {
                throw new RuntimeException("未在 DTOSelectFieldsListFactory 中找到对应 DTO 的 select 列: "
                        + originalEntity.getName() + "#" + dtoId);
            }
            this.dtoSelectPaths = new HashSet<>(paths);
        } else {
            this.originalEntity = entityClass;
            this.dtoSelectPaths = null;
        }
        this.conditionPrefixes = conditionBuilder != null
                ? conditionBuilder.extractJoinTablePrefixes()
                : Collections.emptySet();
    }

    private String generate() {
        if (dtoSelectPaths != null) {
            // 先填充 SELECT 列，已包含前缀
            selectList.addAll(dtoSelectPaths);
            // 再递归生成必须的 JOIN
            parseEntity(originalEntity, MAIN_ALIAS, new HashSet<>());
        } else {
            // 非 DTO 模式：逐字段收集 select，并生成 join
            parseEntity(originalEntity, MAIN_ALIAS, new HashSet<>());
        }
        String select = "select " + String.join(", ", selectList);
        String from = "from " + getTableName(originalEntity) + " " + MAIN_ALIAS;
        return select + "\n" + from + "\n" + String.join("\n", joinClauses);
    }

    /**
     * 解析实体：
     * - 检测层级是否超过 MAX_JOIN_LENGTH
     * - 检测循环引用（processedEntities）并依据 @OneToOne 决定是否继续
     * - 遍历字段：
     *   * 如果是 @Join：判断是否需要生成这个 JOIN（DTO 模式下，仅当 alias 存在于路径集合或 conditionPrefixes 才生成）
     *     并在需要时调用 handleJoin
     *   * 如果是非 @Join 且处于非 DTO 模式，则调用 handleField
     */
    private void parseEntity(Class<?> entity, String alias, Set<Field> joinFields) {
        if (joinFields.size() > MAX_JOIN_LENGTH) {
            throw new RuntimeException("关联层级过深，建议检查实体设计: " + entity.getSimpleName());
        }
        if (processedEntities.contains(entity)) {
            boolean breakable = joinFields.stream().anyMatch(f -> f.isAnnotationPresent(OneToOne.class));
            if (!breakable) {
                throw new RuntimeException("检测到未标注 @OneToOne 的循环引用: " + entity.getSimpleName());
            }
            return;
        }
        processedEntities.add(entity);

        for (Field field : entity.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Join.class)) {
                String nextAlias = "t" + joinLevel;
                boolean needJoin = false;
                if (dtoSelectPaths != null) {
                    // 当路径集中有字段以 nextAlias." 开头，说明这个分支的字段在 SELECT 中，需要 JOIN
                    String prefix = nextAlias + ".";
                    for (String path : dtoSelectPaths) {
                        if (path.startsWith(prefix)) {
                            needJoin = true;
                            break;
                        }
                    }
                    // 或者条件中需要该 alias
                    if (!needJoin && conditionPrefixes.contains(nextAlias)) {
                        needJoin = true;
                    }
                } else {
                    // 非 DTO 模式：总是生成所有 JOIN
                    needJoin = true;
                }
                if (needJoin) {
                    Set<Field> newJoinFields = new HashSet<>(joinFields);
                    newJoinFields.add(field);
                    handleJoin(field, alias, newJoinFields);
                }
            } else if (dtoSelectPaths == null) {
                // 非 DTO 模式下才添加普通字段
                handleField(field, alias);
            }
            // DTO 模式下，普通字段不在这里处理，因为已经在生成前填充了 selectList
        }
        processedEntities.remove(entity);
    }

    /**
     * 生成 JOIN 子句，并继续递归下游实体
     * @param field 当前带 @Join 的字段
     * @param alias 当前实体别名
     * @param joinFields 已访问的 join 字段，用于深度和循环检测
     */
    private void handleJoin(Field field, String alias, Set<Field> joinFields) {
        Join join = field.getAnnotation(Join.class);
        Class<?> refEntity = field.getType();
        String nextAlias = "t" + joinLevel++;
        // 获取目标表的主键列
        Field pk = getPKField(refEntity);
        joinClauses.add(String.format(
                "join %s %s ON %s.%s = %s.%s",
                getTableName(refEntity), nextAlias,
                alias, join.fk(),
                nextAlias, pk.getName()));
        parseEntity(refEntity, nextAlias, joinFields);
    }

    /**
     * 非 DTO 模式：处理普通字段，非主表主键，添加到 selectList
     */
    private void handleField(Field field, String alias) {
        if (!MAIN_ALIAS.equals(alias) && field.isAnnotationPresent(Id.class)) {
            return;
        }
        String column = NamingConvertUtil.camel2SnakeCase(field.getName());
        selectList.add(alias + "." + column);
    }

    private Field getPKField(Class<?> entityClass) {
        for (Field f : entityClass.getDeclaredFields()) {
            if (f.isAnnotationPresent(Id.class)) {
                return f;
            }
        }
        throw new NonPrimaryKeyException(entityClass.getSimpleName() + " 未找到主键字段，请使用 @Id 注解标记主键");
    }

    private String getTableName(Class<?> entity) {
        return AnnoNamingConvertUtil.camel2Snake(entity, entity.getSimpleName());
    }


    public static void main(String[] args) {
        String generated = generated(BaseEmpInfo.class);
        System.out.println("generated = " + generated);
    }
}
