package com.doth.selector.anno.processor.core.dtogenerate;

import lombok.Value;

import java.util.List;

@Value
class JoinChainResult {
    protected List<String> selectFields;
    protected List<JoinInfo> joinInfos;
}