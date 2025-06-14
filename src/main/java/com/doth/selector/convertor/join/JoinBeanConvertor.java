package com.doth.selector.convertor.join;

import com.doth.selector.anno.DependOn;
import com.doth.selector.anno.Join;
import com.doth.selector.convertor.BeanConvertor;
import com.doth.selector.convertor.supports.JoinConvertContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import static com.doth.selector.convertor.supports.ResultSetUtils.extractColumnLabels;

public class JoinBeanConvertor implements BeanConvertor {

    @Override
    public <T> T convert(ResultSet rs, Class<T> beanClass) throws Throwable {
        Class<?> actualClass = beanClass;
        // DTO 模式处理
        if (beanClass.isAnnotationPresent(DependOn.class)) {
            DependOn dependOn = beanClass.getAnnotation(DependOn.class);
            String classPath = dependOn.clzPath();
            try {
                actualClass = Class.forName(classPath);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("无法加载 @DependOn 指定的类: " + classPath, e);
            }
        }

        // 提取列名
        ResultSetMetaData meta = rs.getMetaData();
        Set<String> columnSet = extractColumnLabels(meta);

        // 生成 columnSet 指纹
        // long start = System.currentTimeMillis();
        String fingerprint = JoinConvertContext.fingerprint(columnSet);

        // 一级缓存：按类分类
        Map<String, JoinConvertContext.MetaMap> metaGroup = JoinConvertContext.JOIN_CACHE
                .computeIfAbsent(actualClass, k -> new ConcurrentHashMap<>());

        // 二级缓存：按列集合分类
        JoinConvertContext.MetaMap metaMap = metaGroup.get(fingerprint);
        if (metaMap == null) {
            try {
                metaMap = analyzeClzStruct(actualClass, columnSet, "");
                metaGroup.put(fingerprint, metaMap);
            } catch (Exception e) {
                throw new RuntimeException("解析联表结构失败: " + e.getMessage(), e);
            }
        }


        // long end = System.currentTimeMillis();
        // System.out.printf("\n计算指纹花费: %s", (end - start));

        // 构造实体
        Object entity;
        try {
            entity = buildJoinBean(rs, actualClass, metaMap);
        } catch (Throwable e) {
            throw new RuntimeException("构造实体对象失败: " + e.getMessage(), e);
        }

        // 如果是 DTO，调用对应构造函数
        if (!actualClass.equals(beanClass)) {
            try {
                Constructor<T> constructor = beanClass.getConstructor(entity.getClass());
                return constructor.newInstance(entity);
            } catch (Exception e) {
                throw new RuntimeException("DTO 构造失败: " + beanClass.getName(), e);
            }
        }

        return beanClass.cast(entity);
    }

    /**
     * 分析类结构，生成 MetaMap（递归处理嵌套 Join）
     * @param clz 实体类类对象
     * @param columnSet
     * @param prefix
     * @return
     * @throws Exception
     */
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

                assert join != null;
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

    /**
     * 通过
     * @param rs
     * @param beanClass
     * @param metaMap
     * @return
     * @param <T>
     * @throws Throwable
     */
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
            try { fkValue = rs.getObject(fkCol); } catch (SQLException ignored) { }

            Object refBean = buildJoinBean(rs, field.getType(), nestedMeta);
            if (JoinConvertContext.isAllFieldsNull(refBean, nestedMeta)) {
                continue;
            }

            Field refField = JoinConvertContext.getField(field.getType(), refCol);
            Object refValueFromRs = null;
            try {
                refValueFromRs = rs.getObject(field.getName() + "_" + refField.getName());
            } catch (SQLException ignored) { }

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
