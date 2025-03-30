package com.doth.stupidrefframe_v1.selector.core.executor;

import com.doth.stupidrefframe_v1.selector.core.coordinator.QueryCoordinator_v1;

/**
 * 抽象查询执行器基类 - 强制子类持有实体类型信息
 * @param <T> 目标实体类型
 */
public abstract class BasicKindQueryExecutor<T> {
    protected final Class<T> beanClass;
    protected final QueryCoordinator_v1 coordinator = new QueryCoordinator_v1();

    // 强制子类必须传入实体类型
    protected BasicKindQueryExecutor(Class<T> beanClass) {
        this.beanClass = beanClass;
    }
}