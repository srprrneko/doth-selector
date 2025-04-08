package com.doth.stupidrefframe_v1.selector.v1.coordinator.supports.convertor;

public enum ConvertorType {
        LIGHT,      // 轻量级模式
        JOIN_MAP,        // 多表连接
        STRICT,     // 严格模式
        @Deprecated
        LENIENT     // todo: 宽松模式
}