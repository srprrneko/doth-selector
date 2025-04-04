package com.doth.stupidrefframe_v1.selector.v1.supports.convertor;


import java.sql.ResultSet;

public interface BeanConvertor {
    <T> T convert(ResultSet rs, Class<T> beanClass)
            throws Throwable;
}