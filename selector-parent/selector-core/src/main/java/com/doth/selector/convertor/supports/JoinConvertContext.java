package com.doth.selector.convertor.supports;

import com.doth.selector.convertor.supports.fieldconvertor.FieldConvertor;
import com.doth.selector.convertor.supports.fieldconvertor.FieldConvertorFactory;
import lombok.Getter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 优化后的上下文类，消除字段缓存冗余
 */
public class JoinConvertContext {

    public static final Map<Class<?>, Map<String, MetaMap>> JOIN_CACHE = new ConcurrentHashMap<>();

    // 缓存：字段 setter
    static final Map<Field, MethodHandle> SETTER_CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    // 仅保留字段名到字段的缓存
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
        } catch (IllegalAccessException ignored) {
        }
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
     * 根据class对象和字段名获取Field对象
     */
    public static Field getField(Class<?> clz, String name) {
        Map<String, Field> map = FIELD_NAME_CACHE.get(clz);
        return (map != null ? map.get(name) : null);
    }

    /**
     * 根据类对象获取Field数组（动态获取，避免额外缓存）
     */
    public static Field[] getFields(Class<?> clz) {
        return FIELD_NAME_CACHE.computeIfAbsent(clz, c -> {
            Map<String, Field> map = new ConcurrentHashMap<>();
            for (Field f : c.getDeclaredFields()) {
                f.setAccessible(true);
                map.put(f.getName(), f);
            }
            return map;
        }).values().toArray(new Field[0]);
    }

    /**
     * 实体类的信息元
     */
    public static class MetaMap {

        @Getter
        private final Map<Field, String> fieldMeta = new ConcurrentHashMap<>();

        @Getter
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

        public String getFkColumn(Field field) {
            return fkColumns.get(field);
        }

        public String getRefColumn(Field field) {
            return refColumns.get(field);
        }
    }
}
