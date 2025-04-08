package com.doth.loose.rubbish;

import com.doth.stupidrefframe.selector.v1.executor.supports.builder.ConditionBuilder;

import java.util.List;
import java.util.function.Consumer;

@Deprecated
// ----------------- 构建器模式专属接口 -----------------
public interface BuilderQueryable extends EntityQueryable {
    default <T> List<T> query2Lst(Class<T> beanClass, Consumer<ConditionBuilder> setup) {
        ConditionBuilder builder = new ConditionBuilder();
        setup.accept(builder);
        return getHelper().queryByBuilder(beanClass, builder);
    }

    default <T> T query2T(Class<T> beanClass, Consumer<ConditionBuilder> setup) {
        return getHelper().getSingleResult(query2Lst(beanClass, setup));
    }
}