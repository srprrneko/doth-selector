package com.doth.selector.convertor.join;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.doth.selector.anno.Join;
import com.doth.selector.convertor.BeanConvertor;

public class JoinBeanConvertor implements BeanConvertor {

    // 弱引用缓存 + 同步包装，避免内存泄漏并保证线程安全
    private static final Map<Class<?>, MetaMap> JOIN_CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Field, MethodHandle> SETTER_CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    // 反射元数据缓存，提高字段列表和按名查找效率
    private static final Map<Class<?>, Field[]> CLASS_FIELDS_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, Field>> FIELD_NAME_CACHE = new ConcurrentHashMap<>();

    @Override
    public <T> T convert(ResultSet rs, Class<T> beanClass) throws Throwable {
        Class<?> actualClass = beanClass;
        // DTO 模式处理
        if (beanClass.isAnnotationPresent(com.doth.selector.anno.DependOn.class)) {
            com.doth.selector.anno.DependOn dependOn = beanClass.getAnnotation(com.doth.selector.anno.DependOn.class);
            String classPath = dependOn.clzPath();
            try {
                actualClass = Class.forName(classPath);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("无法加载 @DependOn 指定的类: " + classPath, e);
            }
        }

        ResultSetMetaData meta = rs.getMetaData();
        // 提取并缓存所有列名，避免多次遍历
        Set<String> columnSet = extractColumnLabels(meta);

        // 构建或复用 MetaMap
        MetaMap metaMap = JOIN_CACHE.computeIfAbsent(actualClass, clz -> {
            try {
                return analyzeClzStruct(clz, columnSet, "");
            } catch (Exception e) {
                throw new RuntimeException("解析联表结构失败: " + e.getMessage(), e);
            }
        });

        Object entity;
        try {
            entity = buildJoinBean(rs, actualClass, metaMap);
        } catch (Throwable e) {
            throw new RuntimeException("构造实体对象失败: " + e.getMessage(), e);
        }

        // DTO 构造
        if (!actualClass.equals(beanClass)) {
            try {
                Constructor<T> constructor = beanClass.getConstructor(entity.getClass());
                return constructor.newInstance(entity);
            } catch (Exception e) {
                throw new RuntimeException("DTO 构造失败: " + beanClass.getName(), e);
            }
        }

        return (T) entity;
    }

    private Set<String> extractColumnLabels(ResultSetMetaData meta) throws SQLException {
        int count = meta.getColumnCount();
        Set<String> labels = new HashSet<>(count);
        for (int i = 1; i <= count; i++) {
            labels.add(meta.getColumnLabel(i).toLowerCase());
        }
        return labels;
    }

    private MetaMap analyzeClzStruct(Class<?> clz, Set<String> columnSet, String prefix) throws Exception {
        MetaMap metaMap = new MetaMap();
        // 缓存并复用字段列表
        Field[] fields = CLASS_FIELDS_CACHE.computeIfAbsent(clz, c -> {
            Field[] fs = c.getDeclaredFields();
            for (Field f : fs) {
                f.setAccessible(true);
            }
            return fs;
        });
        // 构建按名查找缓存
        FIELD_NAME_CACHE.computeIfAbsent(clz, c -> {
            Map<String, Field> map = new ConcurrentHashMap<>();
            for (Field f : fields) {
                map.put(f.getName(), f);
            }
            return map;
        });

        for (Field field : fields) {
            if (field.isAnnotationPresent(Join.class)) {
                Join join = field.getAnnotation(Join.class);
                String fkColumn = join.fk();
                String refColumn = join.refFK();

                Field[] subFields = CLASS_FIELDS_CACHE.computeIfAbsent(field.getType(), c -> {
                    Field[] fs = c.getDeclaredFields();
                    for (Field f : fs) f.setAccessible(true);
                    return fs;
                });
                String nestedPrefix = field.getName() + "_";
                boolean hasAnyNestedField = false;
                for (Field subField : subFields) {
                    if (columnSet.contains((nestedPrefix + subField.getName()).toLowerCase())) {
                        hasAnyNestedField = true;
                        break;
                    }
                }
                if (hasAnyNestedField) {
                    MetaMap refMapping = analyzeClzStruct(field.getType(), columnSet, nestedPrefix);
                    metaMap.addNestedMeta(field, refMapping, fkColumn, refColumn);
                }
            } else {
                String colName = (prefix + field.getName()).toLowerCase();
                if (columnSet.contains(colName)) {
                    metaMap.addFieldMeta(field, prefix + field.getName());
                }
            }
        }
        return metaMap;
    }

    private <T> T buildJoinBean(ResultSet rs, Class<T> beanClass, MetaMap metaMap) throws Throwable {
        T bean = beanClass.getDeclaredConstructor().newInstance();
        // 普通字段赋值
        for (Map.Entry<Field, String> entry : metaMap.getFieldMeta().entrySet()) {
            setFieldValue(bean, entry.getKey(), rs.getObject(entry.getValue()));
        }
        // 嵌套对象赋值
        for (Map.Entry<Field, MetaMap> entry : metaMap.getNestedMeta().entrySet()) {
            Field field = entry.getKey();
            MetaMap nestedMeta = entry.getValue();
            String fkCol = metaMap.getFkColumn(field);
            String refCol = metaMap.getRefColumn(field);

            Object fkValue = null;
            try { fkValue = rs.getObject(fkCol); } catch (SQLException ignored) {}

            Object refBean = buildJoinBean(rs, field.getType(), nestedMeta);
            if (isAllFieldsNull(refBean, nestedMeta)) continue;

            Field refField = getField(field.getType(), refCol);
            Object refValueFromRs = null;
            try {
                refValueFromRs = rs.getObject(field.getName() + "_" + refField.getName());
            } catch (SQLException ignored) {}
            Object finalValue = fkValue != null && refValueFromRs != null
                    ? refValueFromRs : (fkValue != null ? fkValue : refValueFromRs);
            if (finalValue != null) {
                setFieldValue(refBean, refField, finalValue);
            }
            setFieldValue(bean, field, refBean);
        }
        return bean;
    }

    private boolean isAllFieldsNull(Object bean, MetaMap metaMap) {
        try {
            for (Field f : metaMap.getFieldMeta().keySet()) {
                if (f.get(bean) != null) return false;
            }
        } catch (IllegalAccessException ignored) { }
        return true;
    }

    private void setFieldValue(Object target, Field field, Object value) throws Throwable {
        MethodHandle setter = SETTER_CACHE.computeIfAbsent(field, f -> {
            try {
                return MethodHandles.lookup().unreflectSetter(f);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("赋值失败: " + e.getMessage(), e);
            }
        });
        setter.invoke(target, value);
    }

    private Field getField(Class<?> clz, String name) {
        Map<String, Field> map = FIELD_NAME_CACHE.get(clz);
        return map == null ? null : map.get(name);
    }
}