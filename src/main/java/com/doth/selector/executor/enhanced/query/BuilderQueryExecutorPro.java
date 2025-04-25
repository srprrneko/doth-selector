package com.doth.selector.executor.enhanced.query;

import com.doth.selector.anno.Overload;
import com.doth.selector.coordinator.mapper.ResultSetMapper;
import com.doth.selector.executor.enhanced.JoinExecutor;
import com.doth.selector.executor.supports.builder.ConditionBuilder;

import java.util.List;
import java.util.function.Consumer;

public class BuilderQueryExecutorPro<T> extends JoinExecutor<T> {

    // protected BuilderQueryExecutorPro(Class<T> beanClass) {
    //     super(beanClass);
    // }



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