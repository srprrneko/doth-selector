package com.doth.selector.convertor.supports.fieldconvertor.impl;

import com.doth.selector.convertor.supports.fieldconvertor.FieldConvertor;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClobConvertor implements FieldConvertor {
    @Override
    public Object convert(ResultSet rs, String columnLabel) throws SQLException {
        Clob clob = rs.getClob(columnLabel);
        return clob != null ? clob.getSubString(1, (int) clob.length()) : null;
    }
}
