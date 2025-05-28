package com.doth.selector.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Join {
    String fk(); // 外键列名（如d_id）
    String refFK() default "id"; // 目标表的主键列名（默认id）
    String alias() default "";
}