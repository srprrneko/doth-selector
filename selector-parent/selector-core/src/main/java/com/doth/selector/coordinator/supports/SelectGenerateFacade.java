package com.doth.selector.coordinator.supports;

import com.doth.selector.coordinator.supports.sql.tool.AliasAppender;
import com.doth.selector.coordinator.supports.sql.tool.AutoQueryGenerator;
import com.doth.selector.coordinator.supports.sql.builder.SqlBuilder;
import com.doth.selector.executor.supports.builder.ConditionBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;

import static com.doth.selector.supports.adapter.SqlNormalizer.replaceWildcard;
import static com.doth.selector.coordinator.supports.sql.builder.SqlBuilder.buildWhereClause;
import static com.doth.selector.coordinator.supports.sql.builder.SqlBuilder.buildFieldList;

@Slf4j
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
    public static <T> String generate4builder(Class<T> beanClass, ConditionBuilder<T> builder) {
        String baseSql = SqlBuilder.buildBaseSelect(beanClass);
        return baseSql + (builder.getWhereClause().isEmpty() ? "" : builder.getFullCause());
    }

    // ----------------- 生成原生的查询 -----------------
    public static String cvn4raw(Class<?> beanClass, String rawSql) {
        String whiteList = buildFieldList(beanClass); // *号转换成白名单
        return replaceWildcard(rawSql, whiteList);  // 白名单替换
    }

    // ----------------- 生成原生的连接查询 -----------------
    public static String cvn4joinRaw(String sql) {
        return AliasAppender.generateAliases(sql); // 是否开启自动别名? 生成别名 : 原始sql
    }

    @Deprecated(since = "3.0", forRemoval = true) // 强制要求开发者不能起别名, 因此删除, 如果非要起, 可能的考虑list<map>: dto的方式去实现
    public static String cvn4joinRaw(String sql, boolean isAutoAlias) {
        return isAutoAlias ? AliasAppender.generateAliases(sql) : sql; // 是否开启自动别名? 生成别名 : 原始sql
    }

    // ----------------- 生成原生的连接查询 -----------------
    @Deprecated
    public static String cvn4joinBuilderVzRaw(String sql, ConditionBuilder builder) {
        String baseSql = AliasAppender.generateAliases(sql);
        return baseSql + builder.getFullCause(); // 是否开启自动别名? 生成别名 : 原始sql
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
        String finalSql = AliasAppender.generateAliases(baseSql);
        long end1 = System.currentTimeMillis();
        System.out.println("\"起别名耗时\" = " + (end1 - start1));
        return buildWhereClause(finalSql, conditions, "");
    }

    @Deprecated
    public static <T> String generateJoin4mapVzClause(Class<T> beanClass, LinkedHashMap<String, Object> condBean, String strClause) {
        String baseSql = AutoQueryGenerator.generated(beanClass);
        String finalSql = AliasAppender.generateAliases(baseSql);
        return buildWhereClause(finalSql, condBean, strClause);
    }

    public static <T> String generateJoin4builder(Class<T> beanClass, ConditionBuilder<T> builder) {
        long start = System.currentTimeMillis();
        String baseSql = AutoQueryGenerator.generated(beanClass, builder);
        long end = System.currentTimeMillis();
        log.info("sql generated time cost: {}", end - start);

        long start1 = System.currentTimeMillis();
        String finalSql = AliasAppender.generateAliases(baseSql);
        long end1 = System.currentTimeMillis();
        log.info("sql alias replaced time cost: " + (end1 - start1));

        return finalSql + (builder.getWhereClause().isEmpty() ? "" : builder.getFullCause());
    }
}
