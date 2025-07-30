package com.doth.selector.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Deprecated // 当前映射影响要求实体名必须以 [表名+驼峰] 暂时不支持别名 todo:等待改进
public @interface TableName {
    String value(); // 存储表名
}
