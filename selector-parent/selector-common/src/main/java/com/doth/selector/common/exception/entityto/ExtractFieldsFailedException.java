package com.doth.selector.common.exception.entityto;


import com.doth.selector.common.exception.SelectorException;

public class ExtractFieldsFailedException extends SelectorException {
    public ExtractFieldsFailedException(String message) {
        super(message);
    }

    public ExtractFieldsFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}