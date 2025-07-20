package com.doth.selector.common.exception.mapping;

import com.doth.selector.common.exception.SelectorException;

public class NonPrimaryKeyException extends SelectorException {
    public NonPrimaryKeyException(String message) {
        super(message);
    }
    public NonPrimaryKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
