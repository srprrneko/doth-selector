package com.doth.loose.rubbish_since331;


import com.doth.stupidrefframe.selector.v1.coordinator.supports.mapper.ResultSetMapper;
import com.doth.stupidrefframe.selector.v1.executor.supports.builder.ConditionBuilder;
import com.doth.stupidrefframe.selector.v1.coordinator.supports.sqlgenerator.facede.SelectGenerateFacade;
import com.doth.stupidrefframe.selector.v1.util.DruidUtil;
import com.doth.stupidrefframe.selector.v1.coordinator.supports.convertor.ConvertorType;
import com.doth.stupidrefframe.selector.v1.util.adapeter.EntityAdapter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Deprecated
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
        System.out.println("最终查询生成的sql: " + sql);
        try (ResultSet rs = DruidUtil.executeQuery(sql, params)) {
            return rsMapper.map(rs, beanClass);
        } catch (SQLException e) {
            throw new RuntimeException("数据库错误", e);
        }
    }


    // ------------------ sqlgenerator 执行中介, 带 map 条件集 ------------------
    public <T> List<T> queryByMap(Class<T> beanClass, LinkedHashMap<String, Object> cond) {
        String sql = SelectGenerateFacade.generate4map(beanClass, cond);
        Object[] params = EntityAdapter.buildParams(cond);
        return executeQuery(beanClass, sql, params);
    }

    // ------------------ sqlgenerator 执行中介, 带 builder 条件集 ------------------
    public <T> List<T> queryByBuilder(Class<T> beanClass, ConditionBuilder builder) {
        String sql = SelectGenerateFacade.generate4builder(beanClass, builder);
        Object[] params = builder.getParams();
        return executeQuery(beanClass, sql, params);
    }

    // ------------------ sqlgenerator 执行中介, 带 map 以及字符串子从句为条件集 ------------------
    public <T> List<T> queryByMapVzClause(Class<T> beanClass, LinkedHashMap<String, Object> cond, String strClause) {
        String sql = SelectGenerateFacade.generate4mapVzClause(beanClass, cond, strClause);
        Object[] params =  EntityAdapter.buildParams(cond);
        return executeQuery(beanClass, sql, params);
    }

    // ------------------ sqlgenerator 执行中介, 带自定义sql作为基本连接, builder进行条件辅助 ------------------
    public <T> List<T> queryByBuilderVzRaw(Class<T> beanClass, String sql, ConditionBuilder builder) {
        sql = sql + builder.getFullSql();
        return executeQuery(beanClass, sql, builder.getParams());
    }

    // ------------------ sqlgenerator 执行中介, 带自定义sql ------------------
    public <T> List<T> queryByRaw(Class<T> beanClass, String sql, Object... params) {
        String finalSql = SelectGenerateFacade.cvn4raw(beanClass, sql); // 普通处理
        return executeQuery(beanClass, finalSql, params);
    }




    // todo: 后续可能迁移
    ////////////////// 新增方法: 动态生成嵌套实体结构的sql //////////////////
    // ------------------ sqlgenerator 执行中介, 带自定义联查sql, todo ------------------
    public <T> List<T> queryJoinByRaw(Class<T> beanClass, String sql, Object... params) {
        String finalSql = SelectGenerateFacade.cvn4joinRaw(sql); // 自动生成别名 cvn: convert
        return executeQuery(beanClass, finalSql, params);
    }

    // ------------------ 联查 sqlgenerator 执行中介, todo ------------------
    public <T> List<T> queryJoinByMap(Class<T> beanClass, LinkedHashMap<String, Object> cond) {
        String sql = SelectGenerateFacade.generateJoin4map(beanClass, cond);
        Object[] params = EntityAdapter.buildParams(cond);
        return executeQuery(beanClass, sql, params);
    }

    // ------------------ 联查 sqlgenerator 执行中介, todo ------------------
    public <T> List<T> queryJoinByMapVzClause(Class<T> beanClass, LinkedHashMap<String, Object> cond, String condClause) {
        String sql = SelectGenerateFacade.generateJoin4mapVzClause(beanClass, cond, condClause);
        Object[] params =  EntityAdapter.buildParams(cond);
        return executeQuery(beanClass, sql, params);
    }

    // ------------------ 联查 sqlgenerator 执行中介, todo  ------------------
    public <T> List<T> queryJoinByBuilder(Class<T> beanClass, ConditionBuilder builder) {
        // String sqlgenerator = SelectGenerateFacade.generate4builder(beanClass, builder);
        String sql = SelectGenerateFacade.generateJoin4builder(beanClass, builder);
        Object[] params = builder.getParams();
        return executeQuery(beanClass, sql, params);
    }





    @Deprecated(since = "1.0") // 因使该类职责不清晰移除, 后续不再使用
    public <T> T getSingleResult(List<T> list) {
        return rsMapper.getSingleResult(list);
    }



}