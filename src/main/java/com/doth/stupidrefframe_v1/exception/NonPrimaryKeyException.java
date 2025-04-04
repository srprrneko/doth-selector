package com.doth.stupidrefframe_v1.exception;

public class NonPrimaryKeyException extends RuntimeException {
    public NonPrimaryKeyException(String message) {
        super(message);
    }
}
