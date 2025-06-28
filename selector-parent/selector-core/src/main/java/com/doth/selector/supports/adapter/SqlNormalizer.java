package com.doth.selector.supports.adapter;

public class SqlNormalizer {


    /**
     * "*" 号替换白名单
     * @param sql sql
     * @param whiteList 查询列表
     * @return 替换好的sql
     */
    public static String replaceWildcard(String sql, String whiteList) {
        return sql.replaceAll("(?i)select\\s+\\*", "select " + whiteList);
    }


}
