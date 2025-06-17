package com.doth.selector.coordinator.supports.sqlgenerator.tool;

import com.doth.selector.anno.DependOn;
import com.doth.selector.anno.Id;
import com.doth.selector.anno.Join;
import com.doth.selector.anno.OneToOne;
import com.doth.selector.common.exception.NonPrimaryKeyException;
import com.doth.selector.common.testbean.join2.Employee;
import com.doth.selector.common.testbean.join3.User;
import com.doth.selector.common.util.AnnoNamingConvertUtil;
import com.doth.selector.common.util.NamingConvertUtil;
import com.doth.selector.dto.DTOSelectFieldsListFactory;
import com.doth.selector.executor.supports.builder.ConditionBuilder;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class AutoQueryGenerator {
    private static final String MAIN_ALIAS = "t0";
    public static final int MAX_JOIN_LENGTH = 10;

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
            dtoPrefixes = dtoSelectPaths.stream()
                    .map(p -> p.substring(0, p.indexOf('.')))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
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
        parseEntity(originalEntity, MAIN_ALIAS, new HashSet<>());
        return "select " + String.join(", ", selectList)
                + "\nfrom " + getTableName(originalEntity) + " " + MAIN_ALIAS
                + "\n" + String.join("\n", joinClauses);
    }

    private void parseEntity(Class<?> entity, String alias, Set<Field> ancestorJoins) {
        if (joinLevel > MAX_JOIN_LENGTH) {
            throw new RuntimeException("关联层级过深: " + entity.getSimpleName());
        }
        if (!processedEntities.add(entity)) {
            boolean canBreak = ancestorJoins.stream().anyMatch(f -> f.isAnnotationPresent(OneToOne.class));
            if (!canBreak) {
                throw new RuntimeException("检测到未标注 @OneToOne 的循环引用: " + entity.getSimpleName());
            }
            return;
        }
        for (Field field : entity.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Join.class)) {
                String nextAlias = "t" + joinLevel;
                if (!dtoMode || dtoPrefixes.contains(nextAlias) || conditionPrefixes.contains(nextAlias)) {
                    joinLevel++;
                    Set<Field> newAncestors = new HashSet<>(ancestorJoins);
                    newAncestors.add(field);
                    Join join = field.getAnnotation(Join.class);
                    if (!dtoMode)
                        selectList.add(alias + "." + NamingConvertUtil.camel2SnakeCase(join.fk()));

                    Class<?> target = field.getType();
                    Field pk = Arrays.stream(target.getDeclaredFields())
                            .filter(f -> f.isAnnotationPresent(Id.class))
                            .findFirst()
                            .orElseThrow(() -> new NonPrimaryKeyException(target.getSimpleName() + " 缺少 @Id 注解"));
                    joinClauses.add(String.format("join %s %s ON %s.%s = %s.%s",
                            getTableName(target), nextAlias,
                            alias, join.fk(), nextAlias, pk.getName()));
                    parseEntity(target, nextAlias, newAncestors);
                }
            } else if (!dtoMode) {
                // 新增：无论任何级别，带 @Id 的字段都包含在 selectList 中
                selectList.add(alias + "." + NamingConvertUtil.camel2SnakeCase(field.getName()));
            }
        }
        processedEntities.remove(entity);
    }

    private String getTableName(Class<?> entity) {
        return AnnoNamingConvertUtil.camel2Snake(entity, entity.getSimpleName());
    }

    public static void main(String[] args) {
        String generated = generated(Employee.class);
        System.out.println("generated = \n" + generated);
    }
}
