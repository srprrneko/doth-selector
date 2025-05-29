package com.doth.selector.dto;

import com.doth.selector.anno.UseDTO;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具类：解析当前调用方法上是否使用了 @UseDTO
 */
public class DtoStackResolver {

    private static final Map<String, String> DTO_METHOD_CACHE = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> CURRENT_DTO_ID = new ThreadLocal<>();
    private static final Map<String, Set<String>> dtoFieldPathCache = new ConcurrentHashMap<>();

    /**
     * 应该由第一次进入时调用
     */
    public static String resolveDTOIdFromStack() {
        if (CURRENT_DTO_ID.get() != null) return CURRENT_DTO_ID.get();

        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stack) {
            String key = element.getClassName() + "#" + element.getMethodName();
            if (DTO_METHOD_CACHE.containsKey(key)) {
                CURRENT_DTO_ID.set(DTO_METHOD_CACHE.get(key));
                return CURRENT_DTO_ID.get();
            }

            try {
                Class<?> clazz = Class.forName(element.getClassName());
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.getName().equals(element.getMethodName()) && method.isAnnotationPresent(UseDTO.class)) {
                        String id = method.getAnnotation(UseDTO.class).id();
                        DTO_METHOD_CACHE.put(key, id);
                        CURRENT_DTO_ID.set(id);
                        return id;
                    }
                }
            } catch (Exception ignored) {}
        }
        CURRENT_DTO_ID.set(null);
        return null;
    }

    /**
     * 任意位置调用获取当前 DTO id（不会重新解析栈）
     */
    public static String getCurrentDTOId() {
        return CURRENT_DTO_ID.get();
    }

    /**
     * 清除上下文，防止泄漏
     */
    public static void clear() {
        CURRENT_DTO_ID.remove();
    }
}
