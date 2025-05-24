package com.doth.selector.common.temp;

public class LbdMeta {
    private final Class<?> clazz;
    private final String fieldName;
    private final Class<?> fieldType;

    public LbdMeta(Class<?> clazz, String fieldName, Class<?> fieldType) {
        this.clazz = clazz;
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Class<?> getFieldType() {
        return fieldType;
    }

    @Override
    public String toString() {
        return clazz.getSimpleName() + ".class, " + fieldName + " : " + fieldType.getSimpleName();
    }
}
