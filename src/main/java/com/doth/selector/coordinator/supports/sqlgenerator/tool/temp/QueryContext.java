package com.doth.selector.coordinator.supports.sqlgenerator.tool.temp;

import com.doth.selector.anno.DependOn;
import com.doth.selector.common.exception.NonPrimaryKeyException;
import com.doth.selector.common.testbean.join2.Employee;
import com.doth.selector.common.testbean.join3.User;
import com.doth.selector.common.util.AnnoNamingConvertUtil;
import com.doth.selector.common.util.NamingConvertUtil;
import com.doth.selector.dto.DTOSelectFieldsListFactory;
import com.doth.selector.executor.supports.builder.ConditionBuilder;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.util.*;

/**
 * 上下文，集中管理所有生成 SQL 过程中的成员变量
 */
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


    public List<String> getSelectList() {
        return selectList;
    }

    public List<String> getJoinClauses() {
        return joinClauses;
    }

    public Set<Class<?>> getProcessedEntities() {
        return processedEntities;
    }

    public Class<?> getOriginalEntity() {
        return originalEntity;
    }

    public void setOriginalEntity(Class<?> originalEntity) {
        this.originalEntity = originalEntity;
    }

    public boolean isDtoMode() {
        return dtoMode;
    }

    public void setDtoMode(boolean dtoMode) {
        this.dtoMode = dtoMode;
    }

    public List<String> getDtoSelectPaths() {
        return dtoSelectPaths;
    }

    public void setDtoSelectPaths(List<String> dtoSelectPaths) {
        this.dtoSelectPaths = dtoSelectPaths;
    }

    public Set<String> getDtoPrefixes() {
        return dtoPrefixes;
    }

    public void setDtoPrefixes(Set<String> dtoPrefixes) {
        this.dtoPrefixes = dtoPrefixes;
    }

    public Set<String> getConditionPrefixes() {
        return conditionPrefixes;
    }

    public void setConditionPrefixes(Set<String> conditionPrefixes) {
        this.conditionPrefixes = conditionPrefixes;
    }

    public int getJoinLevel() {
        return joinLevel;
    }

    public void setJoinLevel(int joinLevel) {
        this.joinLevel = joinLevel;
    }

    public String toString() {
        return "QueryContext{originalEntity = " + originalEntity + ", dtoMode = " + dtoMode + ", dtoSelectPaths = " + dtoSelectPaths + ", dtoPrefixes = " + dtoPrefixes + ", conditionPrefixes = " + conditionPrefixes + ", selectList = " + selectList + ", joinClauses = " + joinClauses + ", processedEntities = " + processedEntities + ", joinLevel = " + joinLevel + "}";
    }
}