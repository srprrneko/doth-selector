package com.doth.selector.convertor.supports.fieldconvertor.impl;

import com.doth.selector.convertor.supports.fieldconvertor.FieldConvertor;

import java.sql.ResultSet;
import java.sql.SQLException;

public class EnumConvertor implements FieldConvertor {
    private final Class<? extends Enum> enumClass;

    public EnumConvertor(Class<? extends Enum> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public Object convert(ResultSet rs, String columnLabel) throws SQLException {
        String value = rs.getString(columnLabel);
        return value != null ? Enum.valueOf(enumClass, value) : null;
    }
}
