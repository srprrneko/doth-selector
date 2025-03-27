package com.doth.stupidrefframe_v1.selector.supports.convertor;

public enum ConvertorType {
        MULTI_JOIN, // 多表连接模式
        LIGHT,      // 轻量级模式
        MAP,        // 多表连接
        STRICT,     // 严格模式
        LENIENT     // todo: 宽松模式（预留）
    }