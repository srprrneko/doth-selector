package com.doth.stupidrefframe.selector.v1.coordinator.core.process;

import com.doth.stupidrefframe.selector.v1.coordinator.core.ExecuteCoordinator;
import com.doth.stupidrefframe.selector.v1.coordinator.supports.mapper.ResultSetMapper;
import com.doth.stupidrefframe.selector.v1.util.adapeter.EntityAdapter;
import com.doth.stupidrefframe.selector.v1.executor.supports.builder.ConditionBuilder;
import com.doth.stupidrefframe.selector.v1.coordinator.supports.convertor.ConvertorType;
import com.doth.stupidrefframe.selector.v1.coordinator.supports.sqlgenerator.facede.SelectGenerateFacade;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author 贤
 * @version 1.0
 * @date 2025/4/5 21:11
 * @description 联查的执行协调者
 */
public class JoinExecuteCoordinator extends ExecuteCoordinator {

    
    // 更换策略, 表示该协调器统一采用多表映射策略
    public JoinExecuteCoordinator() {
        this.rsMapper = new ResultSetMapper(ConvertorType.JOIN_MAP);
    }



    @Override
    public <T> List<T> queryByMap(Class<T> beanClass, LinkedHashMap<String, Object> cond) {
        String sql = SelectGenerateFacade.generateJoin4map(beanClass, cond);

        Object[] params = EntityAdapter.buildParams(cond);
        return executeQuery(beanClass, sql, params);
    }

    @Override
    public <T> List<T> queryByBuilder(Class<T> beanClass, ConditionBuilder builder) {
        String sql = SelectGenerateFacade.generateJoin4builder(beanClass, builder);
        Object[] params = builder.getParams();
        return executeQuery(beanClass, sql, params);
    }

    @Override
    public <T> List<T> queryByMapVzClause(Class<T> beanClass, LinkedHashMap<String, Object> cond, String strClause) {
        String sql = SelectGenerateFacade.generateJoin4mapVzClause(beanClass, cond, strClause);
        Object[] params =  EntityAdapter.buildParams(cond);
        return executeQuery(beanClass, sql, params);
    }

    @Override
    public <T> List<T> queryByBuilderVzRaw(Class<T> beanClass, String sql, ConditionBuilder builder) {
        sql = SelectGenerateFacade.cvn4joinBuilderVzRaw(sql, builder);
        // sqlgenerator = sqlgenerator + builder.getFullSql();
        return executeQuery(beanClass, sql, builder.getParams());
    }

    @Override
    public <T> List<T> queryByRaw(Class<T> beanClass, String sql, Object... params) {
        String finalSql = SelectGenerateFacade.cvn4joinRaw(sql); // 自动生成别名 cvn: convert
        return executeQuery(beanClass, finalSql, params);
    }
}