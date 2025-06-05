package com.doth.selector.coordinator.supports.sqlgenerator.tool;

import com.doth.selector.anno.DependOn;
import com.doth.selector.anno.Id;
import com.doth.selector.anno.Join;
import com.doth.selector.anno.OneToOne;
import com.doth.selector.common.exception.NonPrimaryKeyException;
// import com.doth.selector.common.testbean.join.BaseEmpInfo;
import com.doth.selector.common.testbean.join.BaseEmpInfo;
import com.doth.selector.common.util.AnnoNamingConvertUtil;
import com.doth.selector.common.util.NamingConvertUtil;
import com.doth.selector.dto.DTOSelectFieldsListFactory;
import com.doth.selector.executor.supports.builder.ConditionBuilder;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.util.*;

public class AutoQueryGenerator {

    private static final String MAIN_ALIAS = "t0";
    public static final int MAX_JOIN_LENGTH = 5;

    private final Class<?> originalEntity;
    private final List<String> dtoSelectPaths;      // 保留顺序的字段路径
    private final Set<String> dtoPrefixes;         // DTO select 字段中提取的所有 tN 前缀
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

    private AutoQueryGenerator(Class<?> entityClass, ConditionBuilder<?> conditionBuilder) {
        if (entityClass.isAnnotationPresent(DependOn.class)) {
            DependOn dep = entityClass.getAnnotation(DependOn.class);
            try {
                this.originalEntity = Class.forName(dep.clzPath());
                Class.forName(entityClass.getName(), true, entityClass.getClassLoader());
                this.dtoSelectPaths = resolveDtoSelectPaths(originalEntity, entityClass.getSimpleName());
                System.out.println("this.dtoSelectPaths = " + this.dtoSelectPaths);

                // 提取字段路径前缀（t0、t1 等）
                this.dtoPrefixes = new LinkedHashSet<>();
                for (String path : this.dtoSelectPaths) {
                    int dotIdx = path.indexOf('.');
                    if (dotIdx > 0) {
                        String prefix = path.substring(0, dotIdx);
                        dtoPrefixes.add(prefix);
                    }
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("加载类失败: " + e.getMessage(), e);
            }
        } else {
            this.originalEntity = entityClass;
            this.dtoSelectPaths = null;
            this.dtoPrefixes = Collections.emptySet();
        }

        this.conditionPrefixes = (conditionBuilder != null)
                ? conditionBuilder.extractJoinTablePrefixes()
                : Collections.emptySet();
    }

    private List<String> resolveDtoSelectPaths(Class<?> origin, String dtoId) {
        String decap = Introspector.decapitalize(dtoId);
        List<String> paths = DTOSelectFieldsListFactory.resolveSelectList(origin, decap);
        if (paths.isEmpty()) {
            paths = DTOSelectFieldsListFactory.resolveSelectList(origin, dtoId);
            if (!paths.isEmpty()) {
                System.err.println("⚠️ 仅找到大写首字母形式的注册列: " + origin.getName() + "#" + dtoId);
            }
        }
        if (paths.isEmpty()) {
            throw new RuntimeException("未找到 DTO 查询列: " + origin.getName() + "#" + decap + " 或 " + dtoId);
        }
        return paths;
    }

    private String generate() {
        if (dtoSelectPaths != null) {
            selectList.addAll(dtoSelectPaths);
        }

        parseEntity(originalEntity, MAIN_ALIAS, new HashSet<>());

        return "select " + String.join(", ", selectList) + "\nfrom " +
                getTableName(originalEntity) + " " + MAIN_ALIAS + "\n" +
                String.join("\n", joinClauses);
    }

    private void parseEntity(Class<?> entity, String alias, Set<Field> joinFields) {
        if (joinFields.size() > MAX_JOIN_LENGTH) {
            throw new RuntimeException("关联层级过深: " + entity.getSimpleName());
        }
        if (processedEntities.contains(entity)) {
            boolean canBreak = joinFields.stream().anyMatch(f -> f.isAnnotationPresent(OneToOne.class));
            if (!canBreak) {
                throw new RuntimeException("检测到未标注 @OneToOne 的循环引用: " + entity.getSimpleName());
            }
            return;
        }

        processedEntities.add(entity);
        for (Field field : entity.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Join.class)) {
                String nextAlias = "t" + joinLevel;
                if (shouldGenerateJoin(nextAlias)) {
                    joinLevel++;
                    Set<Field> newJoinFields = new HashSet<>(joinFields);
                    newJoinFields.add(field);
                    handleJoin(field, alias, nextAlias, newJoinFields);
                }
            } else if (dtoSelectPaths == null) {
                handleField(field, alias);
            }
        }
        processedEntities.remove(entity);
    }

    private boolean shouldGenerateJoin(String alias) {
        if (dtoSelectPaths == null) {
            return true;
        }
        return dtoPrefixes.contains(alias) || conditionPrefixes.contains(alias);
    }

    private void handleJoin(Field field, String parentAlias, String thisAlias, Set<Field> newJoinFields) {
        Join join = field.getAnnotation(Join.class);
        Class<?> target = field.getType();
        Field pk = getPKField(target);

        joinClauses.add(String.format(
                "join %s %s ON %s.%s = %s.%s",
                getTableName(target),
                thisAlias,
                parentAlias,
                join.fk(),
                thisAlias,
                pk.getName()
        ));

        parseEntity(target, thisAlias, newJoinFields);
    }

    private void handleField(Field field, String alias) {
        if (!MAIN_ALIAS.equals(alias) && field.isAnnotationPresent(Id.class)) {
            return;
        }
        selectList.add(alias + "." + NamingConvertUtil.camel2SnakeCase(field.getName()));
    }

    private Field getPKField(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Id.class))
                .findFirst()
                .orElseThrow(() -> new NonPrimaryKeyException(clazz.getSimpleName() + " 缺少 @Id 注解"));
    }

    private String getTableName(Class<?> entity) {
        return AnnoNamingConvertUtil.camel2Snake(entity, entity.getSimpleName());
    }

    public static void main(String[] args) {
        System.out.println("generated = " + generated(BaseEmpInfo.class));
    }
}
