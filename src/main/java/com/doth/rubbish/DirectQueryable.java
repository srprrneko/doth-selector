package com.doth.rubbish;

import com.doth.stupidrefframe_v1.selector.supports.SelectorHelper;

import java.util.LinkedHashMap;
import java.util.List;

@Deprecated
// ----------------- 直接条件查询接口 -----------------
public interface DirectQueryable extends EntityQueryable {

    default <T> List<T> query2Lst(Class<T> beanClass) {
        return getHelper().mapSqlCond(beanClass,(LinkedHashMap<String, Object>) null);
    }

    default <T> T query2T(Class<T> beanClass, LinkedHashMap<String, Object> cond) {
        SelectorHelper helper = getHelper();
        return helper.getSingleResult(helper.mapSqlCond(beanClass, cond));
    }
}