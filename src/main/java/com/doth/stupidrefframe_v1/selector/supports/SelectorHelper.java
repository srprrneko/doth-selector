package com.doth.stupidrefframe_v1.selector.supports;


import com.doth.stupidrefframe_v1.selector.supports.builder.ConditionBuilder;
import com.doth.stupidrefframe_v1.selector.supports.convertor.BeanConvertor;
import com.doth.stupidrefframe_v1.selector.supports.convertor.BeanConvertorFactory;
import com.doth.stupidrefframe_v1.exception.NoColumnExistException;
import com.doth.stupidrefframe_v1.exception.NonUniqueResultException;
import com.doth.stupidrefframe_v1.selector.supports.convertor.ConvertorType;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class SelectorHelper {
    // ------------------ 结果集映射 ------------------
    public <T> List<T> mapResultSet(Class<T> beanClass, String sql, Object[] params) {
        System.out.println(sql);
        try (ResultSet rs = BaseDruidUtil.executeQuery(sql, params)) {
            // 使用默认bean转换器
            BeanConvertor convertor = BeanConvertorFactory.getConvertor(ConvertorType.STRICT);
            List<T> result = new ArrayList<>();
            while (rs.next()) {
                result.add(convertor.convert(rs, beanClass));
            }
            return result;
        } catch (NoColumnExistException e) {
            throw new RuntimeException("列映射失败: " + e.getMessage(), e);
        } catch (SQLException e) {
            throw new RuntimeException("数据库错误: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("查询失败: " + e.getMessage(), e);
        } catch (Throwable e) {
            throw new RuntimeException("不太清楚的异常");
        }
    }




    // ------------------ 提取非空字段 ------------------
    public <T> LinkedHashMap<String, Object> extractNonNullFields(T entity) {
        LinkedHashMap<String, Object> condMap = new LinkedHashMap<>();
        if (entity == null) return condMap;

        for (Field field : entity.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object value = field.get(entity);
                if (value != null) {
                    condMap.put(field.getName(), value);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("字段提取失败: " + field.getName(), e);
            }
        }
        return condMap;
    }


    // ------------------ 内部通用结果提取方法 ------------------
    public <T> T getSingleResult(List<T> list) {
        if (list.size() > 1) {
            throw new NonUniqueResultException("查询返回了 " + list.size() + " 条结果");
        }
        return list.isEmpty() ? null : list.get(0);
    }


    // ------------------ sql 执行以及映射, 以map 为条件 ------------------
    public <T> List<T> mapSqlCond(Class<T> beanClass, LinkedHashMap<String, Object> cond) {
        String sql = SqlGenerator.generateSelect(beanClass, cond);
        Object[] params = buildParams(cond);
        return mapResultSet(beanClass, sql, params);
    }
    // ------------------ sql 执行以及映射, 以builder 为条件 ------------------
    public <T> List<T> mapSqlCond(Class<T> beanClass, ConditionBuilder builder) {
        String sql = SqlGenerator.generateSelect(beanClass, builder);
        Object[] params = builder.getParams();
        return mapResultSet(beanClass, sql, params);
    }
    // ------------------ sql 执行以及映射, 以map 键值为条件, 并且拼接字符串条件 ------------------
    public <T> List<T> mapSqlCond(Class<T> beanClass, LinkedHashMap<String, Object> cond, String strClause) {
        String sql = SqlGenerator.generateSelect(beanClass, cond, strClause);
        Object[] params =  buildParams(cond);
        return mapResultSet(beanClass, sql, params);
    }
    public <T> List<T> mapSqlCond(Class<T> beanClass, String sql, Object... params) {
        String normalSql = SqlGenerator.normalizeSql4Raw(beanClass, sql); // 仅仅只是转换sql规范
        System.out.println("normalSql = " + normalSql);
        return mapResultSet(beanClass, normalSql, params);
    }
    public <T> List<T> mapSqlCond(Class<T> beanClass, String sql, ConditionBuilder builder) {
        sql = sql + builder.getFullSql();
        System.out.println("sql = " + sql);
        return mapResultSet(beanClass, sql, builder.getParams());
    }

    // ------------------ 工具方法：结果集映射抽取出来的 行转实体方法 ------------------
    // private <T> T row2Bean(Class<T> beanClass, ResultSet rs, ResultSetMetaData metaData, int columnCount) throws Exception, NoColumnExistException {
    //     // 特殊处理 Map 类型
    //     if (beanClass == Map.class) {
    //         Map<String, Object> map = new LinkedHashMap<>();
    //         for (int i = 1; i <= columnCount; i++) {
    //             String columnName = metaData.getColumnLabel(i);
    //             Object value = rs.getObject(i);
    //             map.put(columnName, value);
    //         }
    //         return (T) map;
    //     }
    //
    //     // 两种映射模式
    //     // 预处理：列名转字段名并缓存 Field 对象, 键值绑定情况下不需要确保顺序所以使用map
    //     Map<Integer, Field> columnIndexToField = new HashMap<>();
    //
    //     // 生成字段名双模式缓存（关键点）
    //     Map<String, Field> fieldMap = new HashMap<>();
    //     for (Field field : beanClass.getDeclaredFields()) {
    //         field.setAccessible(true);
    //         // 原始字段名（兼容显式别名）
    //         fieldMap.put(field.getName().toLowerCase(), field);
    //         // 蛇形转换字段名（兼容原有规范）
    //         String snakeName = SqlGenerator.camel2SnakeCase(field.getName());
    //         fieldMap.put(snakeName.toLowerCase(), field);
    //     }
    //
    //     for (int i = 1; i <= columnCount; i++) {
    //         String columnName = metaData.getColumnLabel(i);
    //
    //         // 双模式匹配（优先直接匹配，其次规范转换）
    //         Field field = fieldMap.get(columnName.toLowerCase()); // 直接匹配
    //         if (field == null) {
    //             String camelName = SqlGenerator.snake2CamelCase(columnName);
    //             field = fieldMap.get(camelName.toLowerCase()); // 规范转换匹配
    //         }
    //
    //         if (field == null) {
    //             throw new NoColumnExistException("列 " + columnName + " 无法映射到 " + beanClass.getName());
    //         }
    //         columnIndexToField.put(i, field);
    //     }
    //
    //     // 创建对象并填充字段值
    //     T bean = beanClass.getDeclaredConstructor().newInstance();
    //     // 修改点：添加严格类型校验
    //     for (Map.Entry<Integer, Field> entry : columnIndexToField.entrySet()) {
    //         int columnIndex = entry.getKey();
    //         Field field = entry.getValue();
    //         Object value = rs.getObject(columnIndex);
    //
    //         // 类型检查逻辑（新增核心代码）
    //         if (value != null) {
    //             Class<?> fieldType = field.getType();
    //             Class<?> valueType = value.getClass();
    //
    //             // 严格类型校验
    //             if (!fieldType.isAssignableFrom(valueType)) {
    //                 String errMsg = String.format(
    //                         "字段类型不匹配! 实体类: %s.%s (字段类型: %s), 数据库类型: %s",
    //                         beanClass.getSimpleName(),
    //                         field.getName(),
    //                         fieldType.getSimpleName(),
    //                         valueType.getSimpleName()
    //                 );
    //                 throw new IllegalArgumentException(errMsg);
    //             }
    //         }
    //
    //         field.set(bean, value);
    //     }
    //     return bean;
    // }

    //######################## 私有区域 #########################
    // ------------------ 工具方法：map正确转换object[]的方式 ------------------
    private Object[] buildParams(LinkedHashMap<String, Object> condBean) {
        List<Object> params = new ArrayList<>();
        if (condBean != null) {
            for (Map.Entry<String, Object> entry : condBean.entrySet()) {
                Object value = entry.getValue();

                // 展开集合为多个参数
                if (value instanceof Collection) { // 如果是集合
                    params.addAll((Collection<?>) value); // 将集合中的元素整块添加到参数列表中
                } else {
                    params.add(value); // 简单添加
                }
            }
        }
        return params.toArray(); // 直接返回数组
    }


}