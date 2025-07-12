package com.doth.selector.coordinator.core.process;

import com.doth.selector.coordinator.core.ExecuteCoordinator;
import com.doth.selector.coordinator.ResultSetMapper;
import com.doth.selector.supports.adapter.EntityAdapter;
import com.doth.selector.executor.supports.builder.ConditionBuilder;
import com.doth.selector.convertor.ConvertorType;
import com.doth.selector.coordinator.supports.SelectGenerateFacade;

import java.util.*;

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

        Object[] params = EntityAdapter.buildParams4CondMap(cond);
        return executeQuery(beanClass, sql, params);
    }

    @Override
    public <T> List<T> queryByBuilder(Class<T> beanClass, ConditionBuilder<T> builder) {
        builder.setEntityClz(beanClass); // 适配lambda

        String sql = SelectGenerateFacade.generateJoin4builder(beanClass, builder);
        Object[] params = builder.getParams();
        // System.out.println("beanClass = " + beanClass);
        return executeQuery(beanClass, sql, params);
    }

    @Override // todo: 有可能后续移除
    public <T> List<T> queryByMapVzClause(Class<T> beanClass, LinkedHashMap<String, Object> cond, String strClause) {
        String sql = SelectGenerateFacade.generateJoin4mapVzClause(beanClass, cond, strClause);
        Object[] params =  EntityAdapter.buildParams4CondMap(cond);
        return executeQuery(beanClass, sql, params);
    }

    @Override
    @Deprecated // todo: 有可能后续移除
    public <T> List<T> queryByBuilderVzRaw(Class<T> beanClass, String sql, ConditionBuilder<T> builder) {
        sql = SelectGenerateFacade.cvn4joinBuilderVzRaw(sql, builder);
        // sql = sql + builder.getFullSql();
        return executeQuery(beanClass, sql, builder.getParams());
    }

    @Override
    public <T> List<T> queryByRaw(Class<T> beanClass, String sql, Object... params) {
        String finalSql = SelectGenerateFacade.cvn4joinRaw(sql); //  自动生成别名 cvn: convert
        // 自动展开所有List/Collection参数
        List<Object> finalParams = new ArrayList<>();
        for (Object param : params) {
            if (param instanceof Collection) {
                finalParams.addAll((Collection<?>) param);
            } else {
                finalParams.add(param);
            }
        }
        return executeQuery(beanClass, finalSql, finalParams.toArray());
    }
}