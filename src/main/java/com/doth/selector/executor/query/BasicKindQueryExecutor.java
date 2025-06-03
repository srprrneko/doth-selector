package com.doth.selector.executor.query;

import com.doth.selector.coordinator.core.process.BasicExecuteCoordinator;
import com.doth.selector.coordinator.core.ExecuteCoordinatorService;

/**
 * 抽象查询执行器基类 - 强制子类持有实体类型信息
 * @param <T> 目标实体类型
 */
public abstract class BasicKindQueryExecutor<T> {

    protected ExecuteCoordinatorService coordinator;

    // protected final Class<T> beanClass;
    protected Class<T> beanClass;

    protected Class<?> dtoClass;



    // 强制子类必须传入实体类型
    protected BasicKindQueryExecutor() { // 开闭原则, 新增参数coordinator不需要selector门面类修改, 由子类自己重写setCoordinator方法即可
        setCoordinator(new BasicExecuteCoordinator()); // 默认使用基础协调器
    }

    public void setBeanClass(Class<T> beanClass) {
        if (this.beanClass != null) { // 使用该判断
            throw new IllegalStateException("beanClass 已被初始化，禁止重复设置");
        }
        this.beanClass = beanClass;
    }

    public void setDtoClass(Class<?> dtoClass) {
        if (this.dtoClass != null) { // 使用该判断
            throw new IllegalStateException("dtoClass 已被初始化，禁止重复设置");
        }
        this.dtoClass = dtoClass;
    }

    protected void setCoordinator(ExecuteCoordinatorService coordinator) {
        this.coordinator = coordinator;
    }
}