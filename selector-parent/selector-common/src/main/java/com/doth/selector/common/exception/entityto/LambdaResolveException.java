package com.doth.selector.common.exception.entityto;

import com.doth.selector.common.exception.SelectorException;


public class LambdaResolveException extends SelectorException {

    public LambdaResolveException(String message) {
        super(message);
    }

    public LambdaResolveException(String message, Throwable cause) {
        super(message, cause);
    }

}
