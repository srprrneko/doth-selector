package com.doth.selector.core.factory.impl;

import com.doth.selector.executor.basic.BasicKindQueryExecutor;
import com.doth.selector.executor.basic.query.BuilderQueryExecutor;
import com.doth.selector.executor.basic.query.DirectQueryExecutor;
import com.doth.selector.executor.basic.query.RawQueryExecutor;
import com.doth.selector.executor.enhanced.query.BuilderQueryExecutorPro;
import com.doth.selector.executor.enhanced.query.DirectQueryExecutorPro;
import com.doth.selector.executor.enhanced.query.RawQueryExecutorPro;
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
     * 泛型安全的空参构造+set模式
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
