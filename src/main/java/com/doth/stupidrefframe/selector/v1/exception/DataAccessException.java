package com.doth.stupidrefframe.selector.v1.exception;

// 异常类
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}