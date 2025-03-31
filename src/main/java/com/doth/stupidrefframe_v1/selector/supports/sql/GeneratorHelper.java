package com.doth.stupidrefframe_v1.selector.supports.sql;


import static com.doth.stupidrefframe_v1.selector.util.NamingConverter.camel2SnakeCase;

/**
 * @project: test01
 * @package: com.example.inserttry.newdbutiltry01
 * @author: doth
 * @creTime: 2025-03-23  19:29
 * @desc: sql映射最外层的类
 * @v: 1.0
 */
public class GeneratorHelper {
    protected static StringBuilder sb;
    protected static String tableName;



    // // ----------------- 不带自定义子句的全局sql -----------------
    // protected static String globalSelect(String baseSql, LinkedHashMap<String, Object> conditions) {
    //     StringBuilder sb = new StringBuilder(baseSql);
    //
    //     if (conditions != null && !conditions.isEmpty()) {
    //         sb.append(" where ");
    //         buildConditions(sb, conditions); // 调用共用方法
    //     }
    //     return sb.toString();
    // }
    //
    // // ----------------- 带自定义子句的全局sql -----------------
    // protected static String globalSelect(String baseSql, LinkedHashMap<String, Object> condBean, String strClause) {
    //     StringBuilder sb = new StringBuilder(baseSql);
    //
    //     if (condBean != null && !condBean.isEmpty()) {
    //         sb.append(" where ");
    //         buildConditions(sb, condBean); // 调用共用方法
    //     }
    //
    //     if (!strClause.isEmpty()) {
    //         sb.append(" ").append(strClause.trim());
    //     }
    //
    //     return sb.toString();
    // }
    // // ----------------- 字段列表(c1,c2...)构建方法 -----------------
    // // 协调
    // public static String normalizeSql4Raw(Class<?> beanClass, String sql) {
    //     // 1. 驼峰转下划线
    //     String snakeSql = camel2SnakeCase(sql, true);
    //     // 2. 获取白名单字段字符串（复用已有方法）
    //     String whiteList = buildFieldList(beanClass);
    //     // 3. 替换*为白名单字段
    //     return replaceWildcard(snakeSql, whiteList);
    // }
}
