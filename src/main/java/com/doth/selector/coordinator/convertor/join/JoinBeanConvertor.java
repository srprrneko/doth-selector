package com.doth.selector.coordinator.convertor.join;

import com.doth.selector.anno.Join;
import com.doth.selector.anno.UseDTO;
import com.doth.selector.common.convertor.ValueConverterFactory;
import com.doth.selector.coordinator.convertor.BeanConvertor;
import com.doth.selector.common.util.TypeResolver;
import com.doth.selector.dto.DTOFactory;
import com.doth.selector.dto.DtoStackResolver;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 联查结果转换器：将 ResultSet 映射为嵌套结构 JavaBean
 * 支持 @Join 注解、一对一嵌套、多级递归、缓存优化
 */
public class JoinBeanConvertor implements BeanConvertor {

    /**
     * 类结构元缓存：避免重复解析
     */
    private static final Map<Class<?>, MetaMap> JOIN_CACHE = new HashMap<>();

    /**
     * 字段Setter缓存，避免重复生成
     */
    private static final Map<Field, MethodHandle> SETTER_CACHE = new HashMap<>();

    /**
     * 核心入口：将 ResultSet 映射为嵌套JavaBean
     */
    @Override
    public <T> T convert(ResultSet rs, Class<T> beanClass) throws Throwable {
        ResultSetMetaData meta = rs.getMetaData();

        MetaMap metaMap;
        try {
            metaMap = JOIN_CACHE.computeIfAbsent(beanClass, clz -> {
                try {
                    return analyzeClzStruct(clz, meta, "");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return buildJoinBean(rs, beanClass, metaMap);
        } catch (RuntimeException e) {
            throw new SQLException("联表结构解析失败", e);
        } finally {
            DtoStackResolver.clear();
        }
    }

    /**
     * 解析类字段结构，支持嵌套结构
     */
    private MetaMap analyzeClzStruct(Class<?> clz, ResultSetMetaData meta, String prefix) throws Exception {
        MetaMap metaMap = new MetaMap();

        for (Field field : clz.getDeclaredFields()) {
            field.setAccessible(true);

            Join join = field.getAnnotation(Join.class);
            if (join != null) {
                String fkColumn = join.fk();
                String refColumn = join.refFK();

                if (columnExists(meta, fkColumn)) {
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

    /**
     * 字段是否存在于结果集
     */
    private boolean columnExists(ResultSetMetaData meta, String columnName) throws SQLException {
        int columnCount = meta.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            if (meta.getColumnLabel(i).equalsIgnoreCase(columnName)) {
                return true;
            }
        }
        return false;
    }


    /**
     * 构建嵌套对象（递归）
     */
    private <T> T buildJoinBean(ResultSet rs, Class<T> beanClass, MetaMap metaMap) throws Throwable {
        // 1. 通过调用栈查找是否启用了 UseDTO 注解
        String dtoId = DtoStackResolver.resolveDTOIdFromStack();

        // 2. 尝试使用 DTOFactory 获取子类，如果没有，则退回使用原类型
        Class<?> actualClass = DTOFactory.resolve(beanClass, dtoId);
        T bean = (T) actualClass.getDeclaredConstructor().newInstance();
        // System.out.println("actualClass = " + actualClass);


        for (Map.Entry<Field, String> entry : metaMap.getFieldMeta().entrySet()) {
            Object val = rs.getObject(entry.getValue());
            setFieldValue(bean, entry.getKey(), val);
        }

        for (Map.Entry<Field, MetaMap> entry : metaMap.getNestedMeta().entrySet()) {
            Field field = entry.getKey();
            MetaMap nestedMeta = entry.getValue();

            String fkColumn = metaMap.getFkColumn(field);
            String refColumn = metaMap.getRefColumn(field);
            Object fkValue = rs.getObject(fkColumn);

            Object refBean = buildJoinBean(rs, field.getType(), nestedMeta);
            Field refField = getField(field.getType(), refColumn);
            if (refField != null) {
                setFieldValue(refBean, refField, fkValue);
            }
            setFieldValue(bean, field, refBean);
        }

        return bean;
    }

    /**
     * 字段赋值：支持默认值容错
     */
    private void setFieldValue(Object target, Field field, Object value) {
        MethodHandle setter = SETTER_CACHE.computeIfAbsent(field, f -> {
            try {
                return MethodHandles.lookup().unreflectSetter(f);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            setter.invoke(target, value);
        } catch (Throwable e1) {
            try {
                setter.invoke(target, ValueConverterFactory.convertIfPossible(field.getType(), value));
            } catch (Throwable e2) {
                try {
                    setter.invoke(target, TypeResolver.getDefaultValue(field.getType()));
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /**
     * 获取字段
     */
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
