package com.doth.selector.coordinator.supports.sqlgenerator.tool.temp;

import com.doth.selector.executor.supports.builder.ConditionBuilder;

/**
 * 策略接口：初始化上下文（解析 @DependOn、DTO 注册、条件前缀等）
 */
public interface QueryInitializationStrategy {
    void initialize(QueryContext context, Class<?> entityClass, ConditionBuilder<?> conditionBuilder);
}
