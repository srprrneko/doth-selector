package com.doth.selector.convertor.supports.fieldconvertor.impl;

import com.doth.selector.convertor.supports.fieldconvertor.FieldConvertor;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BigDecimalConvertor implements FieldConvertor {
    @Override
    public Object convert(ResultSet rs, String columnLabel) throws SQLException {
        BigDecimal decimal = rs.getBigDecimal(columnLabel);
        return decimal;
    }
}
