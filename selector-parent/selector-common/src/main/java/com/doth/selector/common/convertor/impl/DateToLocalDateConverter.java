package com.doth.selector.common.convertor.impl;

import com.doth.selector.common.convertor.ValueConverter;

import java.sql.Date;
import java.time.LocalDate;

public class DateToLocalDateConverter implements ValueConverter {
    @Override
    public boolean supports(Class<?> fieldType, Object value) {
        return fieldType == LocalDate.class && value instanceof Date;
    }

    @Override
    public Object convert(Object value) {
        return ((Date) value).toLocalDate();
    }
}
