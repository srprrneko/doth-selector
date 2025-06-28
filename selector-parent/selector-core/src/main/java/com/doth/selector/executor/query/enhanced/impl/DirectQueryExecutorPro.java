package com.doth.selector.executor.query.enhanced.impl;

import com.doth.selector.anno.Overload;
import com.doth.selector.executor.query.QueryExecutorTag;
import com.doth.selector.executor.query.enhanced.JoinExecutor;
import com.doth.selector.executor.supports.QueryList;
import com.doth.selector.supports.adapter.EntityAdapter;

import java.util.LinkedHashMap;


/**
 * 模版查询加强版 _ 联查映射
 * @param <T> 实体类泛型
 */
public class DirectQueryExecutorPro<T> extends JoinExecutor<T> implements QueryExecutorTag<T> {





    @Overload
    public QueryList<T> query() {
        return new QueryList<>(coordinator.queryByMap(super.beanClass, (LinkedHashMap<String, Object>) null));
    }

    /**
     * 参数是整个对象的方法重载, 服务一个参数为实体的场景, 避免手写键值对, 缺点是相同键的参数无法区分
     * @param t 条件型对象
     * @return 查询结果映射集
     */
    @Overload
    public QueryList<T> query(T t) {
        LinkedHashMap<String, Object> condMap = EntityAdapter.extractNestedFields2Map(t);
        return new QueryList<>(coordinator.queryByMap(super.beanClass, condMap));
    }
    @Overload
    public QueryList<T> query(LinkedHashMap<String, Object> cond) { // 以map为条件的重载
        return new QueryList<>(coordinator.queryByMap(super.beanClass, cond));
    }

    @Overload
    public QueryList<T> query(LinkedHashMap<String, Object> cond, String condClause) {
        return new QueryList<>(coordinator.queryByMapVzClause(super.beanClass, cond, condClause));
    }
    
    // @Overload
    // public T query2T(LinkedHashMap<String, Object> cond) {
    //     return ResultSetMapper.getSingleResult(query2Lst(cond));
    // }
    //
    // @Overload
    // public T query2T(LinkedHashMap<String, Object> cond, String condClause) {
    //     return ResultSetMapper.getSingleResult(query2Lst(cond, condClause));
    // }

}