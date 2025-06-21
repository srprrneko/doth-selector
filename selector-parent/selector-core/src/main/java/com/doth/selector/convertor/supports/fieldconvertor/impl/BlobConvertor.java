package com.doth.selector.convertor.supports.fieldconvertor.impl;

import com.doth.selector.convertor.supports.fieldconvertor.FieldConvertor;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BlobConvertor implements FieldConvertor {
    @Override
    public Object convert(ResultSet rs, String columnLabel) throws SQLException {
        Blob blob = rs.getBlob(columnLabel);
        return blob != null ? blob.getBytes(1, (int) blob.length()) : null;
    }
}
