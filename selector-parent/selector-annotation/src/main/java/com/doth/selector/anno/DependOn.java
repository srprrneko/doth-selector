package com.doth.selector.anno;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DependOn {
    String clzPath();
}
