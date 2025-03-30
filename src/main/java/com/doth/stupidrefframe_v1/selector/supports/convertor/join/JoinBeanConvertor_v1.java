package com.doth.stupidrefframe_v1.selector.supports.convertor.join;

import com.doth.stupidrefframe_v1.anno.JoinColumn;
import com.doth.stupidrefframe_v1.selector.supports.convertor.BeanConvertor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

public class JoinBeanConvertor_v1 implements BeanConvertor {
    // 连接类的缓存区, 值为JoinMapping(嵌套类的内部成员)
    private static final Map<Class<?>, JoinMapping> JOIN_CACHE = new HashMap<>();
    //
    private static final Map<Field, MethodHandle> SETTER_CACHE = new HashMap<>();

    @Override
    public <T> T convert(ResultSet rs, Class<T> beanClass) throws Throwable {
        ResultSetMetaData meta = rs.getMetaData();
        JoinMapping mapping = JOIN_CACHE.computeIfAbsent(beanClass, clz -> {
            try {
                return analyzeJoinStructure(clz, meta, "");
            } catch (Exception e) {
                throw new RuntimeException("解析联表结构失败: " + e.getMessage(), e);
            }
        });
        return buildJoinBean(rs, beanClass, mapping);
    }

    private JoinMapping analyzeJoinStructure(Class<?> clz, ResultSetMetaData meta, String prefix) throws Exception {
        JoinMapping mapping = new JoinMapping();

        for (Field field : clz.getDeclaredFields()) {
            field.setAccessible(true);
            JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
            if (joinColumn != null) {
                String fkColumn = joinColumn.fk();
                String refColumn = joinColumn.referencedColumn().isEmpty() ? "id" : joinColumn.referencedColumn();

                if (columnExists(meta, fkColumn)) {
                    Class<?> refClass = field.getType();
                    String nestedPrefix = field.getName() + "_";
                    JoinMapping refMapping = analyzeJoinStructure(refClass, meta, nestedPrefix);
                    mapping.addNestedMapping(field, refMapping, fkColumn, refColumn);
                }
            } else {
                String columnName = prefix + field.getName(); // id, name, department{department_id, department_name, }
                if (columnExists(meta, columnName)) {
                    mapping.addFieldMapping(field, columnName);
                }
            }
        }

        return mapping;
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

    private <T> T buildJoinBean(ResultSet rs, Class<T> beanClass, JoinMapping mapping) throws Throwable {
        T bean = beanClass.getDeclaredConstructor().newInstance();

        for (Map.Entry<Field, String> entry : mapping.getFieldMappings().entrySet()) {
            Field field = entry.getKey();
            Object value = rs.getObject(entry.getValue());
            setFieldValue(bean, field, value);
        }

        for (Map.Entry<Field, JoinMapping> entry : mapping.getNestedMappings().entrySet()) {
            Field field = entry.getKey();
            JoinMapping nestedMapping = entry.getValue();
            String fkColumn = mapping.getFkColumn(field);
            String refColumn = mapping.getRefColumn(field);

            Object fkValue = rs.getObject(fkColumn);
            Object refBean = buildJoinBean(rs, field.getType(), nestedMapping);

            Field refField = findField(field.getType(), refColumn);
            if (refField != null) {
                setFieldValue(refBean, refField, fkValue);
            }

            setFieldValue(bean, field, refBean);
        }

        return bean;
    }

    private void setFieldValue(Object target, Field field, Object value) throws Throwable {
        MethodHandle setter = SETTER_CACHE.computeIfAbsent(field, f -> {
            try {
                return MethodHandles.lookup().unreflectSetter(f);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        setter.invoke(target, value);
    }

    private Field findField(Class<?> clz, String name) {
        for (Field field : clz.getDeclaredFields()) {
            if (field.getName().equals(name)) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

    private static class JoinMapping {
        private final Map<Field, String> fieldMappings = new HashMap<>();
        private final Map<Field, JoinMapping> nestedMappings = new HashMap<>();
        private final Map<Field, String> fkColumns = new HashMap<>();
        private final Map<Field, String> refColumns = new HashMap<>();

        public void addFieldMapping(Field field, String column) {
            fieldMappings.put(field, column);
        }

        public void addNestedMapping(Field field, JoinMapping mapping, String fkColumn, String refColumn) {
            nestedMappings.put(field, mapping);
            fkColumns.put(field, fkColumn);
            refColumns.put(field, refColumn);
        }

        public Map<Field, String> getFieldMappings() {
            return fieldMappings;
        }

        public Map<Field, JoinMapping> getNestedMappings() {
            return nestedMappings;
        }

        public String getFkColumn(Field field) {
            return fkColumns.get(field);
        }

        public String getRefColumn(Field field) {
            return refColumns.get(field);
        }
    }
}