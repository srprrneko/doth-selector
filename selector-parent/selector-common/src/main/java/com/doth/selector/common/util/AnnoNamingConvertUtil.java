package com.doth.selector.common.util;

import com.doth.selector.anno.ColumnName;
import com.doth.selector.anno.TableName;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.function.Function;

/**
 * <p>注解驱动的名称解析器，优先读取注解值，否则基础转换</p>
 */
public class AnnoNamingConvertUtil {
    private static <T extends Annotation> String normalizeSql(
            AnnotatedElement element,
            Class<T> annotationClass,
            Function<T, String> nameExtractor,
            String defaultValue
    ) {
        if (element.isAnnotationPresent(annotationClass)) {
            T annotation = element.getAnnotation(annotationClass);
            return nameExtractor.apply(annotation);
        }
        // 无注解时，将默认值转为蛇形命名
        return NamingConvertUtil.camel2Snake(defaultValue);
    }


    /**
     * 表名转换
     */
    public static String camel2Snake(Class<?> clz, String defaultValue) {
        return normalizeSql(clz, TableName.class, TableName::value, defaultValue);
    }


    /**
     * 字段名转换
     */
    public static String camel2Snake(Field field, String defaultValue) {
        return normalizeSql(field, ColumnName.class, ColumnName::name, defaultValue);
    }
}
