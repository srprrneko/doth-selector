package com.doth.stupidrefframe.selector.v1.loose.rubbish_since331;

/**
 * 抽象查询执行器基类 - 强制子类持有实体类型信息
 * @param <T> 目标实体类型
 */
@Deprecated
public abstract class BasicKindQueryExecutor<T> {
    protected final ExecuteCoordinator coordinator = new ExecuteCoordinator();
    // protected ExecuteCoordinatorService process;

    protected final Class<T> beanClass;


    // 强制子类必须传入实体类型
    protected BasicKindQueryExecutor(Class<T> beanClass) {
        this.beanClass = beanClass;
    }
    // protected BasicKindQueryExecutor(Class<T> beanClass) {
    //     this.beanClass = beanClass;
    //     this.process = new BasicExecuteCoordinator();
    // }
}