package com.doth.stupidrefframe_v1.selector.util;

import java.util.Collection;
import java.util.Collections;

public class ConditionSqlParser {
    // 解析部分
    // ----------------- 操作符解析逻辑 -----------------
    public static String resolveOperator(Object value) {
        if (value instanceof Collection) {
            return " in (";
        } else if (value instanceof String && ((String) value).contains("%")) {
            return " like ";
        } else {
            return " = ";
        }
    }

    // ----------------- 占位符解析逻辑 -----------------
    public static String resolvePlaceholders(Object value) {
        if (value instanceof Collection) {
            int size = ((Collection<?>) value).size();
            return String.join(", ", Collections.nCopies(size, "?")) + ")";
        } else {
            return "?";
        }
    }

    // ----------------- "*" 号替换白名单 -----------------
    public static String replaceWildcard(String sql, String whiteList) {
        return sql.replaceAll("(?i)select\\s+\\*", "select " + whiteList);
    }
}