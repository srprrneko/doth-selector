package com.doth.selector.coordinator.supports.sql.builder;

import com.doth.selector.common.util.AnnoNamingConvertUtil;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;

import static com.doth.selector.common.util.AnnoNamingConvertUtil.camel2Snake;


public class SqlBuilder {
    public static String buildBaseSelect(Class<?> clazz) {
        String tableName = AnnoNamingConvertUtil.camel2Snake(clazz, clazz.getSimpleName());
        String fields = buildFieldList(clazz);
        return String.format("SELECT %s FROM %s", fields, tableName);
    }

    public static String buildFieldList(Class<?> clz) {
        StringBuilder sb = new StringBuilder();
        Field[] fields = clz.getDeclaredFields();
        for (Field field : fields) {
            String columnName = camel2Snake(field, field.getName());
            sb.append(columnName).append(",");
        }
        if (fields.length > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    // ----------------- 带自定义子句的全局sql -----------------
    public static String buildWhereClause(String baseSql, LinkedHashMap<String, Object> condBean, String strClause) {
        StringBuilder sb = new StringBuilder(baseSql);

        if (condBean != null && !condBean.isEmpty()) {
            sb.append(" where ");
            buildConditions(sb, condBean); // 调用共用方法
        }

        if (!strClause.isEmpty()) {
            sb.append(" ").append(strClause.trim());
        }

        return sb.toString();
    }





    // ----------------- 条件拼接逻辑（核心共用方法） -----------------
    private static void buildConditions(StringBuilder sqlBuilder, LinkedHashMap<String, Object> conditions) {
        conditions.forEach((column, value) -> {
            // 动态生成操作符
            String operator = ConditionResolverUtil.resolveOperator(value);
            // 处理 IN 子句占位符
            String placeholders = ConditionResolverUtil.resolvePlaceholders(value);

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