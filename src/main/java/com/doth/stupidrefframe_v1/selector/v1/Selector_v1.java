package com.doth.stupidrefframe_v1.selector.v1;

import com.doth.stupidrefframe_v1.selector.v1.core.executor.BuilderQueryExecutor;
import com.doth.stupidrefframe_v1.selector.v1.core.executor.DirectQueryExecutor;
import com.doth.stupidrefframe_v1.selector.v1.core.executor.DirectQueryExecutorPro;
import com.doth.stupidrefframe_v1.selector.v1.core.executor.RawQueryExecutor;

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
 */
public class Selector_v1<T> {
    // 类型缓存，每个子类实例持有自己的类型
    protected Class<T> beanClass;

    /**
     *       getGenericSuperclass() 方法属于 当前类的父类视角; 但通过子类的继承关系 反向获取泛型信息。
     *             java 泛型会通过类型擦除, 移除类型信息 但类继承关系中的泛型参数会被保留在字节码的签名
     * <p>
     *             因为继承的原因, getClass 获取到的是子类的Class 对象
     *             简单来讲, getClass 在继承关系下是可以反向获取子类的字节码对象的
     *             而普通默认关系下, 则获取的是当前类的class对象
     */
    public Selector_v1() {
        // 通过子类泛型参数获取具体类型
        Type superClass = getClass().getGenericSuperclass(); // 本质通过子类的继承关系, 反向获取父类泛型的具体参数
        if (!(superClass instanceof ParameterizedType)) {
            throw new IllegalArgumentException("子类必须指定泛型参数");
        }
        ParameterizedType pt = (ParameterizedType) superClass;
        this.beanClass = (Class<T>) pt.getActualTypeArguments()[0];
    }



    // 对象池模式
    // 建造者实现方式 缓冲区, Class 是传入的泛型, 对应执行器, 旨在避免多次new 对象
    private static final Map<Class<?>, BuilderQueryExecutor<?>> builderCache = new HashMap<>();

    // 自定义实现方式 缓冲区
    private static final Map<Class<?>, RawQueryExecutor<?>> rawCache = new HashMap<>();

    // 固定模版实现方式 缓冲区
    private static final Map<Class<?>, DirectQueryExecutor<?>> directCache = new HashMap<>();

    // 固定升级版模版实现方式 缓冲区
    private static final Map<Class<?>, DirectQueryExecutorPro<?>> directCachePro = new HashMap<>();



    // region flexible
    // 带 beanClass 参数的 重载旨在提供更灵活的查询, 例如 : 子类泛型指定的是Student, 但是需要传递Dto的class 对象, 返回T为dto的情况时
    @SuppressWarnings("unchecked")
    public static <T> BuilderQueryExecutor<T> build(Class<T> beanClass) {
        if (!builderCache.containsKey(beanClass)) {
            // 新建实例并确保类型匹配
            /// 加入缓冲区
            builderCache.put(beanClass, new BuilderQueryExecutor<>(beanClass));
        }
        // 从缓冲区获取对应的执行器, 避免多次创建  `
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
        if (!rawCache.containsKey(beanClass)) {
            rawCache.put(beanClass, new RawQueryExecutor<>(beanClass));
        }
        return (RawQueryExecutor<T>) rawCache.get(beanClass);
    }

    @SuppressWarnings("unchecked")
    public BuilderQueryExecutor<T> build() {
        if (!builderCache.containsKey(beanClass)) {
            builderCache.put(beanClass, new BuilderQueryExecutor<>(beanClass));
        }
        return (BuilderQueryExecutor<T>) builderCache.get(beanClass);
    }

    @SuppressWarnings("unchecked")
    public DirectQueryExecutor<T> direct() {
        if (!directCache.containsKey(beanClass)) {
            directCache.put(beanClass, new DirectQueryExecutor<>(beanClass));
        }
        return (DirectQueryExecutor<T>) directCache.get(beanClass);
    }

    @SuppressWarnings("unchecked")
    public DirectQueryExecutorPro<T> direct_() {
        if (!directCache.containsKey(beanClass)) {
            directCachePro.put(beanClass, new DirectQueryExecutorPro<>(beanClass));
        }
        return (DirectQueryExecutorPro<T>) directCachePro.get(beanClass);
    }

    /**
     * 清理所有缓存（供测试或特殊场景使用）
     *
     * <p>使用场景：
     * <ul>
     *   <li>单元测试后清理状态</li>
     *   <li>动态类加载器环境需要重新加载类</li>
     * </ul>
     */
    public static void clearAllCache() {
        builderCache.clear();
        rawCache.clear();
        directCache.clear();
    }
}