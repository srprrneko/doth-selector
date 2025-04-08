package com.doth.loose.rubbish;


import com.doth.stupidrefframe.anno.ColumnName;
import com.doth.stupidrefframe.anno.TableName;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @project: test01
 * @package: com.example.inserttry.newdbutiltry01
 * @author: doth
 * @creTime: 2025-03-23  19:29
 * @desc: sql映射最外层的类
 * @v: 1.0
 */
@Deprecated
public class GeneratorHelper {
    protected static StringBuilder sb;
    protected static String tableName;


    // todo : 废弃方法
    // // ----------------- 查询方法中抽象出的共用方法 -----------------
    // protected static String globalSelect(String columns, String tableName, LinkedHashMap<String, Object> conditions) {
    //     // sqlgenerator 结构搭建
    //     sb = new StringBuilder("select ").append(columns).append(" from ").append(tableName);
    //     if (conditions == null || conditions.isEmpty()) {
    //         return sb.toString();
    //     }
    //     else {
    //         sb.append(" where ");
    //         conditions.forEach((column, value) -> {
    //             // 根据值类型动态选择操作符
    //             String operator = " = ?";
    //             if (value instanceof Collection) { // 如果是集合,
    //                 operator = " in (?)"; // 使用in
    //             } else if (value instanceof String && ((String) value).contains("%")) { // 如果条件包含%,
    //                 operator = " like ?"; // 使用like
    //             }
    //             sb.append(column).append(operator).append(" and ");
    //         });
    //         // 删除多余的and
    //         sb.delete(sb.length() - 5, sb.length());
    //         return sb.toString();
    //     }
    // }
    // protected static <T> String globalSelect(String baseSql, Map<String, Object> condBean, String strClause) {
    //     sb = new StringBuilder(baseSql);
    //
    //     if (condBean != null && !condBean.isEmpty()) {
    //         sb.append(" where ");
    //         for (Map.Entry<String, Object> entry : condBean.entrySet()) {
    //             String column = entry.getKey();
    //             Object value = entry.getValue();
    //
    //             // 仅处理占位符逻辑，不实际存储参数值
    //             if (value instanceof Collection) {
    //                 int size = ((Collection<?>) value).size();
    //                 String placeholders = String.join(", ", Collections.nCopies(size, "?"));
    //                 sb.append(column).append(" in (").append(placeholders).append(")");
    //             } else if (value instanceof String && ((String) value).contains("%")) {
    //                 sb.append(column).append(" like ?");
    //             } else {
    //                 sb.append(column).append(" = ?");
    //             }
    //             sb.append(" and ");
    //         }
    //         sb.setLength(sb.length() - 5); // 删除末尾的 " AND "
    //     }
    //
    //     if (!strClause.isEmpty()) {
    //         sb.append(" ").append(strClause.trim());
    //     }
    //
    //     return sb.toString();
    // }
    // ----------------- 表名转换方法 -----------------
    protected static String getBaseSql(Class<?> clz) {
        tableName = entityNameCvn2Snake(clz); // 通过类转换表名
        String fields = buildFieldList(clz); // 通过类获取字段列表

        return new StringBuilder("select ")
                .append(fields)
                .append(" from ")
                .append(tableName).toString();
    }

    protected static String entityNameCvn2Snake(Class<?> clz) {
        return camel2SnakeCase(clz, clz.getSimpleName());
    }

    // ----------------- 不带自定义子句的全局sql -----------------
    protected static String globalSelect(String baseSql, LinkedHashMap<String, Object> conditions) {
        StringBuilder sb = new StringBuilder(baseSql);

        if (conditions != null && !conditions.isEmpty()) {
            sb.append(" where ");
            appendConditions(sb, conditions); // 调用共用方法
        }
        return sb.toString();
    }
    // ----------------- 带自定义子句的全局sql -----------------
    protected static String globalSelect(String baseSql, LinkedHashMap<String, Object> condBean, String strClause) {
        StringBuilder sb = new StringBuilder(baseSql);

        if (condBean != null && !condBean.isEmpty()) {
            sb.append(" where ");
            appendConditions(sb, condBean); // 调用共用方法
        }

        if (!strClause.isEmpty()) {
            sb.append(" ").append(strClause.trim());
        }

        return sb.toString();
    }
    // ----------------- 字段列表(c1,c2...)构建方法 -----------------
    protected static String buildFieldList(Class<?> clz) {
        StringBuilder sb = new StringBuilder();
        Field[] fields = clz.getDeclaredFields();
        for (Field field : fields) {
            String columnName = camel2SnakeCase(field, field.getName());
            sb.append(columnName).append(",");
        }
        if (fields.length > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public static String normalizeSql4Raw(Class<?> beanClass, String sql) {
        // 1. 驼峰转下划线
        String snakeSql = camel2SnakeCase(sql, true);
        // 2. 获取白名单字段字符串（复用已有方法）
        String whiteList = buildFieldList(beanClass);
        // 3. 替换*为白名单字段
        return replaceWildcard(snakeSql, whiteList);
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

    // 替换*的逻辑（直接使用拼接后的字段字符串）
    private static String replaceWildcard(String sql, String whiteList) {
        return sql.replaceAll("(?i)select\\s+\\*", "select " + whiteList);
    }



    // ####################### 私有部分 ####################### !!!!!!
    // ----------------- 条件拼接逻辑（核心共用方法） -----------------
    private static void appendConditions(StringBuilder sqlBuilder, LinkedHashMap<String, Object> conditions) {
        conditions.forEach((column, value) -> {
            // 动态生成操作符
            String operator = resolveOperator(value);
            // 处理 IN 子句占位符
            String placeholders = resolvePlaceholders(value);

            // 拼接条件
            sqlBuilder.append(column)
                    .append(operator)
                    .append(placeholders)
                    .append(" and ");
        });
        // 删除末尾多余的 AND
        sqlBuilder.setLength(sqlBuilder.length() - 5);
    }



    // 解析部分
    // ----------------- 操作符解析逻辑 -----------------
    private static String resolveOperator(Object value) {
        if (value instanceof Collection) {
            return " in (";
        } else if (value instanceof String && ((String) value).contains("%")) {
            return " like ";
        } else {
            return " = ";
        }
    }

    // ----------------- 占位符解析逻辑 -----------------
    private static String resolvePlaceholders(Object value) {
        if (value instanceof Collection) {
            int size = ((Collection<?>) value).size();
            return String.join(", ", Collections.nCopies(size, "?")) + ")";
        } else {
            return "?";
        }
    }
    //
    // // ----------------- 实体的表名转换 -----------------
    // private static String camel2SnakeCase(Class<?> clz, String defaultValue) {
    //     if (clz.isAnnotationPresent(TableName.class)) {
    //         return clz.getAnnotation(TableName.class).value();
    //     }
    //     return camel2SnakeCase(defaultValue);
    // }
    //
    // // ----------------- 字段的列名转换 -----------------
    // private static String camel2SnakeCase(Field field, String defaultValue) {
    //     if (field.isAnnotationPresent(ColumnName.class)) {
    //         return field.getAnnotation(ColumnName.class).name();
    //     }
    //     return camel2SnakeCase(defaultValue);
    // }

    // 将上方共有逻辑抽取出来
    // ----------------- 公共注解检查逻辑 -----------------
    private static <T extends Annotation> String getAnnotatedName(
            AnnotatedElement element,          // 参数1：被检查的注解元素（类或字段）
            Class<T> annotationClass,          // 参数2：要处理的注解类型（如@TableName）
            Function<T, String> nameExtractor,  // 参数3：从注解中提取名称的函数
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
    private static String camel2SnakeCase(Class<?> clz, String defaultValue) {
        return getAnnotatedName(clz, TableName.class, TableName::value, defaultValue);
    }

    // ----------------- 字段名转换 -----------------
    private static String camel2SnakeCase(Field field, String defaultValue) {
        return getAnnotatedName(field, ColumnName.class, ColumnName::name, defaultValue);
    }


    // ----------------- 驼峰的转化逻辑 -----------------
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

}
