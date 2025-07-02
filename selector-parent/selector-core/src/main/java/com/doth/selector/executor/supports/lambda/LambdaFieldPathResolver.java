package com.doth.selector.executor.supports.lambda;

import com.doth.selector.common.util.TypeResolver;
import com.doth.selector.supports.exception.LambdaPathBuildException;
import com.doth.selector.supports.exception.LambdaResolveException;
import com.doth.selector.supports.exception.MethodReferenceResolveException;
import com.doth.selector.supports.testbean.join.Employee;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static com.doth.selector.common.util.NamingConvertUtil.camel2SnakeCase;
import static com.doth.selector.common.util.NamingConvertUtil.upperFstLetter;


/**
 * 该类用于将 Lambda 解析成字段路径的工具类
 * <p>
 * <strong>支持</strong>
 * <p>1. 方法引用: {@code Employee::getName}</p>
 * <p>2. 链式 Getter Lambda {@code e -> e.getDept().getName()}</p>
 * <br>
 * <strong>职责</strong>
 * <p>selector 中用于 SQL 条件子句构建</p>
 * <br>
 * </p>
 * <p>
 * <strong>技术实现</strong>
 * <P>1. 基于 CGLIB 拦截 getter 调用路径并记录</P>
 * <p>2. 通过 SerializedLambda 机制获取lambda信息 <p/>
 * </p>
 * <p>psvm >> 性能测试: 50000 次三表连接(三次代理创建)总耗时 600~800ms </p>
 *
 * @author War Nick
 */
@Slf4j
public class LambdaFieldPathResolver {

    /**
     * 最大代理创建数量
     */
    private static final int MAX_PROXY_DEPTH = 10;

    /**
     * writeReplace 的缓存
     */
    private static final ConcurrentHashMap<Class<?>, Method> WRITE_REPLACE_CACHE = new ConcurrentHashMap<>();

    /**
     * 解析入口
     * <p>支持并自动识别表达式类型 <strong>方法引用</strong> 与 <strong>链式 Lambda 表达式</strong> </p>
     *
     * @param lambda Lambda 表达式, '::' 或 '链式 getter >> emp.getDept.getCompany.getName...'
     * @param rootClass 表达式的根类型（例如 Employee.class）
     * @param <T> 根对象类型
     * @param <R> 返回类型
     * @return 字段路径 "tN.name"
     */

    public static <T, R> String resolve(SFunction<T, R> lambda, Class<T> rootClass) {
        // 方法引用分支
        if (isMethodReference(lambda)) {
            return resolveMethodRef(lambda);
        } else { // 链式getter分支
            return resolveLambda(lambda, rootClass).getLastFieldPath();
        }
    }

    public static boolean isMethodReference(Serializable lambda) {
        SerializedLambda sl = extractSerializedLambda(lambda);
        return !sl.getImplMethodName().startsWith("lambda$");
    }


    public static <T, R> String resolveMethodRef(SFunction<T, R> methodRef) {
        try {
            String methodName = extractSerializedLambda(methodRef).getImplMethodName();
            return "t0." + convertMethodToField(methodName);
        } catch (Exception e) {
            throw new MethodReferenceResolveException("方法引用解析失败：请确认使用标准 getter 方法，如 Employee::getName", e);
        }
    }

    /**
     * 链式 getter -> Lambda 解析
     * <p>适用于 {@code e -> e.getDepartment().getName()} 这种结构</p>
     * @param lambda lambda 表达式
     * @param rootClass 起始类
     * @return 路径记录器，内部保存字段链
     */
    public static <T> LambdaPathRecorder resolveLambda(Function<T, ?> lambda, Class<T> rootClass) {
        LambdaPathRecorder recorder = new LambdaPathRecorder();
        try {
            T proxy = createCglibProxy(rootClass, recorder);
            lambda.apply(proxy);
        } catch (Exception e) {
            throw new LambdaPathBuildException("Lambda 执行失败：请仅包含 getter 方法调用", e);
        }

        if (recorder.isEmpty()) {
            throw new LambdaPathBuildException("Lambda 表达式未能提取任何字段路径：请确保表达式中调用了 getter 方法");
        }

        return recorder;
    }

    // !!============================== 工具方法区域 ==============================!!

    /**
     * 提取 Lambda 表达式的 SerializedLambda 实例
     * <p>通过反射调用 lambda 内部的 writeReplace 方法获取其序列化结构</p>
     *
     * @param lambda Lambda 表达式（必须实现 Serializable）
     * @return SerializedLambda 对象
     */
    private static SerializedLambda extractSerializedLambda(Serializable lambda) {
        try {
            Method m = WRITE_REPLACE_CACHE.computeIfAbsent(lambda.getClass(), clazz -> {
                try {
                    Method method = clazz.getDeclaredMethod("writeReplace");
                    method.setAccessible(true);
                    return method;
                } catch (Exception e) {
                    throw new LambdaResolveException("提取 writeReplace 方法失败：可能不是合法的 lambda 表达式", e);
                }
            });
            return (SerializedLambda) m.invoke(lambda);
        } catch (Exception e) {
            throw new LambdaResolveException("提取 writeReplace 方法失败：可能不是合法的 lambda 表达式", e);
        }
    }

    /**
     * 递归创建 CGLIB 拦截 getter 调用并记录字段路径
     * @param clazz 要代理的类
     * @param recorder 路径记录器
     * @param <T> 被代理类型
     * @return 创建后的代理对象
     */
    private static <T> T createCglibProxy(Class<T> clazz, LambdaPathRecorder recorder) {
        if (recorder.size() > MAX_PROXY_DEPTH) {
            throw new LambdaPathBuildException("Lambda getter 链过长，存在循环引用风险，最大深度为 " + MAX_PROXY_DEPTH);
        }

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clazz);
        enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> {
            String name = method.getName();
            if (name.startsWith("get")) {
                recorder.append(camel2SnakeCase(name.substring(3), false));
            } else if (name.startsWith("is")) {
                recorder.append(camel2SnakeCase(name.substring(2), false));
            } else
                throw new LambdaPathBuildException("不支持的 Lambda 表达式：仅支持 getter 方法调用，例: getName()/isDel()");


            Class<?> returnType = method.getReturnType();
            // 拦截非自定义类
            if (!returnType.isPrimitive() && !returnType.getName().startsWith("java.")) {
                // 递归继续创建代理
                return createCglibProxy(returnType, recorder);
            } else {
                return TypeResolver.getDefaultValue(returnType);
            }
        });
        return clazz.cast(enhancer.create());
    }

    private static String convertMethodToField(String methodName) {
        if (methodName.startsWith("get")) {
            return camel2SnakeCase(methodName.substring(3), false);
        } else if (methodName.startsWith("is")) {
            return camel2SnakeCase(methodName.substring(2), false);
        }
        return methodName;
    }

    /**
     * 性能测试
     */
    public static void main(String[] args) {
        // 方法引用
        // String path1 = resolve(Employee::getName, Employee.class);
        // System.out.println("自动识别: " + path1);

        // 链式 lambda（Function）
        long start = System.currentTimeMillis();
        for (int i = 0; i < 50000; i++) {
            resolve(e -> e.getDepartment().getCompany().getName(), Employee.class);
        }
        long end = System.currentTimeMillis();
        System.out.println("end - start = " + (end - start));
        // System.out.println("自动识别: " + path2);
    }


}
