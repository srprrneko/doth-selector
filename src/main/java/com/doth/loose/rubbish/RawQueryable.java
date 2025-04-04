package com.doth.loose.rubbish;

import java.util.List;

@Deprecated
// ----------------- 原生SQL专属接口 -----------------
public interface RawQueryable extends EntityQueryable {


    default <T> List<T> query2Lst(Class<T> beanClass, String sql,  Object... params) {
        return getHelper().queryByRaw(beanClass, sql, params);
    }

    default <T> List<T> query2Lst4Join(Class<T> beanClass, String sql, boolean isAutoAlias, Object... params) {
        return getHelper().queryByJoinRaw(beanClass, sql, isAutoAlias, params);
    }



    default <T> T query2T(Class<T> beanClass, String sql, Object... params) {
        List<T> list = query2Lst(beanClass, sql, params);
        return getHelper().getSingleResult(list);
    }

    default <T> T query2T4Join(Class<T> beanClass, String sql, boolean isAutoAlias, Object... params) {
        List<T> list = query2Lst4Join(beanClass, sql, isAutoAlias, params);
        return getHelper().getSingleResult(list);
    }


}
