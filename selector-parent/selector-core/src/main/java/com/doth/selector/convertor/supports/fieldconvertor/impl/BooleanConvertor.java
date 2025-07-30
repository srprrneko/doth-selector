package com.doth.selector.convertor.supports.fieldconvertor.impl;

import com.doth.selector.convertor.supports.fieldconvertor.FieldConvertor;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BooleanConvertor implements FieldConvertor {
    @Override
    public Object convert(ResultSet rs, String columnLabel) throws SQLException {
        Object value = rs.getObject(columnLabel);
        if (value instanceof Boolean) {
            return value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return null;
    }
}
