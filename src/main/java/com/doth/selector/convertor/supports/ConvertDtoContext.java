package com.doth.selector.convertor.supports;

import com.doth.selector.anno.DependOn;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 上下文工具类：管理 DTO 相关的各类缓存与快速生成 fingerprint。
 */
public final class ConvertDtoContext {

    /** DTO 构造器缓存：beanClass -> Constructor(beanClass(actualClass)) */
    private static final Map<Class<?>, Constructor<?>> DTO_CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();

    /** 构造器 MethodHandle 缓存：Constructor -> MethodHandle */
    private static final Map<Constructor<?>, MethodHandle> CONSTRUCTOR_HANDLE_CACHE = new ConcurrentHashMap<>();

    /** 注解实际类缓存：beanClass -> actualClass */
    private static final Map<Class<?>, Class<?>> DEPENDON_ACTUAL_CACHE = new ConcurrentHashMap<>();

    /** fingerprint 缓存：columnSet -> fingerprintKey (逗号连接排序后的列名) */
    private static final Map<Set<String>, String> FINGERPRINT_CACHE = new ConcurrentHashMap<>();

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private ConvertDtoContext() {
    }

    /**
     * 解析 DTO 类上 @DependOn 注解指定的实体类，
     * 若无注解则返回自身。
     */
    public static Class<?> resolveActualClass(Class<?> beanClass) {
        return DEPENDON_ACTUAL_CACHE.computeIfAbsent(beanClass, bc -> {
            DependOn ann = bc.getAnnotation(DependOn.class);
            if (ann != null) {
                try {
                    return Class.forName(ann.clzPath());
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("无法加载 @DependOn 指定的类: " + ann.clzPath(), e);
                }
            }
            return bc;
        });
    }

    /**
     * 获取 DTO 构造器（参数为实际实体类），并缓存。
     * @param beanClass DTO 类型
     * @param actualClass 对应的实体类型
     */
    @SuppressWarnings("unchecked")
    public static <T> Constructor<T> getDtoConstructor(Class<T> beanClass, Class<?> actualClass) {
        return (Constructor<T>) DTO_CONSTRUCTOR_CACHE.computeIfAbsent(beanClass, bc -> {
            try {
                return bc.getConstructor(actualClass);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(
                    "DTO 构造器未找到: " + bc.getName() + "(" + actualClass.getName() + ")", e);
            }
        });
    }

    /**
     * 获取构造器对应的 MethodHandle，并缓存，用于快速实例化 DTO。
     */
    public static MethodHandle getConstructorHandle(Constructor<?> ctor) {
        return CONSTRUCTOR_HANDLE_CACHE.computeIfAbsent(ctor, c -> {
            try {
                return LOOKUP.unreflectConstructor(c);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("无法获取构造器的 MethodHandle: " + c, e);
            }
        });
    }

    /**
     * 根据列名集合生成 fingerprint（先排序再逗号连接），并缓存结果。
     * 返回的 fingerprint 可直接作为 key 用于快速比对列结构。
     */
    public static String getFingerprint(Set<String> columns) {
        return FINGERPRINT_CACHE.computeIfAbsent(columns, cols -> {
            List<String> sorted = new ArrayList<>(cols);
            Collections.sort(sorted);
            return String.join(",", sorted);
        });
    }
}
