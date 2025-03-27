package com.doth.stupidrefframe_v1.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)   // 仅作用于字段
@Retention(RetentionPolicy.RUNTIME) // 运行时生效
public @interface JoinColumn {
    String fk() default "";          // 外键列名（如d_id）
    String referencedColumn() default "id"; // 目标表的主键列名（默认id）
    boolean nullable() default true; // 是否允许空值
}