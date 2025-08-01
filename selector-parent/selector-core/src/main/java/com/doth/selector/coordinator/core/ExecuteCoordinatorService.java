package com.doth.selector.coordinator.core;

import com.doth.selector.executor.supports.builder.ConditionBuilder;

import java.util.LinkedHashMap;
import java.util.List;


public interface ExecuteCoordinatorService {


    /**
     * sql 执行中介, 带 map 条件集
     *
     * @param beanClass 映射目标实体类
     * @param cond      条件集
     * @return 目标实体类结构的集合
     * @param <T> 目标实体类类型
     */
    <T> List<T> queryByMap(Class<T> beanClass, LinkedHashMap<String, Object> cond);

    /**
     * sql 执行中介, 带自定义 builder 条件集
     *
     * @param beanClass 映射目标实体类
     * @param builder   条件构造器
     * @return 目标实体类结构的集合
     * @param <T> 目标实体类类型
     */
    <T> List<T> queryByBuilder(Class<T> beanClass, ConditionBuilder<T> builder);

    /**
     * sql 执行中介, 带 map 以及字符串子从句为条件集
     *
     * @param beanClass 映射目标实体类
     * @param cond 条件键值map
     * @param strClause 自定义从句
     * @return 目标实体类结构的集合
     * @param <T> 目标实体类类型
     */
    <T> List<T> queryByMapVzClause(Class<T> beanClass, LinkedHashMap<String, Object> cond, String strClause);

    /**
     * sql 执行中介, 带自定义sql为核心, builder进行条件辅助
     *
     * @param beanClass 映射目标实体类
     * @param sql 自定义sql, (select...from xxx/ select...join...on)
     * @param builder 条件构造器
     * @return 目标实体类结构的集合
     * @param <T> 目标实体类类型
     */
    <T> List<T> queryByBuilderVzRaw(Class<T> beanClass, String sql, ConditionBuilder<T> builder) ;


    /**
     * sql 执行中介, 全自定义sql
     *
     * @param <T>       目标实体类类型
     * @param beanClass 映射目标实体类
     * @param sql       全自定义sql
     * @param params    参数(?)
     * @return 目标实体类结构的集合
     */
    <T> List<T> queryByRaw(Class<T> beanClass, String sql, Object... params);

}
