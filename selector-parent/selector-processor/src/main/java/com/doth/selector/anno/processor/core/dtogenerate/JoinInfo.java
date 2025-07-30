package com.doth.selector.anno.processor.core.dtogenerate;

import lombok.Value;

import java.util.List;

@Value
class JoinInfo {

    /**
     * 对应 @JoinLevel 和 @Next 的 属性 attrName
     */
    String attrName;

    /**
     * 系统自动命名 的当前 join 层级 的 字段前缀, 例: tN
     */
    String alias;

}