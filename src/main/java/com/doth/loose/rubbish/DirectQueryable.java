package com.doth.loose.rubbish;

import com.doth.loose.rubbish_since331.ExecuteCoordinator;

import java.util.LinkedHashMap;
import java.util.List;

@Deprecated
// ----------------- 直接条件查询接口 -----------------
public interface DirectQueryable extends EntityQueryable {

    default <T> List<T> query2Lst(Class<T> beanClass) {
        return getHelper().queryByMap(beanClass,(LinkedHashMap<String, Object>) null);
    }

    default <T> T query2T(Class<T> beanClass, LinkedHashMap<String, Object> cond) {
        ExecuteCoordinator helper = getHelper();
        return helper.getSingleResult(helper.queryByMap(beanClass, cond));
    }
}