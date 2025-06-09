package com.doth.selector.coordinator.supports.sqlgenerator.tool.temp;

import com.doth.selector.anno.DependOn;
import com.doth.selector.dto.DTOSelectFieldsListFactory;
import com.doth.selector.executor.supports.builder.ConditionBuilder;

import java.beans.Introspector;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * DTO 模式下的初始化策略
 */
public class DtoInitializationStrategy implements QueryInitializationStrategy {
    @Override
    public void initialize(QueryContext ctx, Class<?> entityClass, ConditionBuilder<?> conditionBuilder) {
        DependOn dep = entityClass.getAnnotation(DependOn.class);
        try {
            Class.forName(entityClass.getName(), true, entityClass.getClassLoader());
            Class<?> origin = Class.forName(dep.clzPath());
            ctx.setOriginalEntity(origin);
            ctx.setDtoMode(true);
            String decap = Introspector.decapitalize(entityClass.getSimpleName());
            List<String> paths = DTOSelectFieldsListFactory.resolveSelectList(origin, decap);
            if (paths.isEmpty()) {
                paths = DTOSelectFieldsListFactory.resolveSelectList(origin, entityClass.getSimpleName());
                if (!paths.isEmpty()) {
                    System.err.println("仅找到大写首字母形式的注册列: " + origin.getName());
                }
            }
            if (paths.isEmpty()) {
                throw new RuntimeException("未找到 DTO 查询列: " + origin);
            }
            ctx.setDtoSelectPaths(paths);
            Set<String> prefixes = new LinkedHashSet<>();
            paths.forEach(p -> prefixes.add(p.substring(0, p.indexOf('.'))));
            ctx.setDtoPrefixes(prefixes);
            ctx.getSelectList().addAll(paths);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("加载类失败: " + e.getMessage(), e);
        }
        ctx.setConditionPrefixes(conditionBuilder != null
                ? conditionBuilder.extractJoinTablePrefixes()
                : Collections.emptySet());
    }
}
