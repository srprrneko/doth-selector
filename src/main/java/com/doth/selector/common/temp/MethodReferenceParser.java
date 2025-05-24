package com.doth.selector.common.temp;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.function.Function;

public class MethodReferenceParser {

    public interface SerializableFunction<T, R> extends Function<T, R>, Serializable {}

    public static <T, R> FieldInfo parse(SerializableFunction<T, R> methodRef) {
        // 提取序列化结构
        SerializedLambda lambda = extractSerializedLambda(methodRef);
        Class<?> implClass = resolveImplClass(lambda);
        String fieldName = extractFieldName(lambda);
        return new FieldInfo(implClass, fieldName);
    }

    /**
     * 提取 SerializedLambda 结构
     */
    private static SerializedLambda extractSerializedLambda(Serializable function) {
        try {
            Method writeReplace = function.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            return (SerializedLambda) writeReplace.invoke(function);
        } catch (Exception e) {
            throw new RuntimeException("无法解析 Lambda 表达式，请确认方法引用符合规范", e);
        }
    }

    /**
     * 解析 lambda 对应的实现类
     */
    private static Class<?> resolveImplClass(SerializedLambda lambda) {
        try {
            return Class.forName(lambda.getImplClass().replace('/', '.')); // com/example/Employee >>　com.example.Employee
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("无法加载 lambda 对应的类：" + lambda.getImplClass(), e);
        }
    }

    /**
     * 提取字段名（从方法名如 getEmpName 推断出 empName）
     */
    private static String extractFieldName(SerializedLambda lambda) {
        return convertMethodToField(lambda.getImplMethodName());
    }

    private static String convertMethodToField(String methodName) {
        if (methodName.startsWith("get")) {
            return uncapitalize(methodName.substring(3));
        } else if (methodName.startsWith("is")) {
            return uncapitalize(methodName.substring(2));
        }
        return methodName;
    }

    private static String uncapitalize(String str) {
        return str.isEmpty() ? "" : Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    // 输出字段信息
    public static class FieldInfo {
        private final Class<?> clazz;
        private final String fieldName;

        public FieldInfo(Class<?> clazz, String fieldName) {
            this.clazz = clazz;
            this.fieldName = fieldName;
        }

        @Override
        public String toString() {
            return clazz.getSimpleName() + ".class, " + fieldName;
        }

        public Class<?> getClazz() {
            return clazz;
        }
    }

    // 示例实体和测试
    static class Employee {
        private String empName;
        public String getEmpName() { return empName; }
    }

    public static void main(String[] args) {
        FieldInfo info = parse(Employee::getEmpName);
        System.out.println(info.getClazz());

        inspect(Employee::getEmpName);
    }






    private static <T, R> void inspect(SerializableFunction<T, R> func) {
        try {
            Method writeReplace = func.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            SerializedLambda lambda = (SerializedLambda) writeReplace.invoke(func);

            System.out.println("------ Lambda Structure ------");
            System.out.println("implClass           : " + lambda.getImplClass());
            System.out.println("implMethodName      : " + lambda.getImplMethodName());
            System.out.println("implMethodSignature : " + lambda.getImplMethodSignature());
            System.out.println("functionalInterface : " + lambda.getFunctionalInterfaceClass());
            System.out.println("methodKind          : " + lambda.getImplMethodKind());
            System.out.println("instantiatedType    : " + lambda.getInstantiatedMethodType());
            System.out.println("capturedArgs.length : " + lambda.getCapturedArgCount());
            for (int i = 0; i < lambda.getCapturedArgCount(); i++) {
                System.out.println("capturedArg[" + i + "]   : " + lambda.getCapturedArg(i));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
