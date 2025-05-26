package com.doth.selector.common.util;

import com.doth.selector.annotation.ColumnName;
import com.doth.selector.annotation.TableName;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.function.Function;

/**
 * 注解驱动的名称解析器，优先读取注解值，无注解时调用基础转换
 */
public class AnnoNamingConvertUtil {
    private static <T extends Annotation> String normalizeSql(
            AnnotatedElement element,          // 参数1：被检查的注解元素（类或字段）
            Class<T> annotationClass,          // 参数2：要处理的注解类型（如@TableName）
            Function<T, String> nameExtractor, // 参数3：从注解中提取名称的函数
            String defaultValue                // 参数4：默认名称（无注解时使用）
    ) {
        // 检查元素是否带有指定注解
        if (element.isAnnotationPresent(annotationClass)) {
            // 获取注解实例
            T annotation = element.getAnnotation(annotationClass);
            // 从注解中提取名称（例如调用@TableName的value()方法）
            return nameExtractor.apply(annotation);
        }
        // 无注解时，将默认值转为蛇形命名
        return CamelSnakeConvertUtil.camel2SnakeCase(defaultValue);
    }


    // ----------------- 表名转换 -----------------
    public static String camel2Snake(Class<?> clz, String defaultValue) {
        return normalizeSql(clz, TableName.class, TableName::value, defaultValue);
    }


    // ----------------- 字段名转换 -----------------
    public static String camel2Snake(Field field, String defaultValue) {
        return normalizeSql(field, ColumnName.class, ColumnName::name, defaultValue);
    }
}
