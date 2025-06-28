package com.doth.selector.anno;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Documented
public @interface Overload {
    String desc() default "this is an overload method bro.";
}
