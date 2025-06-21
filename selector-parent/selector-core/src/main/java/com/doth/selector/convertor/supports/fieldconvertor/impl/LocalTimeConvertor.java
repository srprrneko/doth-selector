package com.doth.selector.convertor.supports.fieldconvertor.impl;

import com.doth.selector.convertor.supports.fieldconvertor.FieldConvertor;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;

@Slf4j
public class LocalTimeConvertor implements FieldConvertor {
    @Override
    public Object convert(ResultSet rs, String columnLabel) throws SQLException {
        Time time = null;
        try {
            time = rs.getTime(columnLabel);
            return time != null ? time.toLocalTime() : null;
        } catch (SQLException e) {
            log.error("类型转换出现异常! {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
