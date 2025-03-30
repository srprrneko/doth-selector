package com.doth.stupidrefframe_v1.selector.executer;

import java.util.List;

public class RawQueryExecutor<T> extends AbstractQueryExecutor<T> {

    public RawQueryExecutor(Class<T> beanClass) {
        super(beanClass);
    }


    public List<T> query2Lst(String sql,  Object... params) {
        return helper.mapSqlCond(beanClass, sql, params);
    }

    public T query2T(String sql, Object... params) {
        List<T> list = query2Lst(sql, params);
        return helper.getSingleResult(list);
    }

}