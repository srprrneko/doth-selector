package com.doth.stupidrefframe.exception;

public class NonPrimaryKeyException extends RuntimeException {
    public NonPrimaryKeyException(String message) {
        super(message);
    }
}
