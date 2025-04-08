package com.doth.stupidrefframe.selector.v1.coordinator.supports.convertor;


import java.sql.ResultSet;


public interface BeanConvertor {

    <T> T convert(ResultSet rs, Class<T> beanClass)
            throws Throwable;
}