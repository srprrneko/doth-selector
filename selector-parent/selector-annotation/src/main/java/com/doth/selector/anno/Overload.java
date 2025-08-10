package com.doth.selector.anno;

import java.lang.annotation.*;

// 语义注释, 早期方法重载过多时使用
@Target(ElementType.METHOD)
@Documented
public @interface Overload {
    String desc() default "this is an overload method bro.";
}
