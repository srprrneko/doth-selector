package com.doth.selector.executor.query.basic.impl;

import com.doth.selector.coordinator.ResultSetMapper;
import com.doth.selector.executor.query.BasicKindQueryExecutor;

import java.util.List;

@Deprecated //
public class RawQueryExecutor<T> extends BasicKindQueryExecutor<T> {


    public List<T> query2Lst(String sql,  Object... params) {
        return coordinator.queryByRaw(beanClass, sql, params);
    }


    public T query2T(String sql, Object... params) {
        List<T> list = query2Lst(sql, params);
        return ResultSetMapper.getSingleResult(list);
    }

}