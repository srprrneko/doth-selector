package com.doth.selector.convertor.supports.fieldconvertor.impl;



import com.doth.selector.convertor.supports.fieldconvertor.FieldConvertor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class JSONConvertor implements FieldConvertor {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Object convert(ResultSet rs, String columnLabel) throws SQLException {
        String json = rs.getString(columnLabel);
        try {
            return json != null ? objectMapper.readTree(json) : null;
        } catch (Exception e) {
            throw new SQLException("Invalid JSON format: " + json, e);
        }
    }
}
