package com.doth.selector.executor.supports.builder;

import java.util.List;

/**
 * 分页处理器（BuilderPage = 分页构建器）
 * 职责：管理分页参数并生成相关SQL片段
 */
class BuilderPage {
    private String cursorField;    // 游标字段
    private Object cursorValue;    // 游标值
    private int pageSize = 10;     // 每页数量
    private boolean hasPagination = false; // 分页标识

    /**
     * 初始化分页（initPage = Initialize Pagination）
     * @param cursorField 游标字段
     * @param cursorValue 游标起始值
     * @param pageSize 每页数量
     */
    void initPage(String cursorField, Object cursorValue, int pageSize) {
        this.cursorField = cursorField;
        this.cursorValue = cursorValue;
        this.pageSize = pageSize;
        this.hasPagination = true;
    }

    /**
     * 追加分页条件（appendPaginationCondition = 添加WHERE子句的分页条件）
     * @param sql SQL构建器
     */
    void appendPaginationCondition(StringBuilder sql) {
        if (shouldAddCursorCondition()) {
            appendAndIfNeed(sql);
            sql.append(cursorField).append(" > ?");
        }
    }

    /**
     * 追加排序和限制（appendOrderAndLimit = 添加ORDER BY和LIMIT）
     * @param sql SQL构建器
     */
    void appendOrderAndLimit(StringBuilder sql) {
        if (hasPagination) {
            sql.append(" ORDER BY ").append(cursorField).append(" LIMIT ?");
        }
    }

    /**
     * 添加分页参数（addPaginationParams = 收集分页相关参数）
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

    // 内部方法：是否需要添加游标条件
    private boolean shouldAddCursorCondition() {
        return hasPagination && cursorValue != null && cursorField != null;
    }

    // 内部方法：动态添加AND连接符
    private void appendAndIfNeed(StringBuilder sql) {
        if (sql.length() > 0) {
            sql.append(" and ");
        }
    }
}