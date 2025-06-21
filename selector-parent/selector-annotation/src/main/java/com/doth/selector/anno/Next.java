package com.doth.selector.anno;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Next {
    Class<?> clz();
}
