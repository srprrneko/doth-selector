package com.doth.selector.anno;
import java.lang.annotation.*;

// 历史遗留, 曾配合DTOFactory一起使用
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface UseDTO {
    String id() default "";
}
