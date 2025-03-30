package com.doth.stupidrefframe_v1.selector.supports.sql;

import com.doth.stupidrefframe_v1.selector.supports.builder.ConditionBuilder;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;

/**
 * @project: classFollowing
 * @package: reflect.execrise7sqlgenerate
 * @author: doth
 * @creTime: 2025-03-18  11:11
 * @desc: TODO
 * @v: 1.0
 */
public class SqlGenerator extends GeneratorHelper{
    // todo : 后续再考虑组合模式
    // private GeneratorHelper helper;


    // ----------------- 生成不带条件的查询 -----------------
    public static String generateSelect(Class<?> clz) {
        return getBaseSql(clz);
    }

    // ----------------- 生成带条件的查询 -----------------
    public static String generateSelect(Class<?> clz, LinkedHashMap<String, Object> conditions) {
        String baseSql = getBaseSql(clz);
        return globalSelect(baseSql, conditions);
    }

    // ----------------- 生成以builder为条件的查询 -----------------
    public static <T> String generateSelect(Class<T> beanClass, ConditionBuilder builder) {
        String baseSql = getBaseSql(beanClass);
        return baseSql + (builder.getWhereClause().isEmpty() ? "" : builder.getFullSql());
    }

    public static <T> String generateSelect(Class<T> beanClass, LinkedHashMap<String, Object> condBean, String strClause) {
        String baseSql = getBaseSql(beanClass);
        return globalSelect(baseSql, condBean, strClause);
    }



    @Deprecated
    // ----------------- 插入语句生成 -----------------
    public static String generateInsert(Class<?> clz) {
        // UserInfoMessageBro = user_info_message_bro
        String tableName = entityNameCvn2Snake(clz);

        sb = new StringBuilder("insert into ").append(tableName).append(" (");

        // 获取所有字段
        Field[] fields = clz.getDeclaredFields();
        int fieldsSize = fields.length;

        // 拼接字段名
        for (int i = 0; i < fieldsSize; i++) {
            Field field = fields[i];
            sb.append(field.getName());
            if (i < fieldsSize - 1) {
                sb.append(", ");
            }
        }
        sb.append(") values (");

        // 拼接占位符
        for (int i = 0; i < fieldsSize; i++) {
            sb.append("?");
            if (i < fieldsSize - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");

        return sb.toString();
    }



}
