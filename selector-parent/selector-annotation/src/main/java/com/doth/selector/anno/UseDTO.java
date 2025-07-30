package com.doth.selector.anno;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UseDTO {
    String id() default "";
}
