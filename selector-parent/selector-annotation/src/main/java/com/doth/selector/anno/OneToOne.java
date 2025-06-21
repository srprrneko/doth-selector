package com.doth.selector.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 拦截循环引用的注解, 因职责问题可能需要更名, 目前有几种方案, 不过有一种我比较喜欢
 *  - DontBack
 *  在 DTO 版本最新的进一步完善中, 放宽了循环引用的检测, 原来是双方都需要标明, 该注解, 现在是双方只有一方标明即可, 所以这个注解或许叫做 'DontBack' 比较好
 *  但是还是建议开发者在互相持有关系中 都标明比较好
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OneToOne {
}