package com.doth.selector.executor.query.enhanced.impl;

import com.doth.selector.anno.Overload;
import com.doth.selector.executor.query.QueryExecutorTag;
import com.doth.selector.executor.query.enhanced.JoinExecutor;
import com.doth.selector.executor.supports.QueryList;
import com.doth.selector.executor.supports.builder.ConditionBuilder;

import java.util.List;
import java.util.function.Consumer;

/**
 * Query 执行器 - 条件构造器模式（Pro 加强版）
 * <p>
 * <strong>适用场景</strong>
 * <ul>
 *   <li>推荐使用 Lambda 表达式构造查询条件的所有情况</li>
 *   <li>适用于字段路径复杂或含嵌套对象时的类型安全查询</li>
 *   <li>支持 DTO 查询结果转换（内部由 Selector<T> 驱动）</li>
 * </ul>
 * <br>
 * <strong>支持</strong>
 * <ul>
 *   <li>编译期类型提示，规避字段名拼写错误</li>
 *   <li>支持 IDE 自动补全字段路径</li>
 *   <li>结合 {@code Lambda2FieldNameResolver}，生成精确字段路径</li>
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
public class BuilderQueryExecutorPro<T> extends JoinExecutor<T> implements QueryExecutorTag<T> {

    // protected BuilderQueryExecutorPro(Class<T> beanClass) {
    //     super(beanClass);
    // }

    /**
     * Lambda 条件构造查询 - 推荐主力使用方式
     * <p>使用 {@link ConditionBuilder} 构造 SQL 条件块</p>
     * <p>支持嵌套字段、安全联表引用等能力</p>
     *
     * @param setup Lambda 表达式构建条件
     * @return 查询结果列表
     */
    @Overload
    public QueryList<T> query(Consumer<ConditionBuilder<T>> setup) {
        ConditionBuilder<T> builder = new ConditionBuilder<>(beanClass);
        setup.accept(builder);
        // return new QueryList<>(coordinator.queryByBuilder(beanClass, builder));
        return new QueryList<>(coordinator.queryByBuilder(beanClass, builder));

    }

    /**
     * DTO 模式查询 -
     * <p>支持</p>
     * <ul>
     *     <li>返回结构映射至 DTO</li>
     *     <li> DTO</li>
     * </ul>
     * <p><strong>服务于 Selector<T> 门面类的 queryDtoList 方法</strong></p>
     * <p>内部将返回值类型切换为 {@code dtoClass}</p>
     * <p><strong>不建议直接调用此重载方法，推荐通过 Selector<T> 接口使用</strong></p>
     *
     * @param setup Lambda 表达式构建条件
     * @param dtoModel 仅用作重载区分
     * @return DTO 映射结果列表
     */
    @Overload
    @SuppressWarnings(value = "unchecked")
    public QueryList<T> query(Consumer<ConditionBuilder<T>> setup, boolean dtoModel) {
        ConditionBuilder<T> builder = new ConditionBuilder<>(beanClass); // beanClass = 实体类
        setup.accept(builder);

        return new QueryList<>(coordinator.queryByBuilder((Class<T>) dtoClass, builder)); // 查询映射使用 dto
    }


    @Overload
    @Deprecated
    public QueryList<T> query(String sql, Consumer<ConditionBuilder<T>> conditionSetup) {
        ConditionBuilder<T> builder = new ConditionBuilder<T>();
        conditionSetup.accept(builder);
        return new QueryList<>(coordinator.queryByBuilderVzRaw(beanClass, sql, (ConditionBuilder<T>) builder));
    }


    //
    // @Overload
    // public T query2T(Consumer<ConditionBuilder<T>> setup) {
    //     return ResultSetMapper.getSingleResult(query2Lst(setup));
    // }
    //
    // @Overload
    // public T query2T(String sql, Consumer<ConditionBuilder<T>> conditionSetup) {
    //     return ResultSetMapper.getSingleResult(query2Lst(sql, conditionSetup));
    // }

}