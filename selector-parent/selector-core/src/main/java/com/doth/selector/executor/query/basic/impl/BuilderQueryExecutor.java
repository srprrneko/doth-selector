package com.doth.selector.executor.query.basic.impl;

import com.doth.selector.coordinator.ResultSetMapper;
import com.doth.selector.executor.query.BasicKindQueryExecutor;
import com.doth.selector.executor.supports.builder.ConditionBuilder;

import java.util.List;
import java.util.function.Consumer;

@Deprecated
public class BuilderQueryExecutor<T> extends BasicKindQueryExecutor<T> {



    public List<T> query2Lst(Consumer<ConditionBuilder> setup) {
        ConditionBuilder builder = new ConditionBuilder();
        setup.accept(builder);
        return coordinator.queryByBuilder(beanClass, builder);
    }

    public List<T> query2Lst(String sql, Consumer<ConditionBuilder> conditionSetup) {
        ConditionBuilder builder = new ConditionBuilder();
        conditionSetup.accept(builder);
        return coordinator.queryByBuilderVzRaw(beanClass, sql, (ConditionBuilder) builder);
    }



    @Deprecated
    public T query2T(Consumer<ConditionBuilder> setup) {
        return ResultSetMapper.getSingleResult(query2Lst(setup));
    }

    @Deprecated
    public T query2T(String sql, Consumer<ConditionBuilder> conditionSetup) {
        return ResultSetMapper.getSingleResult(query2Lst(sql, conditionSetup));
    }

}