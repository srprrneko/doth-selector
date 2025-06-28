package com.doth.selector.convertor.join;

import com.doth.selector.anno.Join;
import com.doth.selector.common.util.NamingConvertUtil;
import com.doth.selector.convertor.BeanConvertor;
import com.doth.selector.convertor.supports.ConvertDtoContext;
import com.doth.selector.convertor.supports.JoinConvertContext;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.doth.selector.convertor.supports.JoinConvertContext.*;
import static com.doth.selector.convertor.supports.ResultSetUtils.extractColumnLabels;

public class JoinBeanConvertor implements BeanConvertor {

    @Override
    public <T> T convert(ResultSet rs, Class<T> beanClass) throws Throwable {
        // 1. 通过 @DependOn 解析原类型
        Class<?> actualClass = ConvertDtoContext.resolveActualClass(beanClass);

        // 2. 提取结果集列名
        Set<String> columnSet = extractColumnLabels(rs.getMetaData());


        // 3. 查询缓存
        Map<String, JoinConvertContext.MetaMap> metaGroup = JOIN_CACHE.computeIfAbsent(actualClass, k -> new ConcurrentHashMap<>());

        // 4.获取缓存中的 类信息元
        // 4.1 准备查询列表指纹
        String fingerprint = ConvertDtoContext.getFingerprint(columnSet);
        JoinConvertContext.MetaMap metaMap = metaGroup.computeIfAbsent(fingerprint, fp -> {
            try {
                return analyzeClzStruct(actualClass, columnSet, "");
            } catch (Exception e) {
                throw new RuntimeException("解析联表结构失败: " + e.getMessage(), e);
            }
        });

        // 5. 通过缓存中的 类信息元 构建实际实体
        Object entity;
        try {
            entity = buildJoinBean(rs, actualClass, metaMap);
        } catch (Throwable e) {
            throw new RuntimeException("构造实体对象失败: " + e.getMessage(), e);
        }

        // 6. DTO 构造
        if (!actualClass.equals(beanClass)) {
            try {
                Constructor<T> ctor = ConvertDtoContext.getDtoConstructor(beanClass, actualClass);
                MethodHandle mh = ConvertDtoContext.getConstructorHandle(ctor);
                return (T) mh.invoke(entity);
            } catch (Throwable e) {
                throw new RuntimeException("DTO 构造失败: " + beanClass.getName(), e);
            }
        }

        return beanClass.cast(entity);
    }

    private JoinConvertContext.MetaMap analyzeClzStruct(Class<?> clz, Set<String> columnSet, String prefix) throws Exception {
        JoinConvertContext.MetaMap metaMap = new JoinConvertContext.MetaMap();

        // 缓存并复用字段
        Field[] fields = JoinConvertContext.getFields(clz);


        for (Field field : fields) {
            String snakeName = NamingConvertUtil.camel2SnakeCase(field.getName());
            String colName = prefix + snakeName;

            if (field.isAnnotationPresent(Join.class)) {
                Join join = field.getAnnotation(Join.class);
                String fkColumn = join.fk();
                String refColumn = join.refFK();

                Field[] subFields = JoinConvertContext.getFields(clz);


                String nestedPrefix = snakeName + "_";
                boolean anyPresent = false;
                for (Field subField : subFields) {
                    String nestedCol = nestedPrefix + NamingConvertUtil.camel2SnakeCase(subField.getName());
                    if (columnSet.contains(nestedCol)) {
                        anyPresent = true;
                        break;
                    }
                }

                if (anyPresent) {
                    JoinConvertContext.MetaMap nested = analyzeClzStruct(field.getType(), columnSet, nestedPrefix);
                    metaMap.addNestedMeta(field, nested, fkColumn, refColumn);
                }

            } else if (columnSet.contains(colName)) {
                metaMap.addFieldMeta(field, colName);
            }
        }

        return metaMap;
    }

    private <T> T buildJoinBean(ResultSet rs, Class<T> beanClass, JoinConvertContext.MetaMap metaMap) throws Throwable {
        T bean = beanClass.getDeclaredConstructor().newInstance();

        for (Map.Entry<Field, String> entry : metaMap.getFieldMeta().entrySet()) {
            setFieldValue(bean, entry.getKey(), rs, entry.getValue());
        }

        for (Map.Entry<Field, JoinConvertContext.MetaMap> entry : metaMap.getNestedMeta().entrySet()) {
            Field field = entry.getKey();
            JoinConvertContext.MetaMap nestedMeta = entry.getValue();
            String fkCol = metaMap.getFkColumn(field);
            String refCol = metaMap.getRefColumn(field);

            Object fkValue = safeGetObject(rs, fkCol);
            Object refBean = buildJoinBean(rs, field.getType(), nestedMeta);
            if (isAllFieldsNull(refBean, nestedMeta)) continue;

            Field refField = getField(field.getType(), NamingConvertUtil.snake2CamelCase(refCol));
            String nestedCol = NamingConvertUtil.camel2SnakeCase(field.getName()) + "_" + NamingConvertUtil.camel2SnakeCase(refField.getName());

            Object refVal = safeGetObject(rs, nestedCol);
            if (fkValue != null || refVal != null) {
                setFieldValue(refBean, refField, rs, nestedCol);
            }

            setFieldValue(bean, field, refBean);
        }

        return bean;
    }

    private Object safeGetObject(ResultSet rs, String colName) {
        try {
            return rs.getObject(colName);
        } catch (SQLException ignored) {
            return null;
        }
    }

}
