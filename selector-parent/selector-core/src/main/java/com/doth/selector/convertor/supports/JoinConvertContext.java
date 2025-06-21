package com.doth.selector.convertor.supports;


import com.doth.selector.convertor.supports.fieldconvertor.FieldConvertor;
import com.doth.selector.convertor.supports.fieldconvertor.FieldConvertorFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 上下文类：统一管理缓存和共用工具方法
 */
public class JoinConvertContext {

    // 每个类的元结构, 对应的 查询的 信息
    public static final Map<Class<?>, Map<String, MetaMap>> JOIN_CACHE = new ConcurrentHashMap<>();



    // 缓存：字段 setter
    static final Map<Field, MethodHandle> SETTER_CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    // 缓存：Class -> Field[]
    public static final Map<Class<?>, Field[]> CLASS_FIELDS_CACHE = new ConcurrentHashMap<>();

    // 缓存：Class -> (fieldName -> Field)
    public static final Map<Class<?>, Map<String, Field>> FIELD_NAME_CACHE = new ConcurrentHashMap<>();

    /**
     * 判断一个 Bean 的所有简单字段是否全为 null
     */
    public static boolean isAllFieldsNull(Object bean, MetaMap metaMap) {
        try {
            for (Field f : metaMap.getFieldMeta().keySet()) {
                if (f.get(bean) != null) {
                    return false;
                }
            }
        } catch (IllegalAccessException ignored) { }
        return true;
    }

    /**
     * 通过 MethodHandle 给字段赋值
     */
    public static void setFieldValue(Object target, Field field, Object value) throws Throwable {
        MethodHandle setter = SETTER_CACHE.computeIfAbsent(field, f -> {
            try {
                return MethodHandles.lookup().unreflectSetter(f);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("赋值失败: " + e.getMessage(), e);
            }
        });
        setter.invoke(target, value);
    }
    public static void setFieldValue(Object target, Field field, ResultSet rs, String columnLabel) throws Throwable {
        FieldConvertor convertor = FieldConvertorFactory.getConvertor(field.getType(), field);
        Object value = convertor.convert(rs, columnLabel);

        setFieldValue(target, field, value);
    }


    /**
     * 根据类和字段名查找 Field 对象
     */
    public static Field getField(Class<?> clz, String name) {
        Map<String, Field> map = FIELD_NAME_CACHE.get(clz);
        return (map != null ? map.get(name) : null);
    }


    public static class MetaMap {

        private final Map<Field, String> fieldMeta = new ConcurrentHashMap<>();

        private final Map<Field, MetaMap> nestedMeta = new ConcurrentHashMap<>();

        private final Map<Field, String> fkColumns = new ConcurrentHashMap<>();

        private final Map<Field, String> refColumns = new ConcurrentHashMap<>();

        public void addFieldMeta(Field field, String column) {
            fieldMeta.put(field, column);
        }

        public void addNestedMeta(Field field, MetaMap metaMap, String fkColumn, String refColumn) {
            nestedMeta.put(field, metaMap);
            fkColumns.put(field, fkColumn);
            refColumns.put(field, refColumn);
        }

        public Map<Field, String> getFieldMeta() { return fieldMeta; }

        public Map<Field, MetaMap> getNestedMeta() { return nestedMeta; }
        public String getFkColumn(Field field) { return fkColumns.get(field); }
        public String getRefColumn(Field field) { return refColumns.get(field); }
    }

}
