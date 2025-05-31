package com.doth.selector.anno;

import java.lang.annotation.*;

@Target(ElementType.CONSTRUCTOR)
@Retention(RetentionPolicy.RUNTIME)
public @interface DTOConstructor {
    String id();
    boolean isAutoClzName() default true;
}