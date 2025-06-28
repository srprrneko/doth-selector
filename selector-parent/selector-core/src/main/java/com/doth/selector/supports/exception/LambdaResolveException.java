package com.doth.selector.supports.exception;

/**
 * Lambda 字段路径解析顶级异常
 */
public class LambdaResolveException extends RuntimeException {

    public LambdaResolveException(String message) {
        super(message);
    }

    public LambdaResolveException(String message, Throwable cause) {
        super(message, cause);
    }

}
