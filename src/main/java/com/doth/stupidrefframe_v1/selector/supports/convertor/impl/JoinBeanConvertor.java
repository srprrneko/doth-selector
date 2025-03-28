package com.doth.stupidrefframe_v1.selector.supports.convertor.impl;

import com.doth.stupidrefframe_v1.anno.JoinColumn;
import com.doth.stupidrefframe_v1.selector.supports.SqlGenerator;
import com.doth.stupidrefframe_v1.selector.supports.convertor.BeanConvertor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// todo: 等待完善
@Deprecated
public class JoinBeanConvertor implements BeanConvertor {
    // 复用原始缓存结构
    private static final Map<Class<?>, JoinMapping> JOIN_CACHE = new ConcurrentHashMap<>();
    private static final Map<Field, MethodHandle> SETTER_CACHE = new ConcurrentHashMap<>();

    @Override
    public <T> T convert(ResultSet rs, Class<T> beanClass) throws Throwable {
        JoinMapping mapping = JOIN_CACHE.computeIfAbsent(beanClass, clz -> {
            try {
                return analyzeJoinStructure(clz, rs.getMetaData());
            } catch (Exception e) {
                throw new RuntimeException("解析联表结构失败", e);
            }
        });
        return buildJoinBean(rs, beanClass, mapping);
    }

    // 联表结构解析（修复字段映射问题）
    private JoinMapping analyzeJoinStructure(Class<?> clz, ResultSetMetaData meta) throws Exception {
        JoinMapping mapping = new JoinMapping();

        // 主表字段映射（复用原始逻辑）
        Map<String, Field> fieldCache = new HashMap<>();
        for (Field field : clz.getDeclaredFields()) {
            field.setAccessible(true);
            if (!field.isAnnotationPresent(JoinColumn.class)) {
                fieldCache.put(field.getName().toLowerCase(), field);
            }
        }

        // 构建主表列映射
        mapping.primaryMapping = new HashMap<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String columnLabel = meta.getColumnLabel(i);
            String camelName = SqlGenerator.snake2CamelCase(columnLabel);
            Field field = fieldCache.get(camelName.toLowerCase());

            if (field != null) {
                FieldMapping fm = new FieldMapping(field);
                mapping.primaryMapping.put(i, fm);
                SETTER_CACHE.putIfAbsent(field, fm.setter);
            }
        }

        // 处理关联对象
        for (Field field : clz.getDeclaredFields()) {
            JoinColumn joinAnno = field.getAnnotation(JoinColumn.class);
            if (joinAnno != null) {
                // 自动解析表别名（如d_name -> d）
                String tableAlias = resolveTableAlias(meta, joinAnno.referencedColumn());
                mapping.joinFields.put(field.getName(), new JoinMeta(
                        field.getType(),
                        joinAnno.fk(),
                        joinAnno.referencedColumn(),
                        tableAlias
                ));
            }
        }
        return mapping;
    }

    // 智能表别名解析（创新点）
    private String resolveTableAlias(ResultSetMetaData meta, String refColumn) throws SQLException {
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String column = meta.getColumnLabel(i);
            if (column.endsWith("_" + refColumn)) {
                return column.split("_")[0]; // 拆解d_id -> d
            }
        }
        return ""; // 默认处理
    }

    // 嵌套对象构建（修复表别名问题）
    private <T> T buildJoinBean(ResultSet rs, Class<T> beanClass, JoinMapping mapping) throws Throwable {
        T mainBean = beanClass.getDeclaredConstructor().newInstance();

        // 1. 填充主表字段
        for (Map.Entry<Integer, FieldMapping> entry : mapping.primaryMapping.entrySet()) {
            int index = entry.getKey();
            FieldMapping fm = entry.getValue();
            fm.setValue(mainBean, rs.getObject(index));
        }

        // 2. 构建关联对象
        for (Map.Entry<String, JoinMeta> entry : mapping.joinFields.entrySet()) {
            JoinMeta meta = entry.getValue();
            Object joinBean = meta.joinClass.getDeclaredConstructor().newInstance();

            // 通过表别名获取关联字段（关键修正）
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                String column = rs.getMetaData().getColumnLabel(i);
                if (column.startsWith(meta.tableAlias + "_")) {
                    String fieldName = SqlGenerator.snake2CamelCase(
                            column.substring(meta.tableAlias.length() + 1)
                    );
                    setFieldValue(joinBean, fieldName, rs.getObject(i));
                }
            }

            // 设置关联对象
            Field joinField = beanClass.getDeclaredField(entry.getKey());
            joinField.setAccessible(true);
            joinField.set(mainBean, joinBean);
        }
        return mainBean;
    }

    // 带缓存的字段赋值（优化性能）
    private void setFieldValue(Object bean, String fieldName, Object value) throws Throwable {
        Field field = bean.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);

        MethodHandle setter = SETTER_CACHE.computeIfAbsent(field,
                f -> {
                    try {
                        return MethodHandles.lookup().unreflectSetter(f);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
        setter.invoke(bean, value);
    }

    // 字段映射结构（移植自原始类）
    private static class FieldMapping {
        final Field field;
        final MethodHandle setter;

        FieldMapping(Field field) throws IllegalAccessException {
            this.field = field;
            this.setter = MethodHandles.lookup().unreflectSetter(field);
        }

        void setValue(Object target, Object value) throws Throwable {
            setter.invoke(target, value);
        }
    }

    // 联表元数据（新增表别名字段）
    private static class JoinMeta {
        final Class<?> joinClass;
        final String fkColumn;
        final String referencedColumn;
        final String tableAlias;

        JoinMeta(Class<?> joinClass, String fk, String refCol, String alias) {
            this.joinClass = joinClass;
            this.fkColumn = fk;
            this.referencedColumn = refCol;
            this.tableAlias = alias;
        }
    }

    private static class JoinMapping {
        Map<Integer, FieldMapping> primaryMapping;
        Map<String, JoinMeta> joinFields = new HashMap<>();
    }
}