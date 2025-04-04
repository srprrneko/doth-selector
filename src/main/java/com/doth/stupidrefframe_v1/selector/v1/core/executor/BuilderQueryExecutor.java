package com.doth.stupidrefframe_v1.selector.v1.core.executor;

import com.doth.stupidrefframe_v1.anno.Overload;
import com.doth.stupidrefframe_v1.selector.v1.supports.builder.ConditionBuilder;

import java.util.List;
import java.util.function.Consumer;

public class BuilderQueryExecutor<T> extends BasicKindQueryExecutor<T> {

    public BuilderQueryExecutor(Class<T> beanClass) {
        super(beanClass);
    }



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
        return coordinator.getSingleResult(query2Lst(setup));
    }

    @Overload
    public T query2T(String sql, Consumer<ConditionBuilder> conditionSetup) {
        return coordinator.getSingleResult(query2Lst(sql, conditionSetup));
    }

}