package com.doth.stupidrefframe.selector.v1.executor.basic.query;

import com.doth.stupidrefframe.anno.Overload;
import com.doth.stupidrefframe.selector.v1.coordinator.supports.mapper.ResultSetMapper;
import com.doth.stupidrefframe.selector.v1.executor.basic.BasicKindQueryExecutor;
import com.doth.stupidrefframe.selector.v1.executor.supports.builder.ConditionBuilder;

import java.util.List;
import java.util.function.Consumer;

public class BuilderQueryExecutor<T> extends BasicKindQueryExecutor<T> {



    @Overload
    public List<T> query2Lst(Consumer<ConditionBuilder> setup) {
        ConditionBuilder builder = new ConditionBuilder();
        setup.accept(builder);
        return coordinator.queryByBuilder(beanClass, builder);
    }

    @Overload
    public List<T> query2Lst(String sql, Consumer<ConditionBuilder> conditionSetup) {
        ConditionBuilder builder = new ConditionBuilder();
        conditionSetup.accept(builder);
        return coordinator.queryByBuilderVzRaw(beanClass, sql, (ConditionBuilder) builder);
    }



    @Overload
    public T query2T(Consumer<ConditionBuilder> setup) {
        return ResultSetMapper.getSingleResult(query2Lst(setup));
    }

    @Overload
    public T query2T(String sql, Consumer<ConditionBuilder> conditionSetup) {
        return ResultSetMapper.getSingleResult(query2Lst(sql, conditionSetup));
    }

}