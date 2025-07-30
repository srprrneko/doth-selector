package com.doth.selector.convertor.supports.fieldconvertor.impl;

import com.doth.selector.convertor.supports.fieldconvertor.FieldConvertor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Year;

public class YearConvertor implements FieldConvertor {
    @Override
    public Object convert(ResultSet rs, String columnLabel) throws SQLException {
        int year = rs.getInt(columnLabel);
        return year != 0 ? Year.of(year) : null;
    }
}
