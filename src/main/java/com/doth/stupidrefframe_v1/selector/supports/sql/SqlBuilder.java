package com.doth.stupidrefframe_v1.selector.supports.sql;

import com.doth.stupidrefframe_v1.selector.util.ConditionSqlParser;
import com.doth.stupidrefframe_v1.selector.util.NamingConverter;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;

public class SqlBuilder {
    public static String buildBaseSelect(Class<?> clazz) {
        String tableName = NamingConverter.camel2SnakeCase(clazz, clazz.getSimpleName());
        String fields = buildFieldList(clazz);
        return String.format("SELECT %s FROM %s", fields, tableName);
    }

    protected static String buildFieldList(Class<?> clz) {
        StringBuilder sb = new StringBuilder();
        Field[] fields = clz.getDeclaredFields();
        for (Field field : fields) {
            String columnName = NamingConverter.camel2SnakeCase(field, field.getName());
            sb.append(columnName).append(",");
        }
        if (fields.length > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    // ----------------- 条件拼接逻辑（核心共用方法） -----------------
    public static void buildConditions(StringBuilder sqlBuilder, LinkedHashMap<String, Object> conditions) {
        conditions.forEach((column, value) -> {
            // 动态生成操作符
            String operator = ConditionSqlParser.resolveOperator(value);
            // 处理 IN 子句占位符
            String placeholders = ConditionSqlParser.resolvePlaceholders(value);

            // 拼接条件
            sqlBuilder.append(column)
                    .append(operator)
                    .append(placeholders)
                    .append(" and ");
        });
        // 删除末尾多余的 AND
        sqlBuilder.setLength(sqlBuilder.length() - 5);
    }

}