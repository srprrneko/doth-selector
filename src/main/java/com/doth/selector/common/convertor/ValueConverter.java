package com.doth.selector.common.convertor;

/**
 * 字段类型转换器接口，策略模式核心接口
 */
public interface ValueConverter {
    /**
     * 是否支持该字段类型和输入值
     */
    boolean supports(Class<?> fieldType, Object value);

    /**
     * 将输入值转换为目标字段类型的值
     */
    Object convert(Object value);
}
