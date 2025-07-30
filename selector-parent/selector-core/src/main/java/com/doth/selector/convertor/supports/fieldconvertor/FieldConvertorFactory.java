package com.doth.selector.convertor.supports.fieldconvertor;

import com.doth.selector.convertor.supports.fieldconvertor.impl.*;
import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URL;
import java.time.*;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FieldConvertorFactory {
    private static final Map<Class<?>, FieldConvertor> convertors = new ConcurrentHashMap<>();

    static {
        convertors.put(LocalDate.class, new LocalDateConvertor());
        convertors.put(LocalDateTime.class, new LocalDateTimeConvertor());
        convertors.put(LocalTime.class, new LocalTimeConvertor());
        convertors.put(Year.class, new YearConvertor());

        convertors.put(BigDecimal.class, new BigDecimalConvertor());
        convertors.put(Double.class, (rs, col) -> {
            BigDecimal decimal = rs.getBigDecimal(col);
            return decimal != null ? decimal.doubleValue() : null;
        });

        convertors.put(Boolean.class, new BooleanConvertor());
        convertors.put(UUID.class, new UUIDConvertor());
        convertors.put(URL.class, new URLConvertor());
        convertors.put(JsonNode.class, new JSONConvertor());
        convertors.put(byte[].class, new BlobConvertor());
        convertors.put(String.class, new ClobConvertor()); // 特殊情况下
        convertors.put(Object[].class, new ArrayConvertor());

        // 默认策略
        convertors.put(Object.class, new DefaultConvertor());
    }

    @SuppressWarnings("unchecked")
    public static FieldConvertor getConvertor(Class<?> type, Field field) {
        if (type.isEnum()) {
            return new EnumConvertor((Class<? extends Enum>) type);
        }
        return convertors.getOrDefault(type, convertors.get(Object.class));
    }
}
