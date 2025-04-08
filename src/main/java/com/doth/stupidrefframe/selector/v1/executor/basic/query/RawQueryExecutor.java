package com.doth.stupidrefframe.selector.v1.executor.basic.query;

import com.doth.stupidrefframe.selector.v1.coordinator.supports.mapper.ResultSetMapper;
import com.doth.stupidrefframe.selector.v1.executor.basic.BasicKindQueryExecutor;

import java.util.List;

public class RawQueryExecutor<T> extends BasicKindQueryExecutor<T> {


    public List<T> query2Lst(String sql,  Object... params) {
        return coordinator.queryByRaw(beanClass, sql, params);
    }


    public T query2T(String sql, Object... params) {
        List<T> list = query2Lst(sql, params);
        return ResultSetMapper.getSingleResult(list);
    }

}