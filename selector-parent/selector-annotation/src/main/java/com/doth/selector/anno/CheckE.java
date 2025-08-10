package com.doth.selector.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * check entity, 通过注解处理器检查实体类的 字段类型 是否全部是包装类, 如果已经遵守, 则可去掉
 */
@Target(ElementType.TYPE) // 注解作用于类
@Retention(RetentionPolicy.CLASS)
public @interface CheckE {
}