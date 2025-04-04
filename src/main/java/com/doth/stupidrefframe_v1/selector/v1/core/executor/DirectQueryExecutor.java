package com.doth.stupidrefframe_v1.selector.v1.core.executor;

import com.doth.stupidrefframe_v1.anno.Entity;
import com.doth.stupidrefframe_v1.selector.v1.supports.adapeter.EntityAdapter;

import java.util.LinkedHashMap;
import java.util.List;

// ----------------- 新增固定查询实现类 -----------------
// public class DirectQueryExecutor implements DirectQueryable {
public class DirectQueryExecutor<T> extends BasicKindQueryExecutor<T> {

    public DirectQueryExecutor(Class<T> beanClass) {
        super(beanClass);
    }



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
        return coordinator.getSingleResult(coordinator.queryByMap(beanClass, cond));
    }

    public T query2T(LinkedHashMap<String, Object> cond, String condClause) {
        return coordinator.getSingleResult(query2Lst(cond, condClause));
    }

}