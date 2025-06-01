package com.doth.selector.anno;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface JoinLevel {
    Class<?> clz();
}
