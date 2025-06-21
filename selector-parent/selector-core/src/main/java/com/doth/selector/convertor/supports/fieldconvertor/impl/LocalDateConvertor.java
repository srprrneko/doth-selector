package com.doth.selector.convertor.supports.fieldconvertor.impl;

import com.doth.selector.convertor.supports.fieldconvertor.FieldConvertor;
import lombok.extern.slf4j.Slf4j;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
public class LocalDateConvertor implements FieldConvertor {
    @Override
    public Object convert(ResultSet rs, String columnLabel) throws SQLException {
        Date date = null;
        try {
            date = rs.getDate(columnLabel);
            return date != null ? date.toLocalDate() : null;
        } catch (SQLException e) {
            log.error("类型转换出现异常! {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
