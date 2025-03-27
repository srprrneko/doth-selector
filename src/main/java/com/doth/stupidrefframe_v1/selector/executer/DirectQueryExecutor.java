package com.doth.stupidrefframe_v1.selector.executer;

import java.util.LinkedHashMap;
import java.util.List;

// ----------------- 2. 新增直接条件查询实现类 -----------------
// public class DirectQueryExecutor implements DirectQueryable {
public class DirectQueryExecutor<T> extends AbstractQueryExecutor<T> {

    public DirectQueryExecutor(Class<T> beanClass) {
        super(beanClass);
    }


    public List<T> query2Lst() {
        return helper.mapSqlCond(beanClass,(LinkedHashMap<String, Object>) null);
    }

    public List<T> query2Lst(Object conditionBean) {
        LinkedHashMap<String, Object> condMap = helper.extractNonNullFields(conditionBean);
        return helper.mapSqlCond(beanClass, condMap);
    }

    public List<T> query2Lst(LinkedHashMap<String, Object> cond) {
        return helper.mapSqlCond(beanClass, cond);
    }

    public List<T> query2Lst(LinkedHashMap<String, Object> cond, String condClause) {
        return helper.mapSqlCond(beanClass, cond, condClause);
    }



    public T query2T(LinkedHashMap<String, Object> cond) {
        return helper.getSingleResult(helper.mapSqlCond(beanClass, cond));
    }

    public T query2T(Object conditionBean) {
        return helper.getSingleResult(query2Lst(conditionBean));
    }

    public T query2T(LinkedHashMap<String, Object> cond, String condClause) {
        return helper.getSingleResult(query2Lst(cond, condClause));
    }

}