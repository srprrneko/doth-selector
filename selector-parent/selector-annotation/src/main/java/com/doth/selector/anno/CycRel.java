package com.doth.selector.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cycle Relation : 标注从属对象与当前对象存在循环关系, 用于防止无限生成 sql中的 join子句 的情况
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CycRel {
}