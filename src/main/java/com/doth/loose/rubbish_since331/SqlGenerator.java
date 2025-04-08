package com.doth.loose.rubbish_since331;

import com.doth.stupidrefframe.selector.v1.executor.supports.builder.ConditionBuilder;
import com.doth.stupidrefframe.selector.v1.coordinator.supports.sqlgenerator.builder.SqlBuilder;

import java.util.LinkedHashMap;

import static com.doth.stupidrefframe.selector.v1.util.adapeter.SqlNormalizer.replaceWildcard;
import static com.doth.stupidrefframe.selector.v1.coordinator.supports.sqlgenerator.builder.SqlBuilder.buildFieldList;
import static com.doth.stupidrefframe.selector.v1.util.CamelSnakeConvertUtil.camel2SnakeCase;

/**
 * @project: classFollowing
 * @package: reflect.execrise7sqlgenerate
 * @author: doth
 * @creTime: 2025-03-18  11:11
 * @desc: TODO
 * @v: 1.0
 */
@Deprecated
public class SqlGenerator {


    // ----------------- 生成不带条件的查询 -----------------
    public static String generateSelect(Class<?> clz) {
        return SqlBuilder.buildBaseSelect(clz);
    }

    // ----------------- 生成带条件的查询 -----------------
    public static String generateSelect(Class<?> clz, LinkedHashMap<String, Object> conditions) {
        String baseSql = SqlBuilder.buildBaseSelect(clz);
        return addWhereClause(baseSql, conditions, "");
    }


    public static <T> String generateSelect(Class<T> beanClass, LinkedHashMap<String, Object> condBean, String strClause) {
        String baseSql = SqlBuilder.buildBaseSelect(beanClass);;
        return addWhereClause(baseSql, condBean, strClause);
    }

    // ----------------- 生成以builder为条件的查询 -----------------
    public static <T> String generateSelect(Class<T> beanClass, ConditionBuilder builder) {
        String baseSql = SqlBuilder.buildBaseSelect(beanClass);;
        return baseSql + (builder.getWhereClause().isEmpty() ? "" : builder.getFullSql());
    }

    public static String generateSelect4Raw(Class<?> beanClass, String sql) {
        // 1. 驼峰转下划线
        String snakeSql = camel2SnakeCase(sql, true);
        // 2. 获取白名单字段字符串（复用已有方法）
        String whiteList = buildFieldList(beanClass);
        // 3. 替换*为白名单字段
        return replaceWildcard(snakeSql, whiteList);
    }


    // ----------------- 带自定义子句的全局sql -----------------
    protected static String addWhereClause(String baseSql, LinkedHashMap<String, Object> condBean, String strClause) {
        StringBuilder sb = new StringBuilder(baseSql);

        if (condBean != null && !condBean.isEmpty()) {
            sb.append(" where ");
            // buildConditions(sb, condBean); // 调用共用方法
        }

        if (!strClause.isEmpty()) {
            sb.append(" ").append(strClause.trim());
        }

        return sb.toString();
    }

}
