package com.doth.selector.executor.enhanced.query;

import com.doth.selector.anno.Overload;
import com.doth.selector.coordinator.mapper.ResultSetMapper;
import com.doth.selector.executor.enhanced.JoinExecutor;
import com.doth.selector.common.util.adapeter.EntityAdapter;

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
    public List<T> query2Lst(T t) { // 参数是t的方法重载, 表示以一个实体为条件传递过来
        LinkedHashMap<String, Object> condMap = EntityAdapter.extractNestedFields(t);
        return coordinator.queryByMap(super.beanClass, condMap);
    }
    @Overload
    public List<T> query2Lst(LinkedHashMap<String, Object> cond) { // 以map为条件的重载
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