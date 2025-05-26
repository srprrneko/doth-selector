package com.doth.selector.executor.supports.lambda;

import com.doth.selector.common.testbean.join.Employee;
import com.doth.selector.common.util.TypeResolver;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class LambdaFieldPathResolver {

    /**
     * 最大代理创建数量
     */
    private static final int MAX_PROXY_DEPTH = 10;

    /**
     * writeReplace 的缓存
     */
    private static final ConcurrentHashMap<Class<?>, Method> WRITE_REPLACE_CACHE = new ConcurrentHashMap<>();

    public static <T, R> String resolve(SFunction<T, R> lambda, Class<T> rootClass) {
        // 策略 转换
        if (isMethodReference(lambda)) {
            return resolveFromMethodRef(lambda);
        } else {
            return resolveFromLambda(lambda, rootClass).getLastFieldPath();
        }
    }

    public static boolean isMethodReference(Serializable lambda) {
        SerializedLambda sl = extractSerializedLambda(lambda);
        return !sl.getImplMethodName().startsWith("lambda$");
    }


    /**
     * 方法引用形式，如 Employee::getName，返回 t0.name
     */
    public static <T, R> String resolveFromMethodRef(SFunction<T, R> methodRef) {
        try {
            String methodName = extractSerializedLambda(methodRef).getImplMethodName();
            return "t0." + convertMethodToField(methodName);
        } catch (Exception e) {
            throw new RuntimeException("方法引用解析失败，确认是否为标准 getter 引用", e);
        }
    }

    /**
     * Lambda链式访问，如 e -> e.getDept().getCompany().getName()
     */
    public static <T> LambdaPathRecorder resolveFromLambda(Function<T, ?> lambda, Class<T> rootClass) {
        LambdaPathRecorder recorder = new LambdaPathRecorder();
        try {
            T proxy = createCglibProxy(rootClass, recorder);
            lambda.apply(proxy);
        } catch (Exception e) {
            throw new RuntimeException("Lambda 执行失败，确认是否仅包含 getter 调用", e);
        }

        if (recorder.isEmpty()) {
            throw new RuntimeException("未能提取字段路径，请确保 Lambda 表达式中存在 getter 方法调用");
        }

        return recorder;
    }

    // ===== 工具方法 =====

    private static SerializedLambda extractSerializedLambda(Serializable lambda) {
        try {
            Method m = WRITE_REPLACE_CACHE.computeIfAbsent(lambda.getClass(), clazz -> {
                try {
                    Method method = clazz.getDeclaredMethod("writeReplace");
                    method.setAccessible(true);
                    return method;
                } catch (Exception e) {
                    throw new RuntimeException("获取 writeReplace 方法失败", e);
                }
            });
            return (SerializedLambda) m.invoke(lambda);
        } catch (Exception e) {
            throw new RuntimeException("无法提取 SerializedLambda", e);
        }
    }

    private static <T> T createCglibProxy(Class<T> clazz, LambdaPathRecorder recorder) {
        if (recorder.size() > MAX_PROXY_DEPTH) {
            throw new RuntimeException("Lambda getter 链过长，可能存在循环引用");
        }

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clazz);
        enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> {
            String name = method.getName();
            if (name.startsWith("get")) {
                recorder.append(decapitalize(name.substring(3)));
            } else if (name.startsWith("is")) {
                recorder.append(decapitalize(name.substring(2)));
            } else {
                throw new UnsupportedOperationException("仅支持 getter 方法: " + name);
            }

            Class<?> returnType = method.getReturnType();
            // 获取返回值
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
            return decapitalize(methodName.substring(3));
        } else if (methodName.startsWith("is")) {
            return decapitalize(methodName.substring(2));
        }
        return methodName;
    }

    private static String decapitalize(String str) {
        return str.isEmpty() ? "" : Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }


    public static void main(String[] args) {
        // 方法引用（SFunction）
        String path1 = resolve(Employee::getName, Employee.class);
        System.out.println("自动识别: " + path1);

        // 链式 lambda（Function）
        String path2 = resolve(e -> e.getDepartment().getCompany().getName(), Employee.class);
        System.out.println("自动识别: " + path2);
    }


}
