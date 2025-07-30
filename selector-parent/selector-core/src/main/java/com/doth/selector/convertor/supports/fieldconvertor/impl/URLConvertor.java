package com.doth.selector.convertor.supports.fieldconvertor.impl;

import com.doth.selector.convertor.supports.fieldconvertor.FieldConvertor;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;

public class URLConvertor implements FieldConvertor {
    @Override
    public Object convert(ResultSet rs, String columnLabel) throws SQLException {
        String urlStr = rs.getString(columnLabel);
        try {
            return urlStr != null ? new URL(urlStr) : null;
        } catch (MalformedURLException e) {
            throw new SQLException("Malformed URL: " + urlStr, e);
        }
    }
}
