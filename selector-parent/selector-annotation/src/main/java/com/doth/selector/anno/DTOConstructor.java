package com.doth.selector.anno;

import java.lang.annotation.*;

@Target(ElementType.CONSTRUCTOR)
@Retention(RetentionPolicy.RUNTIME)
public @interface DTOConstructor {
    String id();

    /**
     * <p></p>
     * <p>default true >> 代表所有的 参数名 会自动命名, 当参数名冲突时加上 prefix</p>
     * <p>false >> false则代表无论如何都加上 prefix</p>
     */
    boolean autoPrefix() default true;
}