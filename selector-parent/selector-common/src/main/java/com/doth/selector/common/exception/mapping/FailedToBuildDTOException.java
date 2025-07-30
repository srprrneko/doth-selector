package com.doth.selector.common.exception.mapping;

import com.doth.selector.common.exception.SelectorException;

public class FailedToBuildDTOException extends SelectorException {
    public FailedToBuildDTOException(String message) {
        super(message);
    }

    public FailedToBuildDTOException(String message, Throwable cause) {
        super(message, cause);
    }
}