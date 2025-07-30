package com.doth.selector.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Deprecated // 当前映射影响要求实体名必须以 [表名+驼峰] todo:等待改进
public @interface ColumnName {
    String name() default "";
}
