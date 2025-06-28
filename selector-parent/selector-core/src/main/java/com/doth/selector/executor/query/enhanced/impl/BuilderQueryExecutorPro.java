package com.doth.selector.executor.query.enhanced.impl;

import com.doth.selector.anno.Overload;
import com.doth.selector.executor.query.QueryExecutorTag;
import com.doth.selector.executor.query.enhanced.JoinExecutor;
import com.doth.selector.executor.supports.QueryList;
import com.doth.selector.executor.supports.builder.ConditionBuilder;

import java.util.function.Consumer;

public class BuilderQueryExecutorPro<T> extends JoinExecutor<T> implements QueryExecutorTag<T> {

    // protected BuilderQueryExecutorPro(Class<T> beanClass) {
    //     super(beanClass);
    // }



    @Overload
    public QueryList<T> query(Consumer<ConditionBuilder<T>> setup) {
        ConditionBuilder<T> builder = new ConditionBuilder<>(beanClass);
        setup.accept(builder);
        return new QueryList<>(coordinator.queryByBuilder(beanClass, builder));
    }

    @Overload
    @SuppressWarnings(value = "unchecked")
    public QueryList<T> query(Consumer<ConditionBuilder<T>> setup, boolean dtoModel) {
        ConditionBuilder<T> builder = new ConditionBuilder<>(beanClass); // beanClass = 实体类
        setup.accept(builder);

        return new QueryList<>(coordinator.queryByBuilder((Class<T>) dtoClass, builder)); // 查询映射使用 dto
    }


    @Overload
    @Deprecated
    public QueryList<T> query(String sql, Consumer<ConditionBuilder<T>> conditionSetup) {
        ConditionBuilder<T> builder = new ConditionBuilder<T>();
        conditionSetup.accept(builder);
        return new QueryList<>(coordinator.queryByBuilderVzRaw(beanClass, sql, (ConditionBuilder<T>) builder));
    }


    //
    // @Overload
    // public T query2T(Consumer<ConditionBuilder<T>> setup) {
    //     return ResultSetMapper.getSingleResult(query2Lst(setup));
    // }
    //
    // @Overload
    // public T query2T(String sql, Consumer<ConditionBuilder<T>> conditionSetup) {
    //     return ResultSetMapper.getSingleResult(query2Lst(sql, conditionSetup));
    // }

}