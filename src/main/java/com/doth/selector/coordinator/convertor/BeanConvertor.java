package com.doth.selector.coordinator.convertor;


import java.sql.ResultSet;


public interface BeanConvertor {

    <T> T convert(ResultSet rs, Class<T> beanClass)
            throws Throwable;
}