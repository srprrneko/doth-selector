package com.doth.stupidrefframe_v1.selector.supports.convertor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

// 简化字段匹配逻辑
public class FieldMapping {
    final Field field;
    final MethodHandle setter;

    FieldMapping(Field field) throws IllegalAccessException {
        this.field = field;
        this.setter = MethodHandles.lookup().unreflectSetter(field);
    }

    void setValue(Object target, Object value) throws Throwable {
        setter.invoke(target, value);
    }
}