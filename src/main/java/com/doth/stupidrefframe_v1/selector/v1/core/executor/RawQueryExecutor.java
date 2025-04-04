package com.doth.stupidrefframe_v1.selector.v1.core.executor;

import java.util.List;

public class RawQueryExecutor<T> extends BasicKindQueryExecutor<T> {

    public RawQueryExecutor(Class<T> beanClass) {
        super(beanClass);
    }


    public List<T> query2Lst(String sql,  Object... params) {
        return coordinator.queryByRaw(beanClass, sql, params);
    }

    public List<T> query2Lst4Join(String sql, boolean isAutoAlias, Object... params) {
        return coordinator.queryByJoinRaw(beanClass, sql, isAutoAlias, params);
    }



    public T query2T(String sql, Object... params) {
        List<T> list = query2Lst(sql, params);
        return coordinator.getSingleResult(list);
    }

    public T query2T4Join(String sql, boolean isAutoAlias, Object... params) {
        List<T> list = query2Lst4Join(sql, isAutoAlias, params);
        return coordinator.getSingleResult(list);
    }

}