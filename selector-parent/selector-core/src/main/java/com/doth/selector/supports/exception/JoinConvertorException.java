package com.doth.selector.supports.exception;

public class JoinConvertorException extends RuntimeException {
    public JoinConvertorException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public JoinConvertorException(String msg) {
        super(msg);
    }
}