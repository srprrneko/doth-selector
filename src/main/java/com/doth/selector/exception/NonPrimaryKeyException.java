package com.doth.selector.exception;

public class NonPrimaryKeyException extends RuntimeException {
    public NonPrimaryKeyException(String message) {
        super(message);
    }
}
