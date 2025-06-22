package com.doth.selector.coordinator.supports.sqlgenerator.tool;

import com.doth.selector.anno.DependOn;
import com.doth.selector.anno.Id;
import com.doth.selector.anno.Join;
import com.doth.selector.anno.OneToOne;
import com.doth.selector.common.exception.NonPrimaryKeyException;
import com.doth.selector.common.util.AnnoNamingConvertUtil;
import com.doth.selector.common.util.NamingConvertUtil;
import com.doth.selector.common.dto.DTOSelectFieldsListFactory;
import com.doth.selector.executor.supports.builder.ConditionBuilder;
// import com.doth.selector.supports.testbean.join.BaseEmpInfo;
// import com.doth.selector.supports.testbean.join.Employee;
import com.doth.selector.supports.testbean.join2.BaseEmpInfo;
import com.doth.selector.supports.testbean.join2.Employee;
import com.doth.selector.supports.testbean.join3.StudentInfo;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class AutoQueryGenerator {
    private static final String MAIN_ALIAS = "t0";
    public static final int MAX_JOIN_LENGTH = 10;

    private static Cache<Class<?>, Field[]> FIELD_CACHE = null;

    static {
        FIELD_CACHE = Caffeine.newBuilder()
                .maximumSize(200) // 根据实体数量设置 200 够用了
                .expireAfterAccess(1, TimeUnit.HOURS)
                .build();

    }

    private final Class<?> originalEntity;
    private final boolean dtoMode;
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
            dtoMode = true;
            try {
                originalEntity = Class.forName(dep.clzPath());
                Class.forName(entityClass.getName(), true, entityClass.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("加载类失败: " + e.getMessage(), e);
            }

            String decap = Introspector.decapitalize(entityClass.getSimpleName());
            dtoSelectPaths = resolveDtoSelectPaths(originalEntity, decap, entityClass.getSimpleName());
            dtoPrefixes = extractPrefixes(dtoSelectPaths);
        } else {
            dtoMode = false;
            originalEntity = entityClass;
            dtoSelectPaths = Collections.emptyList();
            dtoPrefixes = Collections.emptySet();
        }

        conditionPrefixes = conditionBuilder != null
                ? conditionBuilder.extractJoinTablePrefixes()
                : Collections.emptySet();
    }

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

    private List<String> resolveDtoSelectPaths(Class<?> origin, String decap, String simpleName) {
        List<String> paths = DTOSelectFieldsListFactory.resolveSelectList(origin, decap);
        if (paths.isEmpty()) {
            paths = DTOSelectFieldsListFactory.resolveSelectList(origin, simpleName);
            if (!paths.isEmpty()) {
                System.err.println("仅找到大写首字母形式的注册列: " + origin.getName() + "#" + simpleName);
            }
        }
        if (paths.isEmpty()) {
            throw new RuntimeException("未找到 DTO 查询列: " + origin.getName() + "#" + decap + " 或 " + simpleName);
        }
        return paths;
    }

    private String generate() {
        if (dtoMode) {
            selectList.addAll(dtoSelectPaths);
        }
        parseEntity(originalEntity, MAIN_ALIAS, Collections.emptySet());

        // 构造 SQL 字符串，使用 StringBuilder 替代 + 拼接（但此处影响不大）
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

    private void parseEntity(Class<?> entity, String alias, Set<Field> ancestorJoins) {
        if (joinLevel > MAX_JOIN_LENGTH) {
            throw new RuntimeException("关联层级过深: " + entity.getSimpleName());
        }
        // cycle 检测：如果重新遇到同一个 entity，且在祖先字段里有 @OneToOne，就直接返回
        if (!processedEntities.add(entity)) {
            for (Field f : ancestorJoins) {
                if (f.isAnnotationPresent(OneToOne.class)) {
                    // OneToOne 循环，跳过后续所有处理
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

                // **新加**：如果目标类已处理过，且本字段标注了 @OneToOne，则跳过
                if (processedEntities.contains(target) && isOneToOne) {
                    continue;
                }

                String nextAlias = "t" + joinLevel;
                boolean usedInDtoOrCond = dtoPrefixes.contains(nextAlias)
                        || conditionPrefixes.contains(nextAlias);
                if (!dtoMode || usedInDtoOrCond) {
                    // 把本 field 也当成祖先，传递给子层级用于循环检测
                    Set<Field> newAncestors = new HashSet<>(ancestorJoins);
                    newAncestors.add(field);

                    // select 外键列
                    if (!dtoMode) {
                        Join join = field.getAnnotation(Join.class);
                        selectList.add(alias + "." + NamingConvertUtil.camel2SnakeCase(join.fk()));
                    }

                    // join 子表
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
                // 普通字段直接 select
                selectList.add(alias + "." + NamingConvertUtil.camel2SnakeCase(field.getName()));
            }
        }

        // 本层遍历完成，移除标记
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
            Field[] fields = clz.getDeclaredFields();
            for (Field field : fields) field.setAccessible(true);
            return fields;
        });
    }

    private String getTableName(Class<?> entity) {
        return AnnoNamingConvertUtil.camel2Snake(entity, entity.getSimpleName());
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        // for (int i = 0; i < 50000; i++) {
            String generated = generated(BaseEmpInfo.class);
        // }
        System.out.println("generated = " + generated);
        long end = System.currentTimeMillis();
        System.out.println("(耗时 ms): " + (end - start));
    }
}
