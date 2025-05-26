package com.doth.selector.common.convertor.impl;

import com.doth.selector.common.convertor.ValueConverter;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class TimestampToLocalDateTimeConverter implements ValueConverter {
    @Override
    public boolean supports(Class<?> fieldType, Object value) {
        return fieldType == LocalDateTime.class && value instanceof Timestamp;
    }

    @Override
    public Object convert(Object value) {
        return ((Timestamp) value).toLocalDateTime();
    }
}
