package com.doth.stupidrefframe.selector.v1.coordinator.supports.convertor.strict;


import com.doth.stupidrefframe.selector.v1.exception.NoColumnExistException;
import com.doth.stupidrefframe.selector.v1.coordinator.supports.convertor.BeanConvertor;
import lombok.Setter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.doth.stupidrefframe.selector.v1.util.CamelSnakeConvertUtil.snake2CamelCase;

/**
 * 职责：将ResultSet中的行数据转换为指定 Bean 对象，轻量版
 */
@Setter
public class StrictBeanConvertorLv implements BeanConvertor {
    // Class级别缓存字段映射
    private static final Map<Class<?>, Map<Integer, FieldMapping>> CLASS_MAPPING_CACHE = new ConcurrentHashMap<>();

    // 缓存MethodHandle提升反射性能
    protected static final Map<Field, MethodHandle> SETTER_CACHE = new ConcurrentHashMap<>();

    // 优化点3：配置方法
    // 可配置的类型校验策略
    private boolean strictTypeCheck = true;

    @Override
    public <T> T convert(ResultSet rs, Class<T> beanClass) throws Throwable {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        if (beanClass == Map.class) {
            return handleMapType(rs, metaData, columnCount);
        }

        // 从缓存获取字段映射
        Map<Integer, FieldMapping> columnMap = CLASS_MAPPING_CACHE.get(beanClass);
        if (columnMap == null) {
            columnMap = createFieldMapping(beanClass, metaData, columnCount);
            CLASS_MAPPING_CACHE.put(beanClass, columnMap);
        }
        return buildBean(beanClass, rs, columnMap);
    }



    private <T> Map<Integer, FieldMapping> createFieldMapping(Class<T> beanClass,
                                                              ResultSetMetaData metaData, int columnCount) throws Exception, NoColumnExistException {
        Map<String, Field> fieldCache = new HashMap<>();
        for (Field field : beanClass.getDeclaredFields()) {
            field.setAccessible(true);
            fieldCache.put(field.getName().toLowerCase(), field);
        }

        Map<Integer, FieldMapping> columnMap = new HashMap<>();
        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnLabel(i);
            String camelName = snake2CamelCase(columnName);
            Field field = fieldCache.get(camelName.toLowerCase());

            if (field == null) {
                throw new NoColumnExistException(String.format(
                        "列[%s]无法映射到%s (候选字段：%s)",
                        columnName, beanClass.getSimpleName(), fieldCache.keySet()
                ));  // 优化点6：增强异常信息
            }

            // 预加载MethodHandle
            FieldMapping mapping = new FieldMapping(field);
            columnMap.put(i, mapping);
            SETTER_CACHE.putIfAbsent(field, mapping.setter);
        }
        return columnMap;
    }

    private <T> T buildBean(Class<T> beanClass, ResultSet rs,
                            Map<Integer, FieldMapping> columnMap) throws Throwable {
        T bean = beanClass.getDeclaredConstructor().newInstance();
        for (Map.Entry<Integer, FieldMapping> entry : columnMap.entrySet()) {
            int index = entry.getKey();
            FieldMapping mapping = entry.getValue();
            Object value = rs.getObject(index);

            if (strictTypeCheck) {
                validateTypeCompatibility(beanClass, mapping.field, value);
            }

            // 使用MethodHandle赋值
            MethodHandle setter = SETTER_CACHE.get(mapping.field);
            if (setter != null) {
                setter.invoke(bean, value);
            } else {
                mapping.field.set(bean, value);  // Fallback
            }
        }
        return bean;
    }

    // 优化点3：可关闭的类型校验
    private void validateTypeCompatibility(Class<?> beanClass, Field field, Object value) {
        if (value == null) return;

        Class<?> fieldType = field.getType();
        if (!fieldType.isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException(String.format(
                    "类型不匹配! %s.%s 需要: %s 实际: %s",
                    beanClass.getSimpleName(), field.getName(),
                    fieldType.getSimpleName(), value.getClass().getSimpleName()
            ));
        }
    }

    @SuppressWarnings("unchecked")
    // 特殊处理返回值为map 的时候
    protected <T> T handleMapType(ResultSet rs, ResultSetMetaData metaData, int columnCount)
            throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 1; i <= columnCount; i++) {
            map.put(metaData.getColumnLabel(i), rs.getObject(i));
        }
        return (T) map;
    }



    private class FieldMapping {
        public final Field field;
        public final MethodHandle setter;

        public FieldMapping(Field field) throws IllegalAccessException {
            this.field = field;
            this.setter = MethodHandles.lookup().unreflectSetter(field);
        }

        public void setValue(Object target, Object value) throws Throwable {
            setter.invoke(target, value);
        }
    }
}