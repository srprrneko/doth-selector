package com.doth.selector.convertor.supports.fieldconvertor;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface FieldConvertor {
    Object convert(ResultSet rs, String columnLabel) throws SQLException;
}
