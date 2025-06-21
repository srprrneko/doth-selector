package com.doth.selector.coordinator.supports.sqlgenerator.tool.temp;

import lombok.Data;

import java.util.*;

/**
 * 上下文，集中管理所有生成 SQL 过程中的成员变量
 */
@Data
public class QueryContext {

    private Class<?> originalEntity;

    private boolean dtoMode;

    private List<String> dtoSelectPaths;

    private Set<String> dtoPrefixes;

    private Set<String> conditionPrefixes;

    private final List<String> selectList = new ArrayList<>();

    private final List<String> joinClauses = new ArrayList<>();

    private final Set<Class<?>> processedEntities = new HashSet<>();

    private int joinLevel = 1;

    public String toString() {
        return "QueryContext{originalEntity = " + originalEntity + ", dtoMode = " + dtoMode + ", dtoSelectPaths = " + dtoSelectPaths + ", dtoPrefixes = " + dtoPrefixes + ", conditionPrefixes = " + conditionPrefixes + ", selectList = " + selectList + ", joinClauses = " + joinClauses + ", processedEntities = " + processedEntities + ", joinLevel = " + joinLevel + "}";
    }
}