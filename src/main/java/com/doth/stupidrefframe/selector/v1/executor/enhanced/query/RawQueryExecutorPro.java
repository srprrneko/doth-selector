package com.doth.stupidrefframe.selector.v1.executor.enhanced.query;

import com.doth.stupidrefframe.selector.v1.coordinator.supports.mapper.ResultSetMapper;
import com.doth.stupidrefframe.selector.v1.executor.enhanced.JoinExecutor;

import java.util.List;

public class RawQueryExecutorPro<T> extends JoinExecutor<T> {


    public List<T> query2Lst(String sql, Object... params) {
        return coordinator.queryByRaw(beanClass, sql, params);
    }


    public T query2T(String sql, boolean isAutoAlias, Object... params) {
        List<T> list = query2Lst(sql, isAutoAlias, params);
        return ResultSetMapper.getSingleResult(list);
    }
}