package com.doth.stupidrefframe_v1.selector.v1.supports.adapeter;

/**
 * @project: test02
 * @package: com.doth.stupidrefframe_v1.selector.v1.supports.adapeter
 * @author: doth
 * @creTime: 2025-04-02  01:32
 * @desc: todo
 * @v: 1.0
 */
public class SqlNormalizer {


    // ----------------- "*" 号替换白名单 -----------------
    public static String replaceWildcard(String sql, String whiteList) {
        return sql.replaceAll("(?i)select\\s+\\*", "select " + whiteList);
    }


}
