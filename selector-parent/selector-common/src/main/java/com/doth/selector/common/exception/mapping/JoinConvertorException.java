package com.doth.selector.common.exception.mapping;

import com.doth.selector.common.exception.SelectorException;

public class JoinConvertorException extends SelectorException {
    public JoinConvertorException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public JoinConvertorException(String msg) {
        super(msg);
    }
}