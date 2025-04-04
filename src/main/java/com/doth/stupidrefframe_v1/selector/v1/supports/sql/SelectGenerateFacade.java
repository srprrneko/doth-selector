package com.doth.stupidrefframe_v1.selector.v1.supports.sql;

import com.doth.stupidrefframe_v1.selector.v1.supports.builder.ConditionBuilder;
import com.doth.stupidrefframe_v1.selector.v1.util.AliasConvertUtil;
import com.doth.stupidrefframe_v1.selector.v1.util.DynamicQueryGenerator;

import java.util.LinkedHashMap;

import static com.doth.stupidrefframe_v1.selector.v1.supports.adapeter.SqlNormalizer.replaceWildcard;
import static com.doth.stupidrefframe_v1.selector.v1.supports.sql.SqlBuilder.appendWhereClause;
import static com.doth.stupidrefframe_v1.selector.v1.util.CamelSnakeConvertUtil.camel2SnakeCase;
import static com.doth.stupidrefframe_v1.selector.v1.supports.sql.SqlBuilder.buildFieldList;

/**
 * @project: classFollowing
 * @package: reflect.execrise7sqlgenerate
 * @author: doth
 * @creTime: 2025-03-18  11:11
 * @desc: TODO
 * @v: 1.0
 */
public class SelectGenerateFacade {


    // ----------------- 生成不带条件的查询 -----------------
    public static String generate(Class<?> clz) {
        return SqlBuilder.buildBaseSelect(clz);
    }

    // ----------------- 生成带条件的查询 -----------------
    public static String generate4map(Class<?> clz, LinkedHashMap<String, Object> conditions) {
        String baseSql = SqlBuilder.buildBaseSelect(clz);
        return appendWhereClause(baseSql, conditions, "");
    }

    // ----------------- 生成以builder为条件的查询 -----------------
    public static <T> String generate4mapVzClause(Class<T> beanClass, LinkedHashMap<String, Object> condBean, String strClause) {
        String baseSql = SqlBuilder.buildBaseSelect(beanClass);;
        return appendWhereClause(baseSql, condBean, strClause);
    }

    // ----------------- 生成以builder为条件的查询 -----------------
    public static <T> String generate4builder(Class<T> beanClass, ConditionBuilder builder) {
        String baseSql = SqlBuilder.buildBaseSelect(beanClass);
        return baseSql + (builder.getWhereClause().isEmpty() ? "" : builder.getFullSql());
    }

    // ----------------- 生成原生的查询 -----------------
    public static String cvn4raw(Class<?> beanClass, String rawSql) {
        String whiteList = buildFieldList(beanClass); // *号转换成白名单
        return replaceWildcard(rawSql, whiteList);  // 白名单替换
    }

    // ----------------- 生成原生的连接查询 -----------------
    public static String cvn4joinRaw(String sql, boolean autoAlias) {
        return autoAlias ? AliasConvertUtil.generateAliases(sql) : sql; // 是否开启自动别名? 生成别名 : 原始sql
    }



    ////////////////// 新增方法: 动态生成嵌套实体结构的sql //////////////////
    public static String generateJoin4map(Class<?> clz, LinkedHashMap<String, Object> conditions) {
        String baseSql = DynamicQueryGenerator.generated(clz);
        System.out.println("baseSql = " + baseSql);
        String finalSql = AliasConvertUtil.generateAliases(baseSql);
        System.out.println("finalSql = " + finalSql);
        return appendWhereClause(finalSql, conditions, "");
    }
}
