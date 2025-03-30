package com.doth.stupidrefframe_v1.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @project: classFollowing
 * @package: reflect.execrise7sqlgenerate
 * @author: doth
 * @creTime: 2025-03-18  11:33
 * @desc: TODO
 * @v: 1.0
 */
@Retention(RetentionPolicy.RUNTIME) // 表示注解在运行时存在
@Target(ElementType.TYPE) // 限制使用范围在类中, 方法字段不能使用
public @interface TableName {
    String value(); // 存储表名
}
