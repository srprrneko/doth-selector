package com.doth.stupidrefframe.selector.v1.executor.enhanced.query;

import com.doth.stupidrefframe.anno.Overload;
import com.doth.stupidrefframe.selector.v1.coordinator.supports.mapper.ResultSetMapper;
import com.doth.stupidrefframe.selector.v1.executor.enhanced.JoinExecutor;
import com.doth.stupidrefframe.selector.v1.util.adapeter.EntityAdapter;

import java.util.LinkedHashMap;
import java.util.List;


/**
 * 模版查询加强版 _ 联查映射
 * @param <T> 实体类泛型
 */
public class DirectQueryExecutorPro<T> extends JoinExecutor<T> {





    @Overload
    public List<T> query2Lst() {
        return coordinator.queryByMap(super.beanClass,(LinkedHashMap<String, Object>) null);
    }
    @Overload
    public List<T> query2Lst(T t) {
        LinkedHashMap<String, Object> condMap = EntityAdapter.extractNestedFields(t);
        return coordinator.queryByMap(super.beanClass, condMap);
    }
    @Overload
    public List<T> query2Lst(LinkedHashMap<String, Object> cond) {
        return coordinator.queryByMap(super.beanClass, cond);
    }

    @Overload
    public List<T> query2Lst(LinkedHashMap<String, Object> cond, String condClause) {
        return coordinator.queryByMapVzClause(super.beanClass, cond, condClause);
    }



    @Overload
    public T query2T(LinkedHashMap<String, Object> cond) {
        return ResultSetMapper.getSingleResult(query2Lst(cond));
    }

    @Overload
    public T query2T(LinkedHashMap<String, Object> cond, String condClause) {
        return ResultSetMapper.getSingleResult(query2Lst(cond, condClause));
    }

}