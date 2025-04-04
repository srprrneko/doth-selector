package com.doth.stupidrefframe_v1.selector.v1.core.executor;

import com.doth.stupidrefframe_v1.selector.v1.core.coordinator.ExecuteCoordinator;
import com.doth.stupidrefframe_v1.selector.v1.supports.convertor.ConvertorType;

/**
 * 抽象查询执行器基类 - 强制子类持有实体类型信息
 * @param <T> 目标实体类型
 */
public abstract class BasicKindQueryExecutor<T> {
    protected final Class<T> beanClass;
    protected final ExecuteCoordinator coordinator = new ExecuteCoordinator();


    // 强制子类必须传入实体类型
    protected BasicKindQueryExecutor(Class<T> beanClass) {
        this.beanClass = beanClass;
    }
}