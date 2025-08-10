package com.doth.selector.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>标注在dao上的注解</p>
 * <p>增加spring支持</p>
 * <p>主要用于生成dao对应的实现类子类</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface AutoImpl {
    boolean springSupport() default false;
}