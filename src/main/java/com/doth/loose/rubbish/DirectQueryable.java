package com.doth.loose.rubbish;

import com.doth.stupidrefframe_v1.selector.core.coordinator.QueryCoordinator_v1;

import java.util.LinkedHashMap;
import java.util.List;

@Deprecated
// ----------------- 直接条件查询接口 -----------------
public interface DirectQueryable extends EntityQueryable {

    default <T> List<T> query2Lst(Class<T> beanClass) {
        return getHelper().mapSqlCond(beanClass,(LinkedHashMap<String, Object>) null);
    }

    default <T> T query2T(Class<T> beanClass, LinkedHashMap<String, Object> cond) {
        QueryCoordinator_v1 helper = getHelper();
        return helper.getSingleResult(helper.mapSqlCond(beanClass, cond));
    }
}