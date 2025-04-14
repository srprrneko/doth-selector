package com.doth.selector.core;

/**
 * 枚举执行策略
 */
public enum ExecutorType {
    BUILDER, // builder 执行器切换策略
    RAW, // 原生执行切换策略
    DIRECT, // 固定执行策略
    DIRECT_PRO, // 固定查询
    RAW_PRO,
    BUILDER_PRO //
}
