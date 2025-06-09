package com.doth.selector.convertor.join;

import com.doth.selector.anno.Join;
import com.doth.selector.convertor.BeanConvertor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

/**
 * 联查结果转换器
 *
 * 支持嵌套对象注入、支持 MethodHandle 高性能字段赋值
 */
public class JoinBeanConvertor implements BeanConvertor {

    private static final Map<Class<?>, MetaMap> JOIN_CACHE = new HashMap<>();
    private static final Map<Field, MethodHandle> SETTER_CACHE = new HashMap<>();

    @Override
    public <T> T convert(ResultSet rs, Class<T> beanClass) throws Throwable {
        Class<?> actualClass = beanClass;

        // ====== [DTO模式处理] ======
        Object dtoInstance = null;
        if (beanClass.isAnnotationPresent(com.doth.selector.anno.DependOn.class)) {
            com.doth.selector.anno.DependOn dependOn = beanClass.getAnnotation(com.doth.selector.anno.DependOn.class);
            String classPath = dependOn.clzPath();
            try {
                actualClass = Class.forName(classPath); // 获取原始实体类
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("无法加载 @DependOn 指定的类: " + classPath, e);
            }
        }

        ResultSetMetaData meta = rs.getMetaData();

        MetaMap metaMap = JOIN_CACHE.computeIfAbsent(actualClass, clz -> {
            try {
                return analyzeClzStruct(clz, meta, "");
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

        // 如果是 DTO 模式，执行 DTO 构造逻辑
        if (!actualClass.equals(beanClass)) {
            try {
                Constructor<T> constructor = beanClass.getConstructor(entity.getClass());
                T t = constructor.newInstance(entity);
                System.out.println("t.getClass() = " + t.getClass());
                return t;
            } catch (Exception e) {
                throw new RuntimeException("DTO 构造失败: " + beanClass.getName(), e);
            }
        }

        return (T) entity;
    }

    private MetaMap analyzeClzStruct(Class<?> clz, ResultSetMetaData meta, String prefix) throws Exception {
        MetaMap metaMap = new MetaMap();

        for (Field field : clz.getDeclaredFields()) {
            field.setAccessible(true);

            if (field.isAnnotationPresent(Join.class)) {
                Join join = field.getAnnotation(Join.class);
                String fkColumn = join.fk();
                String refColumn = join.refFK();

                // 是否存在嵌套字段（只要查到了一个就构造）
                boolean hasAnyNestedField = false;
                for (Field subField : field.getType().getDeclaredFields()) {
                    subField.setAccessible(true);
                    String columnName = field.getName() + "_" + subField.getName(); // 合成: department_name
                    if (columnExists(meta, columnName)) {
                        hasAnyNestedField = true;
                        break;
                    }
                }

                if (hasAnyNestedField) {
                    Class<?> refClass = field.getType();
                    String nestedPrefix = field.getName() + "_";
                    MetaMap refMapping = analyzeClzStruct(refClass, meta, nestedPrefix);
                    metaMap.addNestedMeta(field, refMapping, fkColumn, refColumn);
                }

            } else {
                String columnName = prefix + field.getName();
                if (columnExists(meta, columnName)) {
                    metaMap.addFieldMeta(field, columnName);
                }
            }
        }

        return metaMap;
    }

    private boolean columnExists(ResultSetMetaData meta, String columnName) throws SQLException {
        int columnCount = meta.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String colName = meta.getColumnLabel(i);
            if (colName.equalsIgnoreCase(columnName)) {
                return true;
            }
        }
        return false;
    }

    private <T> T buildJoinBean(ResultSet rs, Class<T> beanClass, MetaMap metaMap) throws Throwable {
        T bean = beanClass.getDeclaredConstructor().newInstance();

        for (Map.Entry<Field, String> entry : metaMap.getFieldMeta().entrySet()) {
            Field k = entry.getKey();
            Object v = rs.getObject(entry.getValue());
            setFieldValue(bean, k, v);
        }

        for (Map.Entry<Field, MetaMap> entry : metaMap.getNestedMeta().entrySet()) {
            Field field = entry.getKey();
            MetaMap nestedMeta = entry.getValue();

            String fkColumn = metaMap.getFkColumn(field);
            String refColumn = metaMap.getRefColumn(field);

            Object fkValue = null;
            try {
                fkValue = rs.getObject(fkColumn);
            } catch (SQLException e) {
                // 忽略缺失的外键字段，继续处理嵌套对象
            }
            Object refBean = buildJoinBean(rs, field.getType(), nestedMeta);

            // 全字段为空，则不赋值（可避免“空壳对象”）
            if (isAllFieldsNull(refBean, nestedMeta)) continue;

            Field refField = getField(field.getType(), refColumn);
            if (refField != null) {
                Object refValueFromRs = null;
                try {
                    refValueFromRs = rs.getObject(field.getName() + "_" + refField.getName()); // e.g. department_id
                } catch (SQLException ignored) {
                }

                Object finalValue = null;
                if (fkValue != null && refValueFromRs != null) {
                    // 优先使用 refValue（即 t1.id），防止主键为 null
                    finalValue = refValueFromRs;
                } else if (fkValue != null || refValueFromRs != null) {
                    finalValue = (fkValue != null) ? fkValue : refValueFromRs;
                }

                if (finalValue != null) {
                    setFieldValue(refBean, refField, finalValue);
                }
            }


            setFieldValue(bean, field, refBean);
        }

        return bean;
    }

    private boolean isAllFieldsNull(Object bean, MetaMap metaMap) {
        try {
            for (Field f : metaMap.getFieldMeta().keySet()) {
                f.setAccessible(true);
                if (f.get(bean) != null) {
                    return false;
                }
            }
        } catch (IllegalAccessException ignored) {
        }
        return true;
    }

    private void setFieldValue(Object target, Field field, Object value) throws Throwable {
        MethodHandle setter = SETTER_CACHE.computeIfAbsent(field, f -> {
            try {
                return MethodHandles.lookup().unreflectSetter(f);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("赋值失败! " + e.getMessage());
            }
        });
        setter.invoke(target, value);
    }

    private Field getField(Class<?> clz, String name) {
        for (Field field : clz.getDeclaredFields()) {
            if (field.getName().equals(name)) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }
}
