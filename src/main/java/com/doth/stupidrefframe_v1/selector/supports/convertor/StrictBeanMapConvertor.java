package com.doth.stupidrefframe_v1.selector.supports.convertor;


import com.doth.stupidrefframe_v1.anno.JoinColumn;
import com.doth.stupidrefframe_v1.anno.Mapping;
import com.doth.stupidrefframe_v1.exception.NoColumnExistException;
import com.doth.stupidrefframe_v1.selector.supports.SqlGenerator;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * 严格模式Bean转换器 - 实现 BeanConvertor 接口
 * 职责：将ResultSet中的行数据转换为指定 Bean 对象，并进行严格类型校验
 */
public class StrictBeanMapConvertor implements BeanConvertor {
    // 增加：缓存注解元数据（类 -> 字段 -> 注解）
    private static final Map<Class<?>, Map<Field, JoinColumn>> joinColumnCache = new HashMap<>();


    /**
     * 反射设置字段值（处理基本类型兼容性、嵌套路径）
     * @param target    目标对象（可能是嵌套对象）
     * @param field     待设置字段
     * @param value     数据库值
     */
    private void setFieldValue(Object target, Field field, Object value)
            throws IllegalAccessException, NoSuchFieldException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        // 处理空值直接跳过
        if (value == null) return;

        // 类型兼容性检查与转换
        Object convertedValue = convertType(field.getType(), value);

        // 处理嵌套路径（如 department.name）
        if (field.getName().contains(".")) {
            setNestedFieldValue(target, field.getName(), convertedValue);
        } else {
            field.setAccessible(true);
            field.set(target, convertedValue);
        }
    }

    /**
     * 处理嵌套路径字段赋值（如 department.name）
     */
    private void setNestedFieldValue(Object root, String fieldPath, Object value)
            throws IllegalAccessException, NoSuchFieldException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        String[] parts = fieldPath.split("\\.");
        Object current = root;

        // 逐级创建嵌套对象实例
        for (int i = 0; i < parts.length - 1; i++) {
            Field parentField = current.getClass().getDeclaredField(parts[i]);
            parentField.setAccessible(true);
            Object child = parentField.get(current);
            if (child == null) {
                child = parentField.getType().getDeclaredConstructor().newInstance();
                parentField.set(current, child);
            }
            current = child;
        }

        // 设置最终字段值
        Field targetField = current.getClass().getDeclaredField(parts[parts.length - 1]);
        targetField.setAccessible(true);
        targetField.set(current, value);
    }
    @Override
    public <T> T convert(ResultSet rs, Class<T> beanClass) throws Exception, NoColumnExistException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        if (beanClass == Map.class) {
            return handleMapType(rs, metaData, columnCount);
        }

        // 修改：增加注解预解析
        preprocessJoinColumns(beanClass);
        Map<Integer, FieldInfo> columnMap = buildColumnMapping(beanClass, metaData, columnCount);
        return buildBeanWithNesting(beanClass, rs, columnMap);
    }

    // 新增：预处理@JoinColumn注解
    private <T> void preprocessJoinColumns(Class<T> clazz) {
        if (joinColumnCache.containsKey(clazz)) return;

        Map<Field, JoinColumn> fieldAnnotations = new HashMap<>();
        for (Field field : clazz.getDeclaredFields()) {
            JoinColumn jc = field.getAnnotation(JoinColumn.class);
            if (jc != null) {
                field.setAccessible(true);
                fieldAnnotations.put(field, jc);
                // 递归处理嵌套类的注解
                preprocessJoinColumns(field.getType());
            }
        }
        joinColumnCache.put(clazz, fieldAnnotations);
    }

    // 重构：字段映射信息包装类
    private static class FieldInfo {
        Field field;
        String fkColumn; // 外键列（如果有）
        boolean isNested;

        FieldInfo(Field field, String fkColumn, boolean isNested) {
            this.field = field;
            this.fkColumn = fkColumn;
            this.isNested = isNested;
        }
    }

    // 重构：带注解处理的列映射构建
    private <T> Map<Integer, FieldInfo> buildColumnMapping(Class<T> beanClass,
                                                           ResultSetMetaData metaData,
                                                           int columnCount) throws Exception, NoColumnExistException {
        Map<Integer, FieldInfo> columnMap = new HashMap<>();
        Map<String, Field> fieldCache = new HashMap<>();
        cacheFieldsRecursively(beanClass, "", fieldCache);

        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnLabel(i);
            Field mappedField = findMatchingField(columnName, beanClass, fieldCache);

            // 检查是否外键列需要特殊处理
            FieldInfo fieldInfo = resolveJoinColumn(beanClass, columnName, mappedField);
            columnMap.put(i, fieldInfo);
        }
        return columnMap;
    }

    // 新增：解析@JoinColumn关联
    private <T> FieldInfo resolveJoinColumn(Class<T> beanClass, String columnName, Field field) {
        Map<Field, JoinColumn> annotations = joinColumnCache.get(beanClass);
        if (annotations == null) return new FieldInfo(field, null, false);

        for (Map.Entry<Field, JoinColumn> entry : annotations.entrySet()) {
            JoinColumn jc = entry.getValue();
            // 匹配外键列（如d_id匹配@JoinColumn(fk="d_id")）
            if (jc.fk().equalsIgnoreCase(columnName)) {
                return new FieldInfo(entry.getKey(), columnName, true);
            }
        }
        return new FieldInfo(field, null, false);
    }

    // 修改后的字段缓存（支持嵌套路径）
    private void cacheFieldsRecursively(Class<?> clazz, String parentPath, Map<String, Field> cache) {
        for (Field field : clazz.getDeclaredFields()) {
            // 处理@JoinColumn标注的字段
            if (field.isAnnotationPresent(JoinColumn.class)) {
                JoinColumn jc = field.getAnnotation(JoinColumn.class);
                // 将外键列名（如d_id）映射到当前字段
                cache.put(jc.fk().toLowerCase(), field);
            }

            // 正常缓存字段名（带路径）
            String fieldPath = parentPath.isEmpty() ? field.getName() : parentPath + "." + field.getName();
            cache.put(fieldPath.toLowerCase(), field);

            // 递归处理嵌套对象字段
            if (!field.getType().getName().startsWith("java.")) {
                cacheFieldsRecursively(field.getType(), fieldPath, cache);
            }
        }
    }

    private Field findMatchingField(String columnName, Class<?> beanClass,
                                    Map<String, Field> fieldCache) throws NoColumnExistException {
        // 优先检查是否外键列（如d_id）
        Field field = fieldCache.get(columnName.toLowerCase());
        if (field != null) return field;

        // 原始逻辑处理带分隔符的列名（如department_name）
        String[] parts = columnName.split("[._]");
        String normalized = String.join(".", Arrays.stream(parts)
                .map(SqlGenerator::snake2CamelCase)
                .toArray(String[]::new)).toLowerCase();

        field = fieldCache.get(normalized);
        if (field == null) {
            throw new NoColumnExistException("未知列: " + columnName + " 出现在 " + beanClass.getSimpleName());
        }
        return field;
    }

    private <T> T buildBeanWithNesting(Class<T> beanClass, ResultSet rs,
                                       Map<Integer, FieldInfo> columnMap) throws Exception {
        T root = beanClass.getDeclaredConstructor().newInstance();
        Map<Object, Map<Field, Object>> nestedCache = new HashMap<>();

        // 第一遍：处理所有外键关联字段（如d_id）
        for (Map.Entry<Integer, FieldInfo> entry : columnMap.entrySet()) {
            FieldInfo info = entry.getValue();
            if (info.isNested) {
                Object value = rs.getObject(entry.getKey());
                handleJoinColumnField(root, info, value, nestedCache);
            }
        }

        // 第二遍：填充其他普通字段
        for (Map.Entry<Integer, FieldInfo> entry : columnMap.entrySet()) {
            FieldInfo info = entry.getValue();
            if (!info.isNested) {
                Object value = rs.getObject(entry.getKey());
                setFieldValue(root, info.field, value);
            }
        }
        return root;
    }

    // 新增：处理@JoinColumn字段的级联赋值
    private void handleJoinColumnField(Object parent, FieldInfo info,
                                       Object columnValue, Map<Object, Map<Field, Object>> cache) throws Exception {
        Field joinField = info.field;
        JoinColumn jc = joinField.getAnnotation(JoinColumn.class);

        // 获取或创建嵌套对象
        Object nestedObj = joinField.get(parent);
        if (nestedObj == null) {
            nestedObj = joinField.getType().newInstance();
            joinField.set(parent, nestedObj);
        }

        // 找到目标字段（通常是主键）
        Field targetField = findTargetField(nestedObj.getClass(), jc.referencedColumn());
        targetField.setAccessible(true);

        // 类型转换（例如String -> Integer）
        Object convertedValue = convertType(targetField.getType(), columnValue);
        targetField.set(nestedObj, convertedValue);

        // 缓存嵌套对象待填充的其他字段
        if (!cache.containsKey(nestedObj)) {
            cache.put(nestedObj, new HashMap<>());
        }
        cache.get(nestedObj).put(targetField, convertedValue);
    }

    // 新增：类型转换支持
    private Object convertType(Class<?> targetType, Object value) {
        if (value == null) return null;
        if (targetType.isAssignableFrom(value.getClass())) return value;

        // 简单类型转换示例
        if (targetType == Integer.class && value instanceof Number) {
            return ((Number) value).intValue();
        }
        // 可扩展其他类型转换
        return value;
    }

    /**
     * 根据referencedColumn名称查找目标类的字段（支持蛇形/驼峰转换）
     * @param targetClass 关联目标类（如Department）
     * @param referencedColumn @JoinColumn中指定的目标列名（如"id"）
     */
    private Field findTargetField(Class<?> targetClass, String referencedColumn)
            throws NoSuchFieldException {

        // 优先直接匹配字段名（考虑驼峰命名）
        try {
            return targetClass.getDeclaredField(referencedColumn);
        } catch (NoSuchFieldException e) {
            // 尝试蛇形转驼峰
            String camelName = SqlGenerator.snake2CamelCase(referencedColumn);
            try {
                return targetClass.getDeclaredField(camelName);
            } catch (NoSuchFieldException ex) {
                throw new NoSuchFieldException(String.format(
                        "目标类 %s 中找不到字段 '%s' 或 '%s'",
                        targetClass.getSimpleName(),
                        referencedColumn,
                        camelName
                ));
            }
        }
    }








    /**
     * 构建列到字段的映射关系
     * 步骤：
     * 1. 缓存Bean字段（原始名和蛇形名）
     * 2. 遍历结果集列，匹配字段
     */
    private <T> Map<Integer, Field> columnMap2Field (Class<T> beanClass,
                                                    ResultSetMetaData metaData,
                                                    int columnCount) throws Exception, NoColumnExistException {

        // 字段缓存：Key为小写字段名，Value为Field对象
        Map<String, Field> fieldCache = new HashMap<>();

        // 递归缓存所有字段（包括嵌套对象）
        cacheFieldsRecursively(beanClass, fieldCache);
        // 遍历Bean的所有字段
        for (Field field : beanClass.getDeclaredFields()) {
            field.setAccessible(true);
            // 同时缓存原始字段名和蛇形转驼峰名
            fieldCache.put(field.getName().toLowerCase(), field);

            // 缓存原始字段名（小写）
            String snakeName = SqlGenerator.camel2SnakeCase(field.getName());
            fieldCache.put(snakeName.toLowerCase(), field);
        }

        // 建立列索引到字段的映射
        Map<Integer, Field> columnMap = new HashMap<>();
        for (int i = 1; i <= columnCount; i++) {
            // 获取列名（使用getColumnLabel支持别名）
            String columnName = metaData.getColumnLabel(i);
            List<String> parts = Arrays.asList(columnName.split("[._]")); // 支持 _ 和 . 分隔符

            // 优先尝试原始列名匹配 (匹配策略)
            // 1. 直接匹配原始列名（如class_id → classId）
            // Field field = fieldCache.get(columnName.toLowerCase());
            // 尝试构建字段路径（如 "department.name"）
            String fieldPath = String.join(".", parts);
            Field field = fieldCache.get(fieldPath.toLowerCase());

            //
            // 2. 若未找到, 尝试蛇形转驼峰匹配（如class_id → classId）
            // 若未找到，回退到驼峰/蛇形转换匹配
            if (field == null) {
                String camelName = SqlGenerator.snake2CamelCase(columnName);
                parts = Arrays.asList(camelName.split("\\."));
                fieldPath = String.join(".", parts);
                field = fieldCache.get(fieldPath.toLowerCase());
            }
            // 验证字段是否存在
            validateFieldExistence(beanClass, columnName, field);
            columnMap.put(i, field);
        }
        return columnMap;
    }
    private void cacheFieldsRecursively(Class<?> clazz, Map<String, Field> fieldCache) {
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            // 检查是否标记为嵌套映射
            if (field.isAnnotationPresent(Mapping.class)) {
                // 递归处理嵌套类字段
                cacheFieldsRecursively(field.getType(), fieldCache);
            } else {
                // 缓存当前字段的原始名和蛇形名
                fieldCache.put(field.getName().toLowerCase(), field);
                String snakeName = SqlGenerator.camel2SnakeCase(field.getName());
                fieldCache.put(snakeName.toLowerCase(), field);
            }
        }
    }



    /**
     * 验证字段是否存在（不存在则抛出异常）
     */
    private <T> void validateFieldExistence(Class<T> beanClass, String columnName, Field field)
            throws NoColumnExistException {
        if (field == null) {
            // 添加更清晰的错误提示，包含原列名和类信息
            throw new NoColumnExistException(String.format(
                    "查询结果包含无法映射的列'%s'（类: %s）",
                    columnName,
                    beanClass.getSimpleName()
            ));
        }
    }

    /**
     * 构建Bean实例并填充字段值
     */
    private <T> T buildBean(Class<T> beanClass, ResultSet rs, Map<Integer, Field> columnMap) throws Exception {
        T bean = beanClass.getDeclaredConstructor().newInstance();
        for (Map.Entry<Integer, Field> entry : columnMap.entrySet()) {
            int index = entry.getKey();
            Field field = entry.getValue();
            Object value = rs.getObject(index);

            // 处理嵌套字段（如 department.name）
            String[] fieldPath = field.getName().split("\\.");
            Object currentObj = bean;
            for (int i = 0; i < fieldPath.length - 1; i++) {
                Field parentField = currentObj.getClass().getDeclaredField(fieldPath[i]);
                parentField.setAccessible(true);
                Object childObj = parentField.get(currentObj);
                if (childObj == null) {
                    childObj = parentField.getType().getDeclaredConstructor().newInstance();
                    parentField.set(currentObj, childObj);
                }
                currentObj = childObj;
            }

            // 最终字段赋值
            Field targetField = currentObj.getClass().getDeclaredField(fieldPath[fieldPath.length - 1]);
            targetField.setAccessible(true);
            validateTypeCompatibility(targetField.getDeclaringClass(), targetField, value);
            targetField.set(currentObj, value);
        }
        return bean;
    }

    /**
     * 严格类型校验
     */
    private void validateTypeCompatibility(Class<?> beanClass, Field field, Object value) {
        if (value == null) return; // 允许NULL值

        Class<?> fieldType = field.getType();
        Class<?> valueType = value.getClass();

        // 检查字段类型是否兼容（包括继承关系）
        if (!fieldType.isAssignableFrom(valueType)) {
            String errorMsg = String.format(
                    "类型不匹配! 类: %s 字段: %s 需要类型: %s 实际类型: %s",
                    beanClass.getSimpleName(),
                    field.getName(),
                    fieldType.getSimpleName(),
                    valueType.getSimpleName()
            );
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * 处理Map类型转换（特殊分支）
     */
    @SuppressWarnings("unchecked")
    private <T> T handleMapType(ResultSet rs, ResultSetMetaData metaData, int columnCount) 
        throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 1; i <= columnCount; i++) {
            // 使用列标签作为键，保留原始列名
            map.put(metaData.getColumnLabel(i), rs.getObject(i));
        }
        return (T) map;
    }
}