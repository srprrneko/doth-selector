package com.doth.selector.common.exception.mapping;

import com.doth.selector.common.exception.SelectorException;

public class NonUniqueResultException extends SelectorException {
    public NonUniqueResultException(String message) {
        super(message);
    }
}