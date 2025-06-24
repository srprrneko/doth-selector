package com.doth.selector.anno;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface JoinLevel {
    Class<?> clz();
    String attrName() default "";


    JoinStrategy JOIN_STRATEGY() default JoinStrategy.JOIN;

    // 所有的连接策略通过 dto 模式完成,
    enum JoinStrategy {
        JOIN,
        LEFT,
        RIGHT,
        // todo
    }
}
