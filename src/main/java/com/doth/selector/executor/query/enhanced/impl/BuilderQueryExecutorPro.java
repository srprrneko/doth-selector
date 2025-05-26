package com.doth.selector.executor.query.enhanced.impl;

import com.doth.selector.annotation.Overload;
import com.doth.selector.coordinator.mapper.ResultSetMapper;
import com.doth.selector.executor.query.enhanced.JoinExecutor;
import com.doth.selector.executor.supports.builder.ConditionBuilder;

import java.util.List;
import java.util.function.Consumer;

public class BuilderQueryExecutorPro<T> extends JoinExecutor<T>  {

    // protected BuilderQueryExecutorPro(Class<T> beanClass) {
    //     super(beanClass);
    // }



    @Overload
    public List<T> query2Lst(Consumer<ConditionBuilder<T>> setup) {
        ConditionBuilder<T> builder = new ConditionBuilder<>(beanClass);
        setup.accept(builder);
        return coordinator.queryByBuilder(beanClass, builder);
    }

    @Overload
    @Deprecated
    public List<T> query2Lst(String sql, Consumer<ConditionBuilder<T>> conditionSetup) {
        ConditionBuilder<T> builder = new ConditionBuilder<T>();
        conditionSetup.accept(builder);
        return coordinator.queryByBuilderVzRaw(beanClass, sql, (ConditionBuilder<T>) builder);
    }



    @Overload
    public T query2T(Consumer<ConditionBuilder<T>> setup) {
        return ResultSetMapper.getSingleResult(query2Lst(setup));
    }

    @Overload
    public T query2T(String sql, Consumer<ConditionBuilder<T>> conditionSetup) {
        return ResultSetMapper.getSingleResult(query2Lst(sql, conditionSetup));
    }

}