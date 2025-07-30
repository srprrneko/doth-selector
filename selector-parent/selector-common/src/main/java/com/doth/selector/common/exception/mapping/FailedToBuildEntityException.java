package com.doth.selector.common.exception.mapping;

import com.doth.selector.common.exception.SelectorException;

public class FailedToBuildEntityException extends SelectorException {

    public FailedToBuildEntityException(String message) {
        super(message);
    }

    public FailedToBuildEntityException(String message, Throwable cause) {
        super(message, cause);
    }
}