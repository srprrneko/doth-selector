package com.doth.stupidrefframe_v1.selector.core.coordinator;


import com.doth.stupidrefframe_v1.selector.util.DruidUtil;
import com.doth.stupidrefframe_v1.selector.supports.sql.SqlGenerator;
import com.doth.stupidrefframe_v1.selector.supports.builder.ConditionBuilder;
import com.doth.stupidrefframe_v1.selector.supports.convertor.ConvertorType;
import com.doth.stupidrefframe_v1.selector.util.ConditionParamBuilder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class QueryCoordinator_v1 {
    // 组合组件
    private final ResultSetMapper rsMapper;
    private final ConditionParamBuilder condProcessor;


    public QueryCoordinator_v1() {
        this.rsMapper = new ResultSetMapper(ConvertorType.JOIN_MAP);
        this.condProcessor = new ConditionParamBuilder();
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
    public <T> List<T> mapSqlCond(Class<T> beanClass, LinkedHashMap<String, Object> cond) {
        String sql = SqlGenerator.generateSelect(beanClass, cond);
        Object[] params = condProcessor.buildParams(cond);
        return executeQuery(beanClass, sql, params);
    }
    // ------------------ sql 执行中介, 带 builder 条件集 ------------------
    public <T> List<T> mapSqlCond(Class<T> beanClass, ConditionBuilder builder) {
        String sql = SqlGenerator.generateSelect(beanClass, builder);
        Object[] params = builder.getParams();
        return executeQuery(beanClass, sql, params);
    }
    // ------------------ sql 执行中介, 带 map 以及字符串子从句为条件集 ------------------
    public <T> List<T> mapSqlCond(Class<T> beanClass, LinkedHashMap<String, Object> cond, String strClause) {
        String sql = SqlGenerator.generateSelect(beanClass, cond, strClause);
        Object[] params =  condProcessor.buildParams(cond);
        return executeQuery(beanClass, sql, params);
    }
    // ------------------ sql 执行中介, 带自定义sql作为连接, builder进行条件辅助 ------------------
    public <T> List<T> mapSqlCond(Class<T> beanClass, String sql, ConditionBuilder builder) {
        sql = sql + builder.getFullSql();
        System.out.println("sql = " + sql);
        return executeQuery(beanClass, sql, builder.getParams());
    }
    // ------------------ sql 执行中介, 带自定义sql ------------------
    public <T> List<T> mapSqlCond(Class<T> beanClass, String sql, Object... params) {
        String normalSql = SqlGenerator.generateSelect4Raw(beanClass, sql); // 仅仅只是转换sql规范
        System.out.println("normalSql = " + normalSql);
        return executeQuery(beanClass, normalSql, params);
    }



    // 暴露必要工具方法
    public LinkedHashMap<String, Object> extractFields(Object entity) {
        return condProcessor.extractNonNullFields(entity);
    }

    public <T> T getSingleResult(List<T> list) {
        return rsMapper.getSingleResult(list);
    }

}