package com.doth.stupidrefframe_v1.selector.supports.convertor;


import com.doth.stupidrefframe_v1.exception.NoColumnExistException;
import java.sql.ResultSet;

public interface BeanConvertor {
    <T> T convert(ResultSet rs, Class<T> beanClass)
            throws Throwable;
}