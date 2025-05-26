package com.doth.selector.common.convertor.impl;

import com.doth.selector.common.convertor.ValueConverter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class DateToLocalDateTimeConverter implements ValueConverter {
    @Override
    public boolean supports(Class<?> fieldType, Object value) {
        return fieldType == LocalDateTime.class && value instanceof Date;
    }

    @Override
    public Object convert(Object value) {
        return ((Date) value).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}

