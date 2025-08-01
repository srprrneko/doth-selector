package com.doth.selector.core;

import com.doth.selector.core.factory.CreateExecutorFactory;
import com.doth.selector.core.factory.impl.DefaultCreateExecutorFactory;
import com.doth.selector.core.model.ExecutorType;
import com.doth.selector.executor.query.BasicKindQueryExecutor;
import com.doth.selector.executor.query.basic.impl.BuilderQueryExecutor;
import com.doth.selector.executor.query.basic.impl.DirectQueryExecutor;
import com.doth.selector.executor.query.basic.impl.RawQueryExecutor;
import com.doth.selector.executor.query.enhanced.impl.BuilderQueryExecutorPro;
import com.doth.selector.executor.query.enhanced.impl.DirectQueryExecutorPro;
import com.doth.selector.executor.query.enhanced.impl.RawQueryExecutorPro;
import com.doth.selector.executor.supports.QueryList;
import com.doth.selector.executor.supports.builder.ConditionBuilder;
import com.doth.selector.executor.supports.lambda.Lambda2FieldNameResolver;
import com.doth.selector.executor.supports.lambda.SFunction;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

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
public class Selector<T> {

    protected Class<T> beanClass;

    public Class<T> getBeanClass() {
        return beanClass;
    }

    // 默认执行器工厂 可通过替换实现扩展
    private static CreateExecutorFactory createExecutorFactory = new DefaultCreateExecutorFactory();

    private static final Map<Class<?>, Map<ExecutorType, BasicKindQueryExecutor<?>>> cache = new ConcurrentHashMap<>();


    // 将动态获取子类泛型更改成了遍历继承链获取泛型, 避免抽象dao的子实现类不指定泛型则报错的现象
    public Selector() {
        // 指向实际实例化的子类, 例: this.class = EmpDAOImpl 而非 selector 本类
        // 用于解析逻辑从最底层的实现类开始，沿继承链向上查找，确保能定位到开发者定义的泛型参数。
        this.beanClass = resolveBeanClass(this.getClass());
    }

    @SuppressWarnings("unchecked")
    private Class<T> resolveBeanClass(Class<?> clazz) {

        // 从子类开始, 递进查找继承体系, 直到本类
        while (clazz != null && !clazz.equals(Selector.class)) { // 区间: 子类 -> ... <- self

            Type superType = clazz.getGenericSuperclass();// 返回泛型参数化类, 例: Selector<Employee>, 用于获取具体类型

            if (superType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) superType;

                // 确保处理的是当前类 的原始类是 本类 -> 防御
                if (pt.getRawType() == Selector.class) {
                    // 获取实际泛型参数
                    Type typeArg = pt.getActualTypeArguments()[0];

                    // 判断用于确保类型转换安全
                    if (typeArg instanceof Class) {
                        return (Class<T>) typeArg;
                    } else if (typeArg instanceof ParameterizedType) { //
                        // 处理嵌套泛型, 例: 如List<Employee> -> List.class
                        return (Class<T>) ((ParameterizedType) typeArg).getRawType();
                    } else if (typeArg instanceof WildcardType) {
                        throw new IllegalArgumentException("泛型参数不能是通配符类型");
                    }
                    return (Class<T>) typeArg;
                }
            }
            clazz = clazz.getSuperclass(); // 递进继承链
        }
        throw new IllegalArgumentException("必须在继承链中指定泛型参数");
    }


    // region 静态方法API

    /**
     * <p>因为当前 pro$ 已支持单表映射, 所以暂时不推荐使用</p>
     */
    @Deprecated
    public static <T> BuilderQueryExecutor<T> bud(Class<T> beanClass) {
        return getExecutor(beanClass, ExecutorType.BUILDER, BuilderQueryExecutor.class);
    }

    public static <T> BuilderQueryExecutorPro<T> bud$(Class<T> beanClass) {
        return getExecutor(beanClass, ExecutorType.BUILDER_PRO, BuilderQueryExecutorPro.class);
    }

    /**
     * <p>因为当前 pro$ 已支持单表映射, 所以暂时不推荐使用</p>
     */
    @Deprecated
    public static <T> RawQueryExecutor<T> raw(Class<T> beanClass) {
        return getExecutor(beanClass, ExecutorType.RAW, RawQueryExecutor.class);
    }

    public static <T> RawQueryExecutorPro<T> raw$(Class<T> beanClass) {
        return getExecutor(beanClass, ExecutorType.RAW_PRO, RawQueryExecutorPro.class);
    }

    /**
     * <p>因为当前 pro$ 已支持单表映射, 所以暂时不推荐使用</p>
     */
    @Deprecated
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

    @SuppressWarnings("unchecked")
    public <D> QueryList<D> queryD(Class<D> dtoClass, Consumer<ConditionBuilder<T>> setup) {
        BuilderQueryExecutorPro<T> executor = bud$();
        executor.setDtoClass(dtoClass);
        return (QueryList<D>) executor.query(setup, true);
    }


    public RawQueryExecutor<T> raw() {
        return getExecutor(beanClass, ExecutorType.RAW, RawQueryExecutor.class);
    }

    public RawQueryExecutorPro<T> raw$() {
        return getExecutor(beanClass, ExecutorType.RAW_PRO, RawQueryExecutorPro.class);
    }


    public DirectQueryExecutor<T> dct() { // 单表时可以使用
        return getExecutor(beanClass, ExecutorType.DIRECT, DirectQueryExecutor.class);
    }

    public DirectQueryExecutorPro<T> dct$() {
        return getExecutor(beanClass, ExecutorType.DIRECT_PRO, DirectQueryExecutorPro.class);
    }

    // endregion


    /**
     * 工厂获取中介
     * 1.通过目标实例获取对应的所有执行器map
     * 2.在执行器map中 通过执行器枚举 获取或创建 对应的单个执行器
     *
     * @param beanClass     目标 Bean 类型 (DAO)
     * @param type          执行器类型
     * @param executorClass 执行器类型
     * @param <E>           界限划定, 确保执行器类型正确
     * @return 执行器实例
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
     * <p>用于将 lambda 转换成解析好的字符串, 避免冗长命名</p>
     *
     * @param lambda 表达式
     * @return 解析好的字符串
     */
    public String f(SFunction<T, ?> lambda) {
        return Lambda2FieldNameResolver.resolve(lambda, beanClass);
    }


    /**
     * 替换工厂 todo: 暂无其他工厂可替换
     *
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