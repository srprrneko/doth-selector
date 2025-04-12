package com.doth.stupidrefframe.selector.v1.anno.processor;

import com.doth.stupidrefframe.selector.v1.anno.Entity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class FieldTypeProcessor {

    /**
     * 验证类是否符合规则（禁止基本类型字段）
     */
    public static void validate(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(Entity.class)) {
            return; // 没有@Entity注解，直接跳过
        }

        List<String> primitiveFields = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            Class<?> type = field.getType();
            if (type.isPrimitive()) { // 检查字段类型是否为基本类型
                primitiveFields.add(field.getName());
            }
        }

        if (!primitiveFields.isEmpty()) {
            String errorMsg = String.format(
                "实体类 %s 包含基本类型字段: %s，请使用包装类型！",
                clazz.getSimpleName(),
                primitiveFields
            );
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * 批量验证多个类
     */
    public static void processClasses(Class<?>... classes) {
        for (Class<?> clazz : classes) {
            validate(clazz);
        }
    }
}