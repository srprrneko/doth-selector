package com.doth.rubbish;

import java.util.*;

@Deprecated
public class ConditionBuilder {
    private final StringBuilder whereClause = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    // 添加条件（示例：eq("name", "Alice") → "name = ?"）
    public ConditionBuilder eq(String field, Object value) {
        appendCondition(field + " = ?", value);
        return this;
    }

    // 添加自定义条件（示例：raw("age > 18") → "age > 18"）
    public ConditionBuilder raw(String condition, Object... values) {
        appendCondition(condition, values);
        return this;
    }

    private void appendCondition(String condition, Object... values) {
        if (whereClause.length() > 0) {
            whereClause.append(" and ");
        }
        whereClause.append(condition);
        Collections.addAll(params, values);
    }

    public String getWhereClause() {
        return whereClause.toString();
    }

    public Object[] getParams() {
        return params.toArray();
    }
}