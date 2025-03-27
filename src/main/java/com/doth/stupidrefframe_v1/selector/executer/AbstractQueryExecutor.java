package com.doth.stupidrefframe_v1.selector.executer;

import com.doth.stupidrefframe_v1.selector.supports.SelectorHelper;

/**
 * 抽象查询执行器基类 - 强制子类持有实体类型信息
 * @param <T> 目标实体类型
 */
public abstract class AbstractQueryExecutor<T> {
    protected final Class<T> beanClass;
    protected final SelectorHelper helper = new SelectorHelper();

    // 强制子类必须传入实体类型
    protected AbstractQueryExecutor(Class<T> beanClass) {
        this.beanClass = beanClass;
    }
}