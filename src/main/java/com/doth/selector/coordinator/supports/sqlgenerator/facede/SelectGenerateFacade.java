package com.doth.selector.coordinator.supports.sqlgenerator.facede;

import com.doth.selector.coordinator.supports.sqlgenerator.tool.AliasConvertUtil;
import com.doth.selector.coordinator.supports.sqlgenerator.tool.AutoQueryGenerator;
import com.doth.selector.coordinator.supports.sqlgenerator.builder.SqlBuilder;
import com.doth.selector.executor.supports.builder.ConditionBuilder;

import java.util.LinkedHashMap;

import static com.doth.selector.common.util.adapeter.SqlNormalizer.replaceWildcard;
import static com.doth.selector.coordinator.supports.sqlgenerator.builder.SqlBuilder.buildWhereClause;
import static com.doth.selector.common.util.CamelSnakeConvertUtil.camel2SnakeCase;
import static com.doth.selector.coordinator.supports.sqlgenerator.builder.SqlBuilder.buildFieldList;

/**
 * @project: classFollowing
 * @package: reflect.execrise7sqlgenerate
 * @author: doth
 * @creTime: 2025-03-18  11:11
 * @desc: 待优化
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
        return buildWhereClause(baseSql, conditions, "");
    }

    // ----------------- 生成以builder为条件的查询 -----------------
    public static <T> String generate4mapVzClause(Class<T> beanClass, LinkedHashMap<String, Object> condBean, String strClause) {
        String baseSql = SqlBuilder.buildBaseSelect(beanClass);;
        return buildWhereClause(baseSql, condBean, strClause);
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
    public static String cvn4joinRaw(String sql) {
        return AliasConvertUtil.generateAliases(sql); // 是否开启自动别名? 生成别名 : 原始sql
    }

    @Deprecated(since = "3.0", forRemoval = true) // 强制要求开发者不能起别名, 因此删除, 如果非要起, 可能的考虑list<map>: dto的方式去实现
    public static String cvn4joinRaw(String sql, boolean isAutoAlias) {
        return isAutoAlias ? AliasConvertUtil.generateAliases(sql) : sql; // 是否开启自动别名? 生成别名 : 原始sql
    }

    // ----------------- 生成原生的连接查询 -----------------
    public static String cvn4joinBuilderVzRaw(String sql, ConditionBuilder builder) {
        String baseSql = AliasConvertUtil.generateAliases(sql);
        return baseSql + builder.getFullSql(); // 是否开启自动别名? 生成别名 : 原始sql
    }



    ////////////////// 新增方法: 动态生成嵌套实体结构的sql //////////////////
    // ----------------- 生成原生的连接查询 -----------------
    public static String generateJoin4map(Class<?> clz, LinkedHashMap<String, Object> conditions) {
        // 生成联查sql
        long start = System.currentTimeMillis();
        String baseSql = AutoQueryGenerator.generated(clz);
        long end = System.currentTimeMillis();
        System.out.println("\"生成联查sql\" = " + (end - start));

        long start1 = System.currentTimeMillis();
        // 起别名耗时
        String finalSql = AliasConvertUtil.generateAliases(baseSql);
        long end1 = System.currentTimeMillis();
        System.out.println("\"起别名耗时\" = " + (end1 - start1));
        return buildWhereClause(finalSql, conditions, "");
    }

    public static <T> String generateJoin4mapVzClause(Class<T> beanClass, LinkedHashMap<String, Object> condBean, String strClause) {
        String baseSql = AutoQueryGenerator.generated(beanClass);
        String finalSql = AliasConvertUtil.generateAliases(baseSql);
        return buildWhereClause(finalSql, condBean, strClause);
    }

    public static <T> String generateJoin4builder(Class<T> beanClass, ConditionBuilder builder) {
        // 生成联查sql
        long start = System.currentTimeMillis();
        String baseSql = AutoQueryGenerator.generated(beanClass);
        long end = System.currentTimeMillis();
        System.out.println("\"生成联查sql\" = " + (end - start));

        long start1 = System.currentTimeMillis();
        // 起别名耗时
        String finalSql = AliasConvertUtil.generateAliases(baseSql);
        long end1 = System.currentTimeMillis();
        System.out.println("\"起别名耗时\" = " + (end1 - start1));

        return finalSql + (builder.getWhereClause().isEmpty() ? "" : builder.getFullSql());
    }
}
