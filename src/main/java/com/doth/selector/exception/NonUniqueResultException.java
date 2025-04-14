package com.doth.selector.exception;

/**
 * 非唯一结果异常（继承自RuntimeException）
 */
public class NonUniqueResultException extends RuntimeException {
    public NonUniqueResultException(String message) {
        super(message);
    }
}