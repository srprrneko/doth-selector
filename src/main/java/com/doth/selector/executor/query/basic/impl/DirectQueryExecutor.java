package com.doth.selector.executor.query.basic.impl;

import com.doth.selector.coordinator.mapper.ResultSetMapper;
import com.doth.selector.common.util.adapeter.EntityAdapter;
import com.doth.selector.executor.query.BasicKindQueryExecutor;

import java.util.LinkedHashMap;
import java.util.List;

// ----------------- 新增固定查询实现类 -----------------
// public class DirectQueryExecutor implements DirectQueryable {
public class DirectQueryExecutor<T> extends BasicKindQueryExecutor<T> {




    public List<T> query2Lst() {
        return coordinator.queryByMap(beanClass,(LinkedHashMap<String, Object>) null);
    }

    public List<T> query2Lst(T t) {
        LinkedHashMap<String, Object> condMap = EntityAdapter.extractNonNullFields(t);
        return coordinator.queryByMap(beanClass, condMap);
    }


    public List<T> query2Lst(LinkedHashMap<String, Object> cond) {
        return coordinator.queryByMap(beanClass, cond);
    }

    public List<T> query2Lst(LinkedHashMap<String, Object> cond, String condClause) {
        return coordinator.queryByMapVzClause(beanClass, cond, condClause);
    }



    public T query2T(LinkedHashMap<String, Object> cond) {
        return ResultSetMapper.getSingleResult(coordinator.queryByMap(beanClass, cond));
    }

    public T query2T(LinkedHashMap<String, Object> cond, String condClause) {
        return ResultSetMapper.getSingleResult(query2Lst(cond, condClause));
    }

}