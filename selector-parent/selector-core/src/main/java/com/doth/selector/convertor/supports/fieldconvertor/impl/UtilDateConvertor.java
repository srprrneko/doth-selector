package com.doth.selector.convertor.supports.fieldconvertor.impl;

import com.doth.selector.convertor.supports.fieldconvertor.FieldConvertor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class UtilDateConvertor implements FieldConvertor {
    @Override
    public Object convert(ResultSet rs, String columnLabel) throws SQLException {
        Object val = rs.getObject(columnLabel);
        if (val instanceof Timestamp) {
            return new Date(((Timestamp) val).getTime());
        } else if (val instanceof LocalDateTime) {
            return Date.from(((LocalDateTime) val).atZone(ZoneId.systemDefault()).toInstant());
        } else if (val instanceof Date) {
            return val;
        }
        return null;
    }
}
