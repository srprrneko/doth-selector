package com.doth.selector.common.temp;

import com.doth.selector.common.SFunction;
import com.doth.selector.common.testbean.join.Employee;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Lambda字段路径解析器：
 *
 * 提供两种方式解析Java Lambda表达式中字段的结构信息：
 * 1. 单字段方法引用：如 Employee::getName，解析为字段名、类型、所属类
 * 2. 链式字段访问：如 e -> e.getDept().getCompany().getName()，解析为完整字段路径、类型、所属类
 *
 * 应用场景：用于动态构建SQL条件、生成查询字段路径、注解处理器等
 */
public class LambdaFieldPathResolver {

    /** writeReplace 缓存，用于提升 SerializedLambda 提取效率 */
    private static final ConcurrentMap<Class<?>, Method> WRITE_REPLACE_CACHE = new ConcurrentHashMap<>();

    /** lambda 中实现类缓存 */
    private static final ConcurrentMap<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();

    /** 字段路径缓存 */
    private static final ConcurrentMap<String, LbdMeta> FIELD_INFO_CACHE = new ConcurrentHashMap<>();

    /** 字段类型缓存 */
    private static final ConcurrentMap<String, Class<?>> FIELD_TYPE_CACHE = new ConcurrentHashMap<>();

    /**
     * 解析方法引用（如 Employee::getName）为字段元信息
     */
    public static <T, R> LbdMeta resolveFieldFromMethodRef(SFunction<T, R> methodRef) {
        Class<?> clazz = getLambdaImplClass(methodRef);
        String methodName = getLambdaMethodName(methodRef);

        String cacheKey = clazz.getName() + "|" + methodName;
        return FIELD_INFO_CACHE.computeIfAbsent(cacheKey, k -> buildMetaFromClassAndMethod(clazz, methodName));
    }

    /**
     * 解析链式Lambda表达式（如 e -> e.getDept().getName()）为字段路径元信息
     * 原: 使用反射
     *  发现lambda 字节码里, 只保留了入口方法和类, 于是在嵌套场景下只能拿到 t0(预期t1+) 和 从类
     *      所以实现不了, 要是 实现的了的话, 也会超级复杂, 没那个必要
     * 现: 使用 代理
     *
     */
    public static <T> LbdMeta resolveFieldPathFromLambda(SFunction<T, ?> lambda) {
        List<String> path = new ArrayList<>();
        Class<T> rootClass = getLambdaParameterType(lambda);
        T proxy = createCglibProxy(rootClass, path);

        lambda.apply(proxy);

        if (path.isEmpty()) throw new RuntimeException("未能从 Lambda 表达式中提取字段路径");

        List<String> aliasPath = new ArrayList<>();
        for (int i = 0; i < path.size(); i++) {
            aliasPath.add("t" + i + "." + path.get(i));
        }

        String finalAliasedPath = aliasPath.get(aliasPath.size() - 1);
        Class<?> current = rootClass;
        for (int i = 0; i < path.size() - 1; i++) {
            current = resolveFieldType(current, path.get(i));
        }
        String lastField = path.get(path.size() - 1);
        Class<?> fieldType = resolveFieldType(current, lastField);

        return new LbdMeta(current, finalAliasedPath, fieldType);
    }

    // ======== Internal helpers ========

    // 用于获取 serializable lambda 结构信息: 通过缓存,
    private static SerializedLambda extractSerializedLambda(Serializable lambda) {
        try {
            Method writeReplace = WRITE_REPLACE_CACHE.computeIfAbsent(lambda.getClass(), clazz -> {
                try {
                    Method m = clazz.getDeclaredMethod("writeReplace");
                    m.setAccessible(true);
                    return m;
                } catch (Exception e) {
                    throw new RuntimeException("获取writeReplace方法失败", e);
                }
            });
            return (SerializedLambda) writeReplace.invoke(lambda);
        } catch (Exception e) {
            throw new RuntimeException("无法提取 SerializedLambda", e);
        }
    }

    private static String getLambdaMethodName(Serializable lambda) {
        return extractSerializedLambda(lambda).getImplMethodName();
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getLambdaImplClass(Serializable lambda) {
        String className = extractSerializedLambda(lambda).getImplClass().replace('/', '.'); //
        return (Class<T>) CLASS_CACHE.computeIfAbsent(className, name -> {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("类未找到: " + name, e);
            }
        });
    }

    private static LbdMeta buildMetaFromClassAndMethod(Class<?> clazz, String methodName) {
        String fieldName = convertMethodToField(methodName);
        Class<?> fieldType = getFieldTypeSafely(clazz, methodName, fieldName);
        String key = clazz.getName() + "#" + fieldName;
        String finalFieldName = FIELD_TYPE_CACHE.containsKey(key) ? fieldName : methodName;
        return new LbdMeta(clazz, "t0." + finalFieldName, fieldType);
    }

    private static Class<?> getFieldTypeSafely(Class<?> clazz, String methodName, String fieldName) {
        try {
            return resolveFieldType(clazz, fieldName);
        } catch (RuntimeException e) {
            return resolveFieldType(clazz, methodName);
        }
    }

    private static Class<?> resolveFieldType(Class<?> clazz, String fieldName) {
        String key = clazz.getName() + "#" + fieldName;
        return FIELD_TYPE_CACHE.computeIfAbsent(key, k -> {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                return field.getType();
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("字段不存在: " + clazz.getSimpleName() + "." + fieldName, e);
            }
        });
    }

    public static String convertMethodToField(String methodName) {
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

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getLambdaParameterType(SFunction<T, ?> lambda) {
        try {
            SerializedLambda serializedLambda = extractSerializedLambda(lambda);
            String methodSignature = serializedLambda.getImplMethodSignature();
            String internalName = methodSignature.substring(2, methodSignature.indexOf(';'));
            String className = internalName.replace('/', '.');
            return (Class<T>) Class.forName(className);
        } catch (Exception e) {
            throw new RuntimeException("无法解析 Lambda 参数类型", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T createCglibProxy(Class<T> clazz, List<String> path) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clazz);
        enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> {
            String methodName = method.getName();
            if (methodName.startsWith("get")) {
                path.add(decapitalize(methodName.substring(3)));
            } else if (methodName.startsWith("is")) {
                path.add(decapitalize(methodName.substring(2)));
            } else {
                throw new UnsupportedOperationException("仅支持 getter 方法: " + methodName);
            }

            Class<?> returnType = method.getReturnType();
            if (!returnType.isPrimitive() && !returnType.getName().startsWith("java.")) {
                return createCglibProxy(returnType, path);
            } else {
                return getDefaultValue(returnType);
            }
        });
        return (T) enhancer.create();
    }

    private static Object getDefaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        return null;
    }

    public static void main(String[] args) {
        /*
            lambda 运行时会被编译成一个匿名内部类, 这个类内部实现了 writeReplace 方法, 返回的是一个 serializableLambda 对象
            其中包含
                1.函数实现类名（即 lambda 写在哪里）
                2.实现方法名（如 getName）
                3.方法签名
                4.被调用的字段/方法的实际类名
               implClass = "com/your/package/YourClass",    // 实际实现方法所在类（字符串）
               implMethodName = "getName",                  // 实现方法名
               implMethodSignature = "()Ljava/lang/String;" // 方法签名

         */
        LbdMeta meta = resolveFieldPathFromLambda((Employee e) -> e.getDepartment().getCompany().getName());
        System.out.println("解析结果: " + meta);
        LbdMeta meta1 = resolveFieldFromMethodRef(Employee::getName);
        System.out.println("meta1 = " + meta1);
    }
}
