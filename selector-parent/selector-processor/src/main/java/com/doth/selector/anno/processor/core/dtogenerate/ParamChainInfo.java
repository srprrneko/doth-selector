package com.doth.selector.anno.processor.core.dtogenerate;

import lombok.Value;

import javax.lang.model.element.VariableElement;
import java.util.List;

@Value
class ParamChainInfo {
    protected List<String> selectColList;
    protected List<JoinInfo> joinInfos;
    protected List<VariableElement> validVarEls;
}