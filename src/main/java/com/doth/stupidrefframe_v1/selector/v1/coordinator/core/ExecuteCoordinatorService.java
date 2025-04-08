package com.doth.stupidrefframe_v1.selector.v1.coordinator.core;

import com.doth.stupidrefframe_v1.selector.v1.executor.supports.builder.ConditionBuilder;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author 贤
 * @version 1.0
 * @date 2025/4/5 20:57
 * @description 执行协调功能总管理
 */
public interface ExecuteCoordinatorService {


    /**
     * sqlgenerator 执行中介, 带 map 条件集
     *
     * @param beanClass 映射目标实体类
     * @param cond      条件集
     * @return 目标实体类结构的集合
     * @param <T> 目标实体类类型
     */
    <T> List<T> queryByMap(Class<T> beanClass, LinkedHashMap<String, Object> cond);

    /**
     * sqlgenerator 执行中介, 带自定义 builder 条件集
     *
     * @param beanClass 映射目标实体类
     * @param builder   条件构造器
     * @return 目标实体类结构的集合
     * @param <T> 目标实体类类型
     */
    <T> List<T> queryByBuilder(Class<T> beanClass, ConditionBuilder builder);

    /**
     * sqlgenerator 执行中介, 带 map 以及字符串子从句为条件集
     *
     * @param beanClass 映射目标实体类
     * @param cond 条件键值map
     * @param strClause 自定义从句
     * @return 目标实体类结构的集合
     * @param <T> 目标实体类类型
     */
    <T> List<T> queryByMapVzClause(Class<T> beanClass, LinkedHashMap<String, Object> cond, String strClause);

    /**
     * sqlgenerator 执行中介, 带自定义sql为核心, builder进行条件辅助
     *
     * @param beanClass 映射目标实体类
     * @param sql 自定义sql, (select...from xxx/ select...join...on)
     * @param builder 条件构造器
     * @return 目标实体类结构的集合
     * @param <T> 目标实体类类型
     */
    <T> List<T> queryByBuilderVzRaw(Class<T> beanClass, String sql, ConditionBuilder builder) ;


    /**
     * sqlgenerator 执行中介, 全自定义sql
     *
     * @param beanClass 映射目标实体类
     * @param sql 全自定义sql
     * @param params 参数(?)
     * @return 目标实体类结构的集合
     * @param <T> 目标实体类类型
     */
    <T> List<T> queryByRaw(Class<T> beanClass, String sql, Object... params);

}
