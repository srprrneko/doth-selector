package com.doth.selector.coordinator.supports.sqlgenerator.tool;

import com.doth.selector.anno.Id;
import com.doth.selector.anno.Join;
import com.doth.selector.anno.OneToOne;
import com.doth.selector.anno.DTOConstructor;
import com.doth.selector.common.exception.NonPrimaryKeyException;
import com.doth.selector.common.util.AnnoNamingConvertUtil;
import com.doth.selector.common.util.CamelSnakeConvertUtil;
import com.doth.selector.dto.DtoStackResolver;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

public class AutoQueryGenerator {
    private static final String MAIN_ALIAS = "t0"; // 默认主表别名
    private static int joinLevel = 1;  // join 层数, 主表固定为0, 从表从1开始

    private static Set<String> dtoFieldSet = null; // 当前使用的DTO字段集合

    public static String generated(Class<?> mainEntity) {
        // 解析当前是否处于 DTO 模式
        String dtoId = DtoStackResolver.resolveDTOIdFromStack();
        if (dtoId != null) {
            dtoFieldSet = extractDtoFieldNames(mainEntity, dtoId);
        } else {
            dtoFieldSet = null; // 表示不使用 DTO 筛选字段
        }

        List<String> selectList = new ArrayList<>();
        List<String> joinClauses = new ArrayList<>();
        joinLevel = 1;

        parseEntity(mainEntity, MAIN_ALIAS, selectList, joinClauses, 0, mainEntity, null, null);

        return new StringBuilder()
                .append("select ").append(String.join(", ", selectList)).append("\n")
                .append("from ").append(getTableName(mainEntity)).append(" ").append(MAIN_ALIAS).append("\n")
                .append(String.join("\n", joinClauses))
                .toString();
    }

    private static void parseEntity(Class<?> entity, String currentAlias,
                                    List<String> selectList, List<String> joinClauses,
                                    int depth, Class<?> mainEntity,
                                    Set<Field> joinField, Set<Class<?>> processedEntities) {
        if (processedEntities == null) {
            processedEntities = new HashSet<>();
        }
        if (processedEntities.contains(entity)) {
            if (entity.equals(mainEntity) && depth > 0) {
                boolean hasOneToOne = false;
                for (Field f : entity.getDeclaredFields()) {
                    if (f.getType() == mainEntity && f.isAnnotationPresent(OneToOne.class)) {
                        hasOneToOne = true;
                        break;
                    }
                }
                if (!hasOneToOne) {
                    throw new RuntimeException("检测到未标注 @OneToOne 的循环引用: " +
                            entity.getSimpleName() + " ←→ " + mainEntity.getSimpleName());
                }
                return;
            }
            throw new RuntimeException("检测到循环引用: " + entity.getSimpleName());
        }

        processedEntities.add(entity);

        for (Field field : entity.getDeclaredFields()) {
            field.setAccessible(true);
            String fieldName = field.getName();

            // 若使用DTO结构过滤字段，且该字段不在其中，跳过
            if (dtoFieldSet != null && !dtoFieldSet.contains(fieldName) && depth == 0) {
                continue;
            }

            if (field.isAnnotationPresent(Join.class)) {
                if (field.getType() == mainEntity && depth > 0) {
                    if (!field.isAnnotationPresent(OneToOne.class)) {
                        throw new RuntimeException("嵌套字段存在未声明@OneToOne关系: " + field.getName());
                    }
                    continue;
                }

                Set<Field> newJoinField = joinField == null ? new HashSet<>() : new HashSet<>(joinField);
                if (newJoinField.add(field)) {
                    handleJoinColumn(field, currentAlias, selectList, joinClauses,
                            depth, mainEntity, newJoinField, processedEntities);
                }
            } else {
                handleNormalField(field, currentAlias, selectList, depth);
            }
        }

        processedEntities.remove(entity);
    }

    private static void handleJoinColumn(Field field, String currentAlias,
                                         List<String> selectList, List<String> joinClauses,
                                         int joinFloors, Class<?> mainEntity,
                                         Set<Field> joinField, Set<Class<?>> processedEntities) {
        Join jc = field.getAnnotation(Join.class);

        // 如果当前是主表，且使用了 DTO 过滤字段，但 DTO 中不包含该关联字段，则跳过 JOIN
        if (joinFloors == 0 && dtoFieldSet != null && !dtoFieldSet.contains(field.getName())) {
            return;
        }

        String fkColumn = currentAlias + "." + jc.fk();
        selectList.add(fkColumn);

        Class<?> refEntity = field.getType();
        String nextAlias = "t" + joinLevel++;
        String refTable = getTableName(refEntity);
        String refColumn = getPKField(refEntity).getName();

        joinClauses.add(
                String.format("join %s %s ON %s.%s = %s.%s",
                        refTable, nextAlias,
                        currentAlias, jc.fk(),
                        nextAlias, refColumn)
        );

        parseEntity(refEntity, nextAlias, selectList, joinClauses,
                joinFloors + 1, mainEntity, joinField, processedEntities);
    }

    private static Field getPKField(Class<?> entityClass) {
        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        throw new NonPrimaryKeyException(entityClass.getSimpleName() + " 未找到主键字段, 请使用@Id注解标记主键");
    }

    private static void handleNormalField(Field field, String alias,
                                          List<String> selectList, int joinFloors) {
        boolean isPrimaryKey = field.isAnnotationPresent(Id.class);
        if (joinFloors > 0 && isPrimaryKey) return;

        String columnName = CamelSnakeConvertUtil.camel2SnakeCase(field.getName());
        selectList.add(alias + "." + columnName);
    }

    private static String getTableName(Class<?> entity) {
        return AnnoNamingConvertUtil.camel2Snake(entity, entity.getSimpleName());
    }

    private static Set<String> extractDtoFieldNames(Class<?> entityClass, String dtoId) {
        Set<String> result = new HashSet<>();
        Constructor<?>[] ctors = entityClass.getDeclaredConstructors();
        for (Constructor<?> ctor : ctors) {
            if (ctor.isAnnotationPresent(DTOConstructor.class)) {
                DTOConstructor anno = ctor.getAnnotation(DTOConstructor.class);
                if (anno.id().equals(dtoId)) {
                    for (var param : ctor.getParameters()) {
                        result.add(param.getName()); // ✅ 构造字段名作为字段名使用
                    }
                    break;
                }
            }
        }
        System.out.println("result = " + result);
        return result;
    }
}
