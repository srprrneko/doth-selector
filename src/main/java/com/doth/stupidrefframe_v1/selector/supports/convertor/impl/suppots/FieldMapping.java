package com.doth.stupidrefframe_v1.selector.supports.convertor.impl.suppots;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

// 简化字段匹配逻辑
public class FieldMapping {
    public final Field field;
    public final MethodHandle setter;

    public FieldMapping(Field field) throws IllegalAccessException {
        this.field = field;
        this.setter = MethodHandles.lookup().unreflectSetter(field);
    }

    public void setValue(Object target, Object value) throws Throwable {
        setter.invoke(target, value);
    }
}