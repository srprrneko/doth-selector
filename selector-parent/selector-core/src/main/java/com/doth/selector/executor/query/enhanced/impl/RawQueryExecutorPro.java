package com.doth.selector.executor.query.enhanced.impl;

import com.doth.selector.executor.query.QueryExecutorTag;
import com.doth.selector.executor.query.enhanced.JoinExecutor;
import com.doth.selector.executor.supports.QueryList;


public class RawQueryExecutorPro<T> extends JoinExecutor<T> implements QueryExecutorTag<T> {


    public QueryList<T> query(String sql, Object... params) {
        return new QueryList<>(coordinator.queryByRaw(beanClass, sql, params));
    }


    // public T query2T(String sql, boolean isAutoAlias, Object... params) {
    //     QueryList<T> QueryList = query2Lst(sql, isAutoAlias, params);
    //     return ResultSetMapper.getSingleResult(QueryList);
    // }
}