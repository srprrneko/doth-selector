package com.doth.selector.common.convertor;

import com.doth.selector.common.convertor.impl.DateToLocalDateConverter;
import com.doth.selector.common.convertor.impl.DateToLocalDateTimeConverter;
import com.doth.selector.common.convertor.impl.TimestampToLocalDateTimeConverter;

import java.util.List;

public class ValueConverterFactory {

    private static final List<ValueConverter> converters = List.of(
        new TimestampToLocalDateTimeConverter(),
        new DateToLocalDateConverter(),
        new DateToLocalDateTimeConverter()
        // todo: ..
    );

    public static Object convertIfPossible(Class<?> fieldType, Object value) {
        for (ValueConverter converter : converters) {
            if (converter.supports(fieldType, value)) {
                return converter.convert(value);
            }
        }
        return value; // 无匹配策略，返回原值
    }
}
