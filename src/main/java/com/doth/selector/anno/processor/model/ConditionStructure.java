package com.doth.selector.anno.processor.model;

// 用于记录方法名中字段与操作符的结构
public class ConditionStructure {
    public final String fieldName;
    public final String operator;

    public ConditionStructure(String fieldName, String operator) {
        this.fieldName = fieldName;
        this.operator = operator;
    }
}
