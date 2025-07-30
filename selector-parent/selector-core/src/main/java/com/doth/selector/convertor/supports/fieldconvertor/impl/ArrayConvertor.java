package com.doth.selector.convertor.supports.fieldconvertor.impl;

import com.doth.selector.convertor.supports.fieldconvertor.FieldConvertor;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ArrayConvertor implements FieldConvertor {
    @Override
    public Object convert(ResultSet rs, String columnLabel) throws SQLException {
        Array array = rs.getArray(columnLabel);
        return array != null ? array.getArray() : null;
    }
}
