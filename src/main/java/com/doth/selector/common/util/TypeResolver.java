package com.doth.selector.common.util;

/**
 * @author 贤
 * @version 1.0
 * @date 2025/5/24 19:26
 * @description java类型的处理器
 */
public class TypeResolver {

    public static Object getDefaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        return null;
    }

}