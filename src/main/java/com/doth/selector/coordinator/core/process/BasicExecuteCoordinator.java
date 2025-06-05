package com.doth.selector.coordinator.core.process;

import com.doth.selector.coordinator.core.ExecuteCoordinator;
import com.doth.selector.coordinator.mapper.ResultSetMapper;
import com.doth.selector.common.util.adapeter.EntityAdapter;
import com.doth.selector.executor.supports.builder.ConditionBuilder;
import com.doth.selector.convertor.ConvertorType;
import com.doth.selector.coordinator.supports.sqlgenerator.facede.SelectGenerateFacade;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author 贤
 * @version 1.0
 * @date 2025/4/5 20:43
 * @description 基础的执行协调器
 */
public class BasicExecuteCoordinator extends ExecuteCoordinator {

    // 采用轻量级策略
    public BasicExecuteCoordinator() {
        this.rsMapper = new ResultSetMapper(ConvertorType.LIGHT);
    }



    @Override
    public <T> List<T> queryByMap(Class<T> beanClass, LinkedHashMap<String, Object> cond) {
        String sql = SelectGenerateFacade.generate4map(beanClass, cond);
        Object[] params = EntityAdapter.buildParams(cond);
        return executeQuery(beanClass, sql, params);
    }

    @Override
    public <T> List<T> queryByBuilder(Class<T> beanClass, ConditionBuilder<T> builder) {
        String sql = SelectGenerateFacade.generate4builder(beanClass, builder);
        Object[] params = builder.getParams();
        return executeQuery(beanClass, sql, params);
    }

    @Override
    public <T> List<T> queryByMapVzClause(Class<T> beanClass, LinkedHashMap<String, Object> cond, String strClause) {
        String sql = SelectGenerateFacade.generate4mapVzClause(beanClass, cond, strClause);
        Object[] params =  EntityAdapter.buildParams(cond);
        return executeQuery(beanClass, sql, params);
    }

    @Override
    public <T> List<T> queryByBuilderVzRaw(Class<T> beanClass, String sql, ConditionBuilder<T> builder) {
        // sqlgenerator = sqlgenerator + builder.getFullSql();

        return executeQuery(beanClass, sql, builder.getParams());
    }

    @Override
    public <T> List<T> queryByRaw(Class<T> beanClass, String sql, Object... params) {
        String finalSql = SelectGenerateFacade.cvn4raw(beanClass, sql); // 普通处理
        return executeQuery(beanClass, finalSql, params);
    }
}