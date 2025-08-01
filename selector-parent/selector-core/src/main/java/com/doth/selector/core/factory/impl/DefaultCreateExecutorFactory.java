package com.doth.selector.core.factory.impl;

import com.doth.selector.executor.query.BasicKindQueryExecutor;
import com.doth.selector.executor.query.basic.impl.BuilderQueryExecutor;
import com.doth.selector.executor.query.basic.impl.DirectQueryExecutor;
import com.doth.selector.executor.query.basic.impl.RawQueryExecutor;
import com.doth.selector.executor.query.enhanced.impl.BuilderQueryExecutorPro;
import com.doth.selector.executor.query.enhanced.impl.DirectQueryExecutorPro;
import com.doth.selector.executor.query.enhanced.impl.RawQueryExecutorPro;
import com.doth.selector.core.model.ExecutorType;
import com.doth.selector.core.factory.CreateExecutorFactory;

import java.util.function.Supplier;

/**
 * 默认工厂实现（创建原有固定执行器）////;
 */
public class DefaultCreateExecutorFactory implements CreateExecutorFactory {
    @Override
    public BasicKindQueryExecutor<?> createExecutor(Class<?> beanClass, ExecutorType type) {
        switch (type) {
            case BUILDER:
                return createWithSetter(beanClass, BuilderQueryExecutor::new);
            case RAW:
                return createWithSetter(beanClass, RawQueryExecutor::new);
            case DIRECT:
                return createWithSetter(beanClass, DirectQueryExecutor::new);
            case DIRECT_PRO:
                return createWithSetter(beanClass, DirectQueryExecutorPro::new);
            case BUILDER_PRO:
                return createWithSetter(beanClass, BuilderQueryExecutorPro::new);
            case RAW_PRO:
                return createWithSetter(beanClass, RawQueryExecutorPro::new);
            default:
                throw new IllegalArgumentException("未知执行器类型: " + type);
        }
    }

    /**
     * 泛型安全的空参构造 + setter
     * <p>说明: 原本是 通过构造方法的 方法共享泛型, 但是发现每个类都要有一个构造方法干同样的事之后, 就改成了setter</p>
     * @param beanClass 目标Bean类型
     * @param supplier 空参构造函数引用
     * @return 正确配置的执行器
     */
    @SuppressWarnings("unchecked")
    private <T> BasicKindQueryExecutor<T> createWithSetter(Class<?> beanClass, Supplier<? extends BasicKindQueryExecutor<T>> supplier) {
        // 创建空参实例
        BasicKindQueryExecutor<T> executor = supplier.get();
        // 类型安全转换
        Class<T> typedClass = (Class<T>) beanClass;
        executor.setBeanClass(typedClass);
        return executor;
    }
}
