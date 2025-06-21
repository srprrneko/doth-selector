package com.doth.selector.executor.supports.lambda;

import java.io.Serializable;
import java.util.function.Function;

/**
 * 本质上是通过 java 对 lambda 的特殊机制, 让 lambda 表达式变的透明化
 *  在 该接口中 自动 加入了 writeReplace 方法, 该方法返回的 是一个序列化后的 lambda表达式, 这是JVM为我们做的
 *      并且当这个类被序列化之后, JVM会自动调用这个方法, 然后返回一个 SerializedLambda 实例, 其中包含所有lambda 的信息
 *
 * @param <T>
 * @param <R>
 */
@FunctionalInterface
public interface SFunction<T, R> extends Function<T, R>, Serializable {
    // private Object writeReplace() throws ObjectStreamException { 实际上是有这个方法的
    //     return new SerializedLambda(...);
    // }

}
