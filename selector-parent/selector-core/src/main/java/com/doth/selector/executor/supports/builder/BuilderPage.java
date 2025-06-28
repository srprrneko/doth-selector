package com.doth.selector.executor.supports.builder;

import java.util.List;


@Deprecated // 暂不建议使用
class BuilderPage {
    private String cursorField; // 游标字段
    private Object cursorValue; // 游标值
    private int pageSize = 10; // 每页数量, 默认
    private boolean hasPagination = false; // 分页标识

    /**
     * 初始化
     * @param cursorField 游标字段
     * @param cursorValue 游标起始值
     * @param pageSize    每页数量
     */
    void initPage(String cursorField, Object cursorValue, int pageSize) {
        this.cursorField = cursorField;
        this.cursorValue = cursorValue;
        this.pageSize = pageSize;
        this.hasPagination = true;
    }

    /**
     * 追加分页条件
     * @param sql
     */
    void appendPaginationCondition(StringBuilder sql) {
        if (shouldAddCursorCondition()) {
            appendAndIfNeed(sql);
            sql.append(cursorField).append(" > ?");
        }
    }

    /**
     * 追加排序和限制
     * @param sql
     */
    void appendOrderAndLimit(StringBuilder sql) {
        if (hasPagination) {
            sql.append(" ORDER BY ").append(cursorField).append(" LIMIT ?");
        }
    }

    /**
     * 添加分页参数
     * @param params 参数集合
     */
    void addPaginationParams(List<Object> params) {
        if (hasPagination) {
            if (cursorValue != null) {
                params.add(cursorValue);
            }
            params.add(pageSize);
        }
    }

    // 判断是否应该添加游标条件
    private boolean shouldAddCursorCondition() {
        return hasPagination && cursorValue != null && cursorField != null;
    }

    private void appendAndIfNeed(StringBuilder sql) {
        if (sql.length() > 0) {
            sql.append(" and ");
        }
    }
}