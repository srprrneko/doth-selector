package com.doth.stupidrefframe_v1.selector.v1.core.coordinator;


import com.doth.stupidrefframe_v1.selector.v1.supports.sql.SelectGenerateFacade;
import com.doth.stupidrefframe_v1.selector.v1.util.DruidUtil;
import com.doth.stupidrefframe_v1.selector.v1.supports.convertor.ConvertorType;
import com.doth.stupidrefframe_v1.selector.v1.supports.adapeter.EntityAdapter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ExecuteCoordinator {
    // 组合组件
    private ResultSetMapper rsMapper;
    // private final EntityAdapter condProcessor;

    // 更改转换策略
    public void setConvertorType(ConvertorType convertorType) {
        this.rsMapper = new ResultSetMapper(convertorType);
    }

    public ExecuteCoordinator() {
        this.rsMapper = new ResultSetMapper(ConvertorType.JOIN_MAP);
        // this.condProcessor = new EntityAdapter();
    }

    // 执行并映射, 同事使用
    <T> List<T> executeQuery(Class<T> beanClass, String sql, Object[] params) {
        try (ResultSet rs = DruidUtil.executeQuery(sql, params)) {
            return rsMapper.map(rs, beanClass);
        } catch (SQLException e) {
            throw new RuntimeException("数据库错误", e);
        }
    }


    // ------------------ sql 执行中介, 带 map 条件集 ------------------
    public <T> List<T> queryByMap(Class<T> beanClass, LinkedHashMap<String, Object> cond) {
        String sql = SelectGenerateFacade.generate4map(beanClass, cond);
        Object[] params = EntityAdapter.buildParams(cond);
        return executeQuery(beanClass, sql, params);
    }

    // ------------------ sql 执行中介, 带 builder 条件集 ------------------
    public <T> List<T> queryByBuilder(Class<T> beanClass, com.doth.stupidrefframe_v1.selector.v1.supports.builder.ConditionBuilder builder) {
        String sql = SelectGenerateFacade.generate4builder(beanClass, builder);
        Object[] params = builder.getParams();
        return executeQuery(beanClass, sql, params);
    }

    // ------------------ sql 执行中介, 带 map 以及字符串子从句为条件集 ------------------
    public <T> List<T> queryByMapVzClause(Class<T> beanClass, LinkedHashMap<String, Object> cond, String strClause) {
        String sql = SelectGenerateFacade.generate4mapVzClause(beanClass, cond, strClause);
        Object[] params =  EntityAdapter.buildParams(cond);
        return executeQuery(beanClass, sql, params);
    }

    // ------------------ sql 执行中介, 带自定义sql作为连接, builder进行条件辅助 ------------------
    public <T> List<T> queryByBuilderVzRaw(Class<T> beanClass, String sql, com.doth.stupidrefframe_v1.selector.v1.supports.builder.ConditionBuilder builder) {
        sql = sql + builder.getFullSql();
        System.out.println("sql = " + sql);
        return executeQuery(beanClass, sql, builder.getParams());
    }

    // ------------------ sql 执行中介, 带自定义sql ------------------
    public <T> List<T> queryByRaw(Class<T> beanClass, String sql, Object... params) {
        String finalSql = SelectGenerateFacade.cvn4raw(beanClass, sql); // 普通处理
        return executeQuery(beanClass, finalSql, params);
    }

    // ------------------ sql 执行中介, 带自定义联查sql ------------------
    public <T> List<T> queryByJoinRaw(Class<T> beanClass, String sql, boolean autoAlias, Object... params) {
        String finalSql = SelectGenerateFacade.cvn4joinRaw(sql, autoAlias); // 自动生成别名 cvn: convert
        return executeQuery(beanClass, finalSql, params);
    }


    // todo: 待迁移
    ////////////////// 新增方法: 动态生成嵌套实体结构的sql //////////////////
    // ------------------ 联查 sql 执行中介, todo: ------------------
    public <T> List<T> queryJoinByMap(Class<T> beanClass, LinkedHashMap<String, Object> cond) {
        String sql = SelectGenerateFacade.generateJoin4map(beanClass, cond);
        System.out.println("最终查询语句: " + sql);
        Object[] params = EntityAdapter.buildParams(cond);
        return executeQuery(beanClass, sql, params);
    }








    @Deprecated(since = "1.0") // 因使该类职责不清晰移除, 后续不再使用
    public <T> T getSingleResult(List<T> list) {
        return rsMapper.getSingleResult(list);
    }


}