package com.doth.selector.executor.query.enhanced.impl;

import com.doth.selector.executor.query.QueryExecutorTag;
import com.doth.selector.executor.query.enhanced.JoinExecutor;
import com.doth.selector.executor.supports.QueryList;

/**
 * Query 执行器 - 原生 SQL 模式（Pro 加强版）
 * <p>
 * <strong>适用场景</strong>
 * <ul>
 *   <li>适用于开发者自定义 SQL 查询的高级使用场景</li>
 *   <li>适用于无法由字段/注解构建的复杂 SQL 条件</li>
 *   <li>适用于拼接联表、函数调用、分页等任意 SQL 能力</li>
 * </ul>
 * <br>
 * <strong>使用约定</strong>
 * <ul>
 *   <li>需由用户自行编写完整 SQL 且确保字段名正确</li>
 *   <li>参数通过 {@code ?} 占位符传入，按顺序绑定</li>
 * </ul>
 * <br>
 * <strong>继承关系说明</strong>
 * <ul>
 *   <li>继承自 {@link JoinExecutor}，具备联表支持能力</li>
 *   <li>实现 {@link QueryExecutorTag} 标识接口，支持自动收集</li>
 * </ul>
 *
 * @param <T> 实体类型
 */
public class RawQueryExecutorPro<T> extends JoinExecutor<T> implements QueryExecutorTag<T> {

    /**
     * 原生 SQL 查询 - 参数可变
     * <p>执行一条由用户自定义的原始 SQL</p>
     * <p>SQL 中使用 {@code ?} 占位符绑定参数</p>
     *
     * @param sql 原始 SQL 语句（必须完整合法）
     * @param params SQL 参数列表，依照 ? 顺序绑定
     * @return 查询结果列表
     */
    public QueryList<T> query(String sql, Object... params) {
        return new QueryList<>(coordinator.queryByRaw(beanClass, sql, params));
    }


    // public T query2T(String sql, boolean isAutoAlias, Object... params) {
    //     QueryList<T> QueryList = query2Lst(sql, isAutoAlias, params);
    //     return ResultSetMapper.getSingleResult(QueryList);
    // }
}