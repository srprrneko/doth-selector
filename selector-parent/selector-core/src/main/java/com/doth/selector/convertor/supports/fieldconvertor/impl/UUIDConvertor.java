package com.doth.selector.convertor.supports.fieldconvertor.impl;

import com.doth.selector.convertor.supports.fieldconvertor.FieldConvertor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class UUIDConvertor implements FieldConvertor {
    @Override
    public Object convert(ResultSet rs, String columnLabel) throws SQLException {
        String value = rs.getString(columnLabel);
        return value != null ? UUID.fromString(value) : null;
    }
}
