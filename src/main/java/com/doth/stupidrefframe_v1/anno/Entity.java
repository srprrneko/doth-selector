package com.doth.stupidrefframe_v1.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE) // 注解作用于类
@Retention(RetentionPolicy.CLASS)
public @interface Entity {
}