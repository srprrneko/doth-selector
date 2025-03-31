package com.doth.stupidrefframe_v1.selector.util;

import java.lang.reflect.Field;
import java.util.*;

// 新类：专注字段提取和参数处理
public class ConditionParamBuilder {
    public <T> LinkedHashMap<String, Object> extractNonNullFields(T entity) {
        LinkedHashMap<String, Object> condMap = new LinkedHashMap<>();
        if (entity == null) return condMap;

        for (Field field : entity.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object value = field.get(entity);
                if (value != null) {
                    condMap.put(field.getName(), value);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("字段提取失败: " + field.getName(), e);
            }
        }
        return condMap;
    }

    // ------------------ 工具方法：map正确转换object[]的方式 ------------------
    public Object[] buildParams(LinkedHashMap<String, Object> condBean) {
        List<Object> params = new ArrayList<>();
        if (condBean != null) {
            for (Map.Entry<String, Object> entry : condBean.entrySet()) {
                Object value = entry.getValue();

                // 展开集合为多个参数
                if (value instanceof Collection) { // 如果是集合
                    params.addAll((Collection<?>) value); // 将集合中的元素整块添加到参数列表中
                } else {
                    params.add(value); // 简单添加
                }
            }
        }
        return params.toArray(); // 直接返回数组
    }
}