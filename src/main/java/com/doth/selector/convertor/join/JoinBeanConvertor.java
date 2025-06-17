package com.doth.selector.convertor.join;

import com.doth.selector.anno.Join;
import com.doth.selector.convertor.BeanConvertor;
import com.doth.selector.convertor.supports.ConvertDtoContext;
import com.doth.selector.convertor.supports.JoinConvertContext;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.doth.selector.convertor.supports.JoinConvertContext.JOIN_CACHE;
import static com.doth.selector.convertor.supports.ResultSetUtils.extractColumnLabels;

public class JoinBeanConvertor implements BeanConvertor {

    @Override
    public <T> T convert(ResultSet rs, Class<T> beanClass) throws Throwable {
        // 1. 解析实际类型 @DependOn
        Class<?> actualClass = ConvertDtoContext.resolveActualClass(beanClass);

        // 2. 提取列名
        ResultSetMetaData meta = rs.getMetaData();
        Set<String> columnSet = extractColumnLabels(meta);

        // 3. 生成 fingerprint（列集合指纹，用于分组缓存）
        String fingerprint = ConvertDtoContext.getFingerprint(columnSet);

        // 4. 一级缓存：按实际实体类分类
        Map<String, JoinConvertContext.MetaMap> metaGroup = JOIN_CACHE
                .computeIfAbsent(actualClass, k -> new ConcurrentHashMap<>());

        // 5. 二级缓存：按列集合指纹分类
        JoinConvertContext.MetaMap metaMap = metaGroup.get(fingerprint);
        if (metaMap == null) {
            try {
                metaMap = analyzeClzStruct(actualClass, columnSet, "");
                metaGroup.put(fingerprint, metaMap);
            } catch (Exception e) {
                throw new RuntimeException("解析联表结构失败: " + e.getMessage(), e);
            }
        }

        // 6. 构造实体对象
        Object entity;
        try {
            entity = buildJoinBean(rs, actualClass, metaMap);
        } catch (Throwable e) {
            throw new RuntimeException("构造实体对象失败: " + e.getMessage(), e);
        }

        // 7. DTO 模式：通过 ConvertDtoContext 获取并缓存构造器 + MethodHandle，高性能实例化 DTO
        if (!actualClass.equals(beanClass)) {
            try {
                Constructor<T> ctor = ConvertDtoContext.getDtoConstructor(beanClass, actualClass);
                MethodHandle mh = ConvertDtoContext.getConstructorHandle(ctor);
                T dto = (T) mh.invoke(entity);
                return dto;
            } catch (Throwable e) {
                throw new RuntimeException("DTO 构造失败: " + beanClass.getName(), e);
            }
        }

        // 8. 普通模式，直接返回实体
        return beanClass.cast(entity);
    }


    private JoinConvertContext.MetaMap analyzeClzStruct(Class<?> clz, Set<String> columnSet, String prefix) throws Exception {
        JoinConvertContext.MetaMap metaMap = new JoinConvertContext.MetaMap();

        // 缓存并复用字段列表
        Field[] fields = JoinConvertContext.CLASS_FIELDS_CACHE.computeIfAbsent(clz, c -> {
            Field[] fs = c.getDeclaredFields();
            for (Field f : fs) f.setAccessible(true);
            return fs;
        });

        // 构建按名查找缓存
        JoinConvertContext.FIELD_NAME_CACHE.computeIfAbsent(clz, c -> {
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

                // 处理子对象字段
                Field[] subFields = JoinConvertContext.CLASS_FIELDS_CACHE
                        .computeIfAbsent(field.getType(), c -> {
                            Field[] fs = c.getDeclaredFields();
                            for (Field f : fs) f.setAccessible(true);
                            return fs;
                        });
                String nestedPrefix = field.getName() + "_";
                boolean hasAnyNested = false;
                for (Field subField : subFields) {
                    if (columnSet.contains((nestedPrefix + subField.getName()).toLowerCase())) {
                        hasAnyNested = true;
                        break;
                    }
                }
                if (hasAnyNested) {
                    JoinConvertContext.MetaMap nestedMeta = analyzeClzStruct(field.getType(), columnSet, nestedPrefix);
                    metaMap.addNestedMeta(field, nestedMeta, fkColumn, refColumn);
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

    private <T> T buildJoinBean(ResultSet rs,
                                Class<T> beanClass,
                                JoinConvertContext.MetaMap metaMap) throws Throwable {
        T bean = beanClass.getDeclaredConstructor().newInstance();

        // 简单字段赋值
        for (Map.Entry<Field, String> entry : metaMap.getFieldMeta().entrySet()) {
            JoinConvertContext.setFieldValue(bean, entry.getKey(), rs.getObject(entry.getValue()));
        }

        // 嵌套对象赋值
        for (Map.Entry<Field, JoinConvertContext.MetaMap> entry : metaMap.getNestedMeta().entrySet()) {
            Field field = entry.getKey();
            JoinConvertContext.MetaMap nestedMeta = entry.getValue();
            String fkCol = metaMap.getFkColumn(field);
            String refCol = metaMap.getRefColumn(field);

            Object fkValue = null;
            try {
                fkValue = rs.getObject(fkCol);
            } catch (SQLException ignored) {
            }

            Object refBean = buildJoinBean(rs, field.getType(), nestedMeta);
            if (JoinConvertContext.isAllFieldsNull(refBean, nestedMeta)) {
                continue;
            }

            Field refField = JoinConvertContext.getField(field.getType(), refCol);
            Object refValueFromRs = null;
            try {
                refValueFromRs = rs.getObject(field.getName() + "_" + refField.getName());
            } catch (SQLException ignored) {
            }

            Object finalValue = fkValue != null && refValueFromRs != null
                    ? refValueFromRs
                    : (fkValue != null ? fkValue : refValueFromRs);

            if (finalValue != null) {
                JoinConvertContext.setFieldValue(refBean, refField, finalValue);
            }
            JoinConvertContext.setFieldValue(bean, field, refBean);
        }

        return bean;
    }
}
