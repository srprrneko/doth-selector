package com.doth.selector.executor.query.enhanced.impl;

import com.doth.selector.anno.Overload;
import com.doth.selector.executor.query.QueryExecutorTag;
import com.doth.selector.executor.query.enhanced.JoinExecutor;
import com.doth.selector.executor.supports.QueryList;
import com.doth.selector.supports.adapter.EntityAdapter;

import java.util.LinkedHashMap;


/**
 * Query 执行器 - 直接字段映射模式（Pro 加强版）
 * <p>
 * <strong>适用场景</strong>
 * <ul>
 *   <li>适用于不依赖注解、直接构造字段查询场景</li>
 *   <li>支持实体作为查询条件的“快捷查询”方式</li>
 *   <li>支持 Map 形式传入字段路径（必须指明完整路径 tN.field）</li>
 *   <li>支持空参 -> 查询所有记录</li>
 * </ul>
 * <br>
 * <strong>建议优先使用</strong>
 * <ul>
 *   <li>lambda 表达式形式 {@code builder.eq(Entity::getField)}</li>
 * </ul>
 * <br>
 * <strong>后续规划</strong>
 * <ul>
 *   <li><code>TODO</code>: 计划支持 SFunction 类型的 Map 键增强</li>
 * </ul>
 * <br>
 * <strong>继承关系说明</strong>
 * <ul>
 *   <li>继承自 {@link JoinExecutor}，具备联表支持能力</li>
 *   <li>实现 {@link QueryExecutorTag} 标识接口，支持自动收集</li>
 * </ul>
 *
 * @param <T> 实体类型
 */
public class DirectQueryExecutorPro<T> extends JoinExecutor<T> implements QueryExecutorTag<T> {

    /**
     * 空参查询 - 查询所有记录
     * <p>等价于 SELECT * ..</p>
     *
     * @return 查询结果列表
     */
    @Overload
    public QueryList<T> query() {
        return new QueryList<>(coordinator.queryByMap(super.beanClass, (LinkedHashMap<String, Object>) null));
    }

    /**
     * 实体条件查询 - 提取对象字段作为查询条件
     * <p>字段为 null 的将被忽略</p>
     * <p>适用于用户传入完整实体作为条件容器的场景</p>
     *
     * @param t 查询条件对象（如 new User().setName("Tom")）
     * @return 查询结果列表
     */
    @Overload
    public QueryList<T> query(T t) {
        LinkedHashMap<String, Object> condMap = EntityAdapter.extractNestedFields2Map(t);
        return new QueryList<>(coordinator.queryByMap(super.beanClass, condMap));
    }

    /**
     * Map 条件查询 - 手动构造字段路径作为查询条件
     * <p><strong>必须指定完整路径：如 t0.name</strong></p>
     * <p>建议优先使用 Lambda 执行器以获得字段路径自动推导能力</p>
     *
     * @param cond 条件 Map，如 {"t0.name" -> "Tom"}
     * @return 查询结果列表
     */
    @Overload
    public QueryList<T> query(LinkedHashMap<String, Object> cond) { // 以map为条件的重载
        return new QueryList<>(coordinator.queryByMap(super.beanClass, cond));
    }

    @Deprecated
    @Overload
    public QueryList<T> query(LinkedHashMap<String, Object> cond, String condClause) {
        return new QueryList<>(coordinator.queryByMapVzClause(super.beanClass, cond, condClause));
    }
    
    // @Overload
    // public T query2T(LinkedHashMap<String, Object> cond) {
    //     return ResultSetMapper.getSingleResult(query2Lst(cond));
    // }
    //
    // @Overload
    // public T query2T(LinkedHashMap<String, Object> cond, String condClause) {
    //     return ResultSetMapper.getSingleResult(query2Lst(cond, condClause));
    // }

}