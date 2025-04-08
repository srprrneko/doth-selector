package com.doth.stupidrefframe.selector.v1.core;

import com.doth.stupidrefframe.selector.v1.executor.basic.BasicKindQueryExecutor;
import com.doth.stupidrefframe.selector.v1.executor.basic.query.BuilderQueryExecutor;
import com.doth.stupidrefframe.selector.v1.executor.basic.query.DirectQueryExecutor;
import com.doth.stupidrefframe.selector.v1.executor.basic.query.RawQueryExecutor;
import com.doth.stupidrefframe.selector.v1.executor.enhanced.query.BuilderQueryExecutorPro;
import com.doth.stupidrefframe.selector.v1.executor.enhanced.query.DirectQueryExecutorPro;
import com.doth.stupidrefframe.selector.v1.executor.enhanced.query.RawQueryExecutorPro;
import com.doth.stupidrefframe.selector.v1.core.factory.CreateExecutorFactory;
import com.doth.stupidrefframe.selector.v1.core.factory.impl.DefaultCreateExecutorFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * 实体查询门面类 - 提供所有查询方法使用入口, 整合多种查询策略的执行器, 简化调用复杂度, 提高代码可读性
 *
 * <p>设计目标：
 * <ol>
 *   <li>通过缓存机制复用执行器实例，减少对象创建开销</li>
 *   <li>通过泛型保证类型安全，避免强制转换错误</li>
 *   <li>隐藏底层实现细节，提供简洁的链式调用API</li>
 * </ol>
 */
public class SelectorV2<T> {

    protected Class<T> beanClass;
    // 默认执行器工厂（可通过替换实现扩展）
    private static CreateExecutorFactory createExecutorFactory = new DefaultCreateExecutorFactory();
    // 统一缓存：Class -> 执行器实例
    private static final Map<Class<?>, Map<ExecutorType, BasicKindQueryExecutor<?>>> cache = new HashMap<>();



    public SelectorV2() {
        Type superClass = getClass().getGenericSuperclass();
        if (!(superClass instanceof ParameterizedType)) {
            throw new IllegalArgumentException("子类必须指定泛型参数");
        }
        this.beanClass = (Class<T>) ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }



    // region 静态方法API
    public static <T> BuilderQueryExecutor<T> bud(Class<T> beanClass) {
        return getExecutor(beanClass, ExecutorType.BUILDER, BuilderQueryExecutor.class);
    }

    public static <T> BuilderQueryExecutorPro<T> bud$(Class<T> beanClass) {
        return getExecutor(beanClass, ExecutorType.BUILDER_PRO, BuilderQueryExecutorPro.class);
    }


    public static <T> RawQueryExecutor<T> raw(Class<T> beanClass) {
        return getExecutor(beanClass, ExecutorType.RAW, RawQueryExecutor.class);
    }

    public static <T> RawQueryExecutorPro<T> raw$(Class<T> beanClass) {
        return getExecutor(beanClass, ExecutorType.RAW_PRO, RawQueryExecutorPro.class);
    }


    public static <T> DirectQueryExecutor<T> dct(Class<T> beanClass) {
        return getExecutor(beanClass, ExecutorType.DIRECT, DirectQueryExecutor.class);
    }

    public static <T> DirectQueryExecutorPro<T> dct$(Class<T> beanClass) {
        return getExecutor(beanClass, ExecutorType.DIRECT_PRO, DirectQueryExecutorPro.class);
    }
    // endregion



    // region 实例方法API
    public BuilderQueryExecutor<T> bud() {
        return getExecutor(beanClass, ExecutorType.BUILDER, BuilderQueryExecutor.class);
    }

    public BuilderQueryExecutorPro<T> bud$() {
        return getExecutor(beanClass, ExecutorType.BUILDER_PRO, BuilderQueryExecutorPro.class);
    }


    public RawQueryExecutor<T> raw() {
        return getExecutor(beanClass, ExecutorType.RAW, RawQueryExecutor.class);
    }

    public RawQueryExecutorPro<T> raw$() {
        return getExecutor(beanClass, ExecutorType.RAW_PRO, RawQueryExecutorPro.class);
    }


    public DirectQueryExecutor<T> dct() {
        return getExecutor(beanClass, ExecutorType.DIRECT, DirectQueryExecutor.class);
    }

    public DirectQueryExecutorPro<T> dct$() {
        return getExecutor(beanClass, ExecutorType.DIRECT_PRO, DirectQueryExecutorPro.class);
    }

    // endregion



    /**
     * 工厂获取中介
     *  1.通过目标实例获取对应的所有执行器map
     *  2.在执行器map中 通过执行器枚举 获取或创建 对应的单个执行器
     *
     * @param beanClass 目标 Bean 类型 (DAO)
     * @param type 执行器类型
     * @param executorClass 执行器类型
     * @return 执行器实例
     * @param <E> 界限划定, 确保执行器类型正确
     */
    @SuppressWarnings("unchecked")
    private static <E extends BasicKindQueryExecutor<?>> E getExecutor(Class<?> beanClass, ExecutorType type, Class<E> executorClass) {
        // 获取或初始化类缓存
        Map<ExecutorType, BasicKindQueryExecutor<?>> classCache = cache.computeIfAbsent(beanClass, k
                -> new HashMap<>()
        ); // 一参键目标类, 一参没有二参创建

        // 懒加载执行器
        BasicKindQueryExecutor<?> executor = classCache.computeIfAbsent(type, t -> createExecutorFactory.createExecutor(beanClass, type));

        // 类型安全检查
        if (!executorClass.isInstance(executor)) { // 对等检查, 避免类型混乱
            throw new IllegalStateException("执行器类型不匹配: " + executor.getClass());
        }
        return (E) executor;
    }



    /**
     * 替换工厂 todo
     * @param factory 工厂
     */
    @Deprecated
    public static void setExecutorFactory(CreateExecutorFactory factory) {
        createExecutorFactory = factory;
    }



    /**
     * 清理缓存
     */
    public static void clearAllCache() {
        cache.clear();
    }
}