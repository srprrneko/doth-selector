package com.doth.selector.coordinator.supports.sqlgenerator.tool.temp;

import com.doth.selector.executor.supports.builder.ConditionBuilder;

import java.util.Collections;

/**
 * 一般模式下的初始化策略
 */
public class GeneralInitializationStrategy implements QueryInitializationStrategy {

    @Override
    public void initialize(QueryContext ctx, Class<?> entityClass, ConditionBuilder<?> conditionBuilder) {
        ctx.setOriginalEntity(entityClass);
        ctx.setDtoMode(false);
        ctx.setDtoSelectPaths(Collections.emptyList());
        ctx.setDtoPrefixes(Collections.emptySet());
        ctx.setConditionPrefixes(conditionBuilder != null
                ? conditionBuilder.extractJoinTablePrefixes()
                : Collections.emptySet());
        // 通用模式下，初始 selectList 留空，在解析字段时自动添加所有 @Id 或普通字段
    }
}
