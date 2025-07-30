package com.doth.selector.common.exception;

public class SelectorException extends RuntimeException {
    public SelectorException() {
        super();
    }

    public SelectorException(String message) {
        super(message);
    }

    public SelectorException(String message, Throwable cause) {
        super(message, cause);
    }

    public SelectorException(Throwable cause) {
        super(cause);
    }
}
