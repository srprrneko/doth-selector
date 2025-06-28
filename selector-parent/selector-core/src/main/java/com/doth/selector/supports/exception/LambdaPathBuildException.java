package com.doth.selector.supports.exception;

/**
 * Lambda 路径构建失败异常（链式 getter）
 */
public class LambdaPathBuildException extends LambdaResolveException {

    public LambdaPathBuildException(String message, Throwable cause) {
        super(message, cause);
    }

    public LambdaPathBuildException(String message) {
        super(message);
    }

}
