package com.doth.selector.supports.exception;

/**
 * 方法引用解析失败异常
 */
public class MethodReferenceResolveException extends LambdaResolveException {

    public MethodReferenceResolveException(String message, Throwable cause) {
        super(message, cause);
    }

}
