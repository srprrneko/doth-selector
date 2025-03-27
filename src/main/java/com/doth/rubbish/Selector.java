package com.doth.rubbish;

import com.doth.stupidrefframe_v1.selector.executer.BuilderQueryExecutor;
import com.doth.stupidrefframe_v1.selector.executer.DirectQueryExecutor;
import com.doth.stupidrefframe_v1.selector.executer.RawQueryExecutor;

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
public final class Selector {
    /**
     * 构建器模式执行器缓存（Key: 实体类型）
     * <p>说明：
     * <ul>
     *   <li>使用 Class<?> 作为键，允许存储任意实体类型</li>
     *   <li>值类型为 BuilderQueryExecutor<?>，实际存储时泛型参数会被擦除</li>
     *   <li>通过门面类的类型约束确保键值匹配</li>
     * </ul>
     */
    private static final Map<Class<?>, BuilderQueryExecutor<?>> builderCache = new HashMap<>();

    /**
     * 原生SQL执行器缓存（Key: 实体类型）
     * <p>设计考虑：
     * <ul>
     *   <li>无状态执行器可安全复用，无需每次创建新实例</li>
     *   <li>缓存生命周期与应用一致，避免频繁GC</li>
     * </ul>
     */
    private static final Map<Class<?>, RawQueryExecutor<?>> rawCache = new HashMap<>();

    /**
     * 直接条件查询执行器缓存（Key: 实体类型）
     * <p>类型安全保证：
     * <ul>
     *   <li>get方法通过泛型参数 T 约束返回类型</li>
     *   <li>存入缓存时已确保 Class<T> 对应 Executor<T></li>
     * </ul>
     */
    private static final Map<Class<?>, DirectQueryExecutor<?>> directCache = new HashMap<>();

    /**
     * 获取构建器模式查询执行器
     *
     * @param beanClass 目标实体类型（如 User.class）
     * @param <T>       实体类型泛型参数
     * @return 已缓存的或新建的构建器执行器实例
     *
     * <p>实现细节：
     * <ol>
     *   <li>首次调用时创建实例并缓存</li>
     *   <li>后续调用直接返回缓存实例</li>
     *   <li>强制转型安全由泛型参数 T 保证</li>
     * </ol>
     */
    @SuppressWarnings("unchecked")
    public static <T> BuilderQueryExecutor<T> builder(Class<T> beanClass) {
        if (!builderCache.containsKey(beanClass)) {
            // 新建实例并确保类型匹配
            builderCache.put(beanClass, new BuilderQueryExecutor<>(beanClass));
        }
        // 安全转型：存入时已保证 Class<T> 对应 BuilderQueryExecutor<T>
        return (BuilderQueryExecutor<T>) builderCache.get(beanClass);
    }

    /**
     * 获取原生SQL查询执行器
     *
     * @param beanClass 目标实体类型
     * @param <T>       实体类型泛型参数
     * @return 已初始化的执行器实例
     *
     * <p>设计要点：
     * <ul>
     *   <li>执行器内部不维护查询状态，可安全复用</li>
     *   <li>实际SQL生成在每次查询时动态创建</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    public static <T> RawQueryExecutor<T> raw(Class<T> beanClass) {
        if (!rawCache.containsKey(beanClass)) {
            rawCache.put(beanClass, new RawQueryExecutor<>(beanClass));
        }
        return (RawQueryExecutor<T>) rawCache.get(beanClass);
    }

    /**
     * 获取直接条件查询执行器
     *
     * @param beanClass 目标实体类型
     * @param <T>       实体类型泛型参数
     * @return 已配置的执行器实例
     *
     * <p>功能说明：
     * <ul>
     *   <li>支持Map条件、JavaBean条件等多种查询方式</li>
     *   <li>内部通过SelectorHelper处理字段映射</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    public static <T> DirectQueryExecutor<T> direct(Class<T> beanClass) {
        if (!directCache.containsKey(beanClass)) {
            directCache.put(beanClass, new DirectQueryExecutor<>(beanClass));
        }
        return (DirectQueryExecutor<T>) directCache.get(beanClass);
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