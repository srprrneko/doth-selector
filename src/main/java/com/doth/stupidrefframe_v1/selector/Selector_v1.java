package com.doth.stupidrefframe_v1.selector;

import com.doth.stupidrefframe_v1.selector.executer.BuilderQueryExecutor;
import com.doth.stupidrefframe_v1.selector.executer.DirectQueryExecutor;
import com.doth.stupidrefframe_v1.selector.executer.RawQueryExecutor;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * 实体查询门面类 - 提供统一的静态入口，整合多种查询策略的执行器
 *
 * <p>设计目标：
 * <ol>
 *   <li>通过缓存机制复用执行器实例，减少对象创建开销</li>
 *   <li>通过泛型保证类型安全，避免强制转换错误</li>
 *   <li>隐藏底层实现细节，提供简洁的链式调用API</li>
 * </ol>
 *
 * <p>注意事项：
 * <ul>
 *   <li>执行器需设计为无状态，确保线程安全和复用安全</li>
 *   <li>缓存键使用 Class<?> 通配符以支持多种实体类型</li>
 *   <li>强制转型在门面类内部可控，外部调用无需感知</li>
 * </ul>
 */
public class Selector_v1<T> {
    // 类型缓存，每个子类实例持有自己的类型
    protected Class<T> beanClass;

    public Selector_v1() {
        // 通过子类泛型参数获取具体类型
        Type superClass = getClass().getGenericSuperclass();
        if (!(superClass instanceof ParameterizedType)) {
            throw new IllegalArgumentException("子类必须指定泛型参数");
        }
        ParameterizedType pt = (ParameterizedType) superClass;
        this.beanClass = (Class<T>) pt.getActualTypeArguments()[0];
    }





    private static final Map<Class<?>, BuilderQueryExecutor<?>> builderCache = new HashMap<>();


    private static final Map<Class<?>, RawQueryExecutor<?>> rawCache = new HashMap<>();


    private static final Map<Class<?>, DirectQueryExecutor<?>> directCache = new HashMap<>();


    // region ez impl
    @SuppressWarnings("unchecked")
    public static <T> BuilderQueryExecutor<T> builder(Class<T> beanClass) {
        if (!builderCache.containsKey(beanClass)) {
            // 新建实例并确保类型匹配
            builderCache.put(beanClass, new BuilderQueryExecutor<>(beanClass));
        }
        // 安全转型：存入时已保证 Class<T> 对应 BuilderQueryExecutor<T>
        return (BuilderQueryExecutor<T>) builderCache.get(beanClass);
    }

    @SuppressWarnings("unchecked")
    public static <T> RawQueryExecutor<T> raw(Class<T> beanClass) {
        if (!rawCache.containsKey(beanClass)) {
            rawCache.put(beanClass, new RawQueryExecutor<>(beanClass));
        }
        return (RawQueryExecutor<T>) rawCache.get(beanClass);
    }

    @SuppressWarnings("unchecked")
    public static <T> DirectQueryExecutor<T> direct(Class<T> beanClass) {
        if (!directCache.containsKey(beanClass)) {
            directCache.put(beanClass, new DirectQueryExecutor<>(beanClass));
        }
        return (DirectQueryExecutor<T>) directCache.get(beanClass);
    }
    // endregion

    // 通过实例方法绑定泛型类型
    @SuppressWarnings("unchecked")
    public RawQueryExecutor<T> raw() {
        if (!builderCache.containsKey(beanClass)) {
            // 新建实例并确保类型匹配
            rawCache.put(beanClass, new RawQueryExecutor<>(beanClass));
        }
        // 安全转型：存入时已保证 Class<T> 对应 BuilderQueryExecutor<T>
        return (RawQueryExecutor<T>)  rawCache.get(beanClass);
    }

    // @SuppressWarnings("unchecked")

    // @SuppressWarnings("unchecked")
    // public static <T> BuilderQueryExecutor<T> builder() {
    //     if (!builderCache.containsKey(beanClass)) {
    //         // 新建实例并确保类型匹配
    //         builderCache.put(beanClass, new BuilderQueryExecutor<>(beanClass));
    //     }
    //     // 安全转型：存入时已保证 Class<T> 对应 BuilderQueryExecutor<T>
    //     return (BuilderQueryExecutor<T>) builderCache.get(beanClass);
    // }



    // @SuppressWarnings("unchecked")
    // public static <T> DirectQueryExecutor<T> direct() {
    //     if (!directCache.containsKey(beanClass)) {
    //         directCache.put(beanClass, new DirectQueryExecutor<>(beanClass));
    //     }
    //     return (DirectQueryExecutor<T>) directCache.get(beanClass);
    // }

    // /**
    //  * 清理所有缓存（供测试或特殊场景使用）
    //  *
    //  * <p>使用场景：
    //  * <ul>
    //  *   <li>单元测试后清理状态</li>
    //  *   <li>动态类加载器环境需要重新加载类</li>
    //  * </ul>
    //  */
    // public static void clearAllCache() {
    //     builderCache.clear();
    //     rawCache.clear();
    //     directCache.clear();
    // }
}