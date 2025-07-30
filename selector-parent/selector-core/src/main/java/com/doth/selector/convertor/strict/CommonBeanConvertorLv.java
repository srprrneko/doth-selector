package com.doth.selector.convertor.strict;


import com.doth.selector.common.exception.mapping.NoColumnExistException;
import com.doth.selector.convertor.BeanConvertor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.doth.selector.common.util.NamingConvertUtil.snake2Camel;

@Deprecated
public class CommonBeanConvertorLv implements BeanConvertor {
    // // 缓存字段映射
    // private static final Map<Class<?>, Map<Integer, FieldMapping>> CLASS_MAPPING_CACHE = new ConcurrentHashMap<>();
    //
    // // 缓存MethodHandle
    // protected static final Map<Field, MethodHandle> SETTER_CACHE = new ConcurrentHashMap<>();

    @Override
    public <T> T convert(ResultSet rs, Class<T> beanClass) throws Throwable {
       return null;
    }


}