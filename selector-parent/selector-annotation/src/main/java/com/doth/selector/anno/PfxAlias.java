package com.doth.selector.anno;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
@Documented
@Deprecated(since = "", forRemoval = true)
public @interface PfxAlias {
    String name();
}
