package com.doth.selector.core.factory;

import com.doth.selector.executor.query.BasicKindQueryExecutor;
import com.doth.selector.core.model.ExecutorType;

/**
 * 执行器工厂接口（允许自定义实现）
 */
public interface CreateExecutorFactory {

    /**
     * 创建执行器
     *
     * @param beanClass 目标实体类
     * @param type 执行器枚举
     * @return 执行器基类
     */
    BasicKindQueryExecutor<?> createExecutor(Class<?> beanClass, ExecutorType type);
}