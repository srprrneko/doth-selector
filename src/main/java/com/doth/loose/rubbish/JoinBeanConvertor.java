package com.doth.loose.rubbish;

import com.doth.stupidrefframe.anno.Join;
import com.doth.stupidrefframe.selector.v1.coordinator.supports.convertor.BeanConvertor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.doth.stupidrefframe.selector.v1.util.CamelSnakeConvertUtil.snake2CamelCase;

@Deprecated
public class JoinBeanConvertor implements BeanConvertor {
    private static final Map<Class<?>, JoinMapping> JOIN_CACHE = new ConcurrentHashMap<>();
    private static final Map<Field, MethodHandle> SETTER_CACHE = new ConcurrentHashMap<>();

    @Override
    public <T> T convert(ResultSet rs, Class<T> beanClass) throws Throwable {
        JoinMapping mapping = JOIN_CACHE.computeIfAbsent(beanClass, clz -> {
            try {
                return analyzeJoinStructure(clz, rs.getMetaData());
            } catch (Exception e) {
                throw new RuntimeException("解析联表结构失败: " + e.getMessage() , e);
            }
        });
        return buildJoinBean(rs, beanClass, mapping);
    }

    // 无使用
    // private Field findReferencedField(Class<?> targetClass, String referencedColumn) throws NoSuchFieldException {
    //     // 处理驼峰转换
    //     String camelName = SelectGenerateFacade.snake2CamelCase(referencedColumn);
    //
    //     // 递归查找字段（支持父类）
    //     Class<?> clazz = targetClass;
    //     while (clazz != null) {
    //         try {
    //             Field field = clazz.getDeclaredField(camelName);
    //             field.setAccessible(true);
    //             return field;
    //         } catch (NoSuchFieldException e) {
    //             clazz = clazz.getSuperclass();
    //         }
    //     }
    //     throw new NoSuchFieldException(referencedColumn + "在" + targetClass + "中不存在");
    // }
    private JoinMapping analyzeJoinStructure(Class<?> clz, ResultSetMetaData meta) throws Exception {
        JoinMapping mapping = new JoinMapping();

        // 主表字段缓存（驼峰命名）
        Map<String, Field> primaryFieldCache = new HashMap<>();
        for (Field field : clz.getDeclaredFields()) {
            field.setAccessible(true);
            primaryFieldCache.put(field.getName().toLowerCase(), field);
        }

        // 处理主表列映射
        mapping.primaryMapping = new HashMap<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String columnLabel = meta.getColumnLabel(i);
            String camelName = snake2CamelCase(columnLabel);
            Field field = primaryFieldCache.get(camelName.toLowerCase());
            if (field != null) {
                mapping.primaryMapping.put(columnLabel, field);
            }
        }

        // 处理关联字段
        for (Field field : clz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Join.class)) {
                Join join = field.getAnnotation(Join.class);
                processJoinField(mapping, field, join, meta);
            }
        }

        return mapping;
    }
    // 无使用
    // private boolean containsColumn(ResultSetMetaData meta, String columnLabel) throws SQLException {
    //     for (int i = 1; i <= meta.getColumnCount(); i++) {
    //         if (meta.getColumnLabel(i).equalsIgnoreCase(columnLabel)) {
    //             return true;
    //         }
    //     }
    //     return false;
    // }
    // private void processNestedFields(JoinMapping mapping,
    //                                  Class<?> targetClass,
    //                                  String parentPrefix,
    //                                  ResultSetMetaData meta) throws Exception {
    //
    //     for (Field field : targetClass.getDeclaredFields()) {
    //         field.setAccessible(true);
    //
    //         // 处理普通字段
    //         if (!field.isAnnotationPresent(Join.class)) {
    //             String fullColumn = parentPrefix + SelectGenerateFacade.camel2SnakeCase(field.getName());
    //             if (containsColumn(meta, fullColumn)) {
    //                 mapping.primaryMapping.put(fullColumn, field);
    //             }
    //             continue;
    //         }
    //
    //         // 处理关联字段
    //         Join join = field.getAnnotation(Join.class);
    //         String currentPrefix = parentPrefix + field.getName() + "_";
    //
    //         // 递归处理嵌套对象
    //         processNestedFields(mapping, field.getType(), currentPrefix, meta);
    //
    //         // 处理外键映射
    //         String fkColumn = parentPrefix + join.fk();
    //         if (containsColumn(meta, fkColumn)) {
    //             Field refField = findReferencedField(field.getType(), join.referencedColumn());
    //             mapping.primaryMapping.put(fkColumn, refField);
    //         }
    //     }
    // }
    private void processJoinField(JoinMapping mapping, Field joinField, Join join, ResultSetMetaData meta) throws Exception {
        Class<?> joinClass = joinField.getType();
        String prefix = joinField.getName() + "_"; // 如department_

        Map<String, Field> joinFieldCache = new HashMap<>();
        for (Field field : joinClass.getDeclaredFields()) {
            field.setAccessible(true);
            joinFieldCache.put(field.getName().toLowerCase(), field);
        }

        Map<String, Field> columnMapping = new HashMap<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String columnLabel = meta.getColumnLabel(i);
            if (columnLabel.startsWith(prefix)) {
                String joinColumnName = columnLabel.substring(prefix.length());
                String camelName = snake2CamelCase(joinColumnName);
                Field field = joinFieldCache.get(camelName.toLowerCase());
                if (field != null) {
                    columnMapping.put(columnLabel, field);
                }
            }
        }

        // 处理外键
        Field refField = joinFieldCache.get(join.referencedColumn().toLowerCase());
        if (refField != null) {
            columnMapping.put(join.fk(), refField);
        }

        mapping.joinMappings.put(joinField, columnMapping);
    }

    private <T> T buildJoinBean(ResultSet rs, Class<T> beanClass, JoinMapping mapping) throws Throwable {
        T instance = beanClass.getDeclaredConstructor().newInstance();

        // 填充主表字段
        for (Map.Entry<String, Field> entry : mapping.primaryMapping.entrySet()) {
            setFieldValue(instance, entry.getValue(), rs.getObject(entry.getKey()));
        }

        // 填充关联字段
        for (Map.Entry<Field, Map<String, Field>> entry : mapping.joinMappings.entrySet()) {
            Field joinField = entry.getKey();
            Object joinInstance = joinField.getType().getDeclaredConstructor().newInstance();

            for (Map.Entry<String, Field> joinEntry : entry.getValue().entrySet()) {
                setFieldValue(joinInstance, joinEntry.getValue(), rs.getObject(joinEntry.getKey()));
            }

            setFieldValue(instance, joinField, joinInstance);
        }

        return instance;
    }

    private void setFieldValue(Object target, Field field, Object value) throws Throwable {
        MethodHandle setter = SETTER_CACHE.computeIfAbsent(field, f -> {
            try {
                // 关键修改点：使用privateLookupIn突破模块访问限制
                MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup());
                return privateLookup.unreflectSetter(f);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("无法访问字段: " + field.getName(), e);
            }
        });
        setter.invoke(target, value);
    }

    private static class JoinMapping {
        Map<String, Field> primaryMapping = new HashMap<>();
        Map<Field, Map<String, Field>> joinMappings = new HashMap<>();
    }
}