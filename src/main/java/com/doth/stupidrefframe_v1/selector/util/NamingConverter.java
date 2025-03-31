package com.doth.stupidrefframe_v1.selector.util;

import com.doth.stupidrefframe_v1.anno.ColumnName;
import com.doth.stupidrefframe_v1.anno.TableName;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NamingConverter {
    private static StringBuilder sb;
    public static String camel2SnakeCase(String input) {
        if (input == null || input.isEmpty()) return "";
        sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i != 0) sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    // ----------------- 下划线的转化逻辑 -----------------
    public static String snake2CamelCase(String input) {
        if (input == null || input.isEmpty()) return "";
        sb = new StringBuilder();
        boolean nextUpper = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i); // 遍历每一个字符
            if (c == '_') {
                nextUpper = true; // 下划线后，下一个字母需要大写 (避免第一个字母大写)
            } else {
                if (nextUpper) { // 下一个字母需要大写
                    sb.append(Character.toUpperCase(c)); // 将下一个字母大写
                    nextUpper = false; // 重置
                } else {
                    sb.append(Character.toLowerCase(c));
                }
            }
        }
        return sb.toString();
    }


    public static String camel2SnakeCase(String sql, Boolean isRaw) {
        if (!isRaw) return camel2SnakeCase(sql);
        String regex = "(?<=[a-z0-9])[A-Z]";
        Matcher matcher = Pattern.compile(regex).matcher(sql);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, "_" + matcher.group().toLowerCase());
        }
        matcher.appendTail(buffer);
        return buffer.toString().replaceAll("_+", "_");
    }



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
        return camel2SnakeCase(defaultValue);
    }

    // ----------------- 表名转换 -----------------
    public static String camel2SnakeCase(Class<?> clz, String defaultValue) {
        return normalizeSql(clz, TableName.class, TableName::value, defaultValue);
    }

    // ----------------- 字段名转换 -----------------
    public static String camel2SnakeCase(Field field, String defaultValue) {
        return normalizeSql(field, ColumnName.class, ColumnName::name, defaultValue);
    }
}