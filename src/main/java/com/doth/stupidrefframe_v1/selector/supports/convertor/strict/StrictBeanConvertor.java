package com.doth.stupidrefframe_v1.selector.supports.convertor.strict;


import com.doth.stupidrefframe_v1.selector.supports.sql.SqlGenerator;
import com.doth.stupidrefframe_v1.exception.NoColumnExistException;
import com.doth.stupidrefframe_v1.selector.supports.convertor.BeanConvertor;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 严格模式Bean转换器 - 实现 BeanConvertor 接口
 * 职责：将ResultSet中的行数据转换为指定 Bean 对象，并进行严格类型校验
 */
public class StrictBeanConvertor implements BeanConvertor {
    @Override
    public <T> T convert(ResultSet rs, Class<T> beanClass) throws Exception, NoColumnExistException {
        // 通过结果集元数据获取结果总列数量, 文本
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // 分支处理1：Map类型直接转换
        if (beanClass == Map.class) {
            return handleMapType(rs, metaData, columnCount);
        }

        // 分支处理2：Bean类型转换
        // 步骤1：构建列索引到字段的映射关系
        Map<Integer, Field> columnMap = columnMap2Field(beanClass, metaData, columnCount);
        // 步骤2：根据映射关系构建Bean实例
        return buildBean(beanClass, rs, columnMap);
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

            // 优先尝试原始列名匹配 (匹配策略)
            // 1. 直接匹配原始列名（如class_id → classId）
            Field field = fieldCache.get(columnName.toLowerCase());

            //
            // 2. 若未找到, 尝试蛇形转驼峰匹配（如class_id → classId）
            if (field == null) {
                String camelName = SqlGenerator.snake2CamelCase(columnName);
                field = fieldCache.get(camelName.toLowerCase());
            }
            // 验证字段是否存在
            validateFieldExistence(beanClass, columnName, field);
            columnMap.put(i, field);
        }
        return columnMap;
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
        // 创建示例
        T bean = beanClass.getDeclaredConstructor().newInstance();
        // 遍历字段填充值
        for (Map.Entry<Integer, Field> entry : columnMap.entrySet()) {
            int index = entry.getKey();
            Field field = entry.getValue();
            Object value = rs.getObject(index);

            // 类型兼容性校验（非空时检查）
            validateTypeCompatibility(beanClass, field, value);
            // 填充
            field.set(bean, value);
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