package com.doth.rubbish;

import java.util.List;

@Deprecated
// ----------------- 原生SQL专属接口 -----------------
public interface RawQueryable extends EntityQueryable {
    default <T> List<T> query2Lst(Class<T> beanClass, String sql,  Object... params) {
        return getHelper().mapSqlCond(beanClass, sql, params);
    }

    default <T> T query2T(Class<T> beanClass, String sql, Object... params) {
        List<T> list = query2Lst(beanClass, sql, params);
        return getHelper().getSingleResult(list);
    }
}
