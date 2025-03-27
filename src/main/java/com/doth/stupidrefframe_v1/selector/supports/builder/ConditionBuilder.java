// 保持原始包路径不变
package com.doth.stupidrefframe_v1.selector.supports.builder;


import java.util.*;


/**
 * SQL条件构建器，用于动态生成WHERE子句和分页查询
 * 使用示例：new ConditionBuilder().eq("name", "Alice").page("id", 1000, 20).getFullSql()
 */
public class ConditionBuilder {
    // where子句构建器
    // 使用StringBuilder处理频繁字符串拼接操作，相比String直接拼接更高效（避免多次创建String对象）
    private final StringBuilder whereClause = new StringBuilder();

    // sql参数集合
    // 使用ArrayList存储参数，提供快速随机访问和高效的空间增长策略（默认扩容50%）
    private final List<Object> params = new ArrayList<>();


    // 分页处理器（组合模式） 隔离分页逻辑
    private final BuilderPage pagination = new BuilderPage();

    /**
     * 等于条件（eq = Equal）
     * @param field 字段名
     * @param value 比较值
     */
    public ConditionBuilder eq(String field, Object value) {
        appendCondition(field + " = ?", value);
        return this;
    }

    /**
     * 大于条件（gt = Greater Than）
     * @param field 字段名
     * @param value 比较值
     */
    public ConditionBuilder gt(String field, Object value) {
        appendCondition(field + " > ?", value);
        return this;
    }

    /**
     * 小于条件（lt = Less Than）
     * @param field 字段名
     * @param value 比较值
     */
    public ConditionBuilder lt(String field, Object value) {
        appendCondition(field + " < ?", value);
        return this;
    }

    /**
     * 区间条件（between = 范围查询）
     * @param field 字段名
     * @param start 起始值
     * @param end 结束值
     */
    public ConditionBuilder between(String field, Object start, Object end) {
        appendCondition(field + " between ? and ?", start, end);
        return this;
    }

    /**
     * 模糊匹配（like = 模糊查询）
     * @param field 字段名
     * @param pattern 匹配模式（需包含%）
     */
    public ConditionBuilder like(String field, String pattern) {
        appendCondition(field + " like ?", pattern);
        return this;
    }

    /**
     * IN查询（in = 多值匹配）
     * @param field 字段名
     * @param values 值数组
     */
    public ConditionBuilder in(String field, Object... values) {
        // 创建与参数数量相同的"?"占位符列表（如["?","?","?"]）
        // 使用Collections.nCopies生成指定数量的占位符，配合String.join实现IN语句参数动态化
        String placeholders = String.join(",", Collections.nCopies(values.length, "?"));
        appendCondition(field + " in (" + placeholders + ")", values);
        return this;
    }

    /**
     * 不等于（ne = Not Equal）
     * @param field 字段名
     * @param value 比较值
     */
    public ConditionBuilder ne(String field, Object value) {
        appendCondition(field + " != ?", value);
        return this;
    }

    /**
     * 大于等于（ge = Greater than or Equal）
     * @param field 字段名
     * @param value 比较值
     */
    public ConditionBuilder ge(String field, Object value) {
        appendCondition(field + " >= ?", value);
        return this;
    }

    /**
     * 小于等于（le = Less than or Equal）
     * @param field 字段名
     * @param value 比较值
     */
    public ConditionBuilder le(String field, Object value) {
        appendCondition(field + " <= ?", value);
        return this;
    }

    /**
     * 空值检查（isNull = 字段为NULL）
     * @param field 字段名
     */
    public ConditionBuilder isNull(String field) {
        appendCondition(field + " is null");
        return this;
    }

    /**
     * 非空检查（isNotNull = 字段不为NULL）
     * @param field 字段名
     */
    public ConditionBuilder isNotNull(String field) {
        appendCondition(field + " is not null");
        return this;
    }

    /**
     * 自定义条件（raw = 原生SQL）
     * @param rawClause 原生条件语句
     * @param values 对应参数
     */
    public ConditionBuilder raw(String rawClause, Object... values) {
        appendCondition(rawClause, values);
        return this;
    }

    // 内部方法：追加条件和参数
    // 使用可变参数和Collections.addAll实现批量参数添加
    private void appendCondition(String condition, Object... values) {
        if (whereClause.length() > 0) {
            whereClause.append(" and ");// StringBuilder追加字符串效率高于String拼接
        }
        whereClause.append(condition);
        // 使用Collections工具类批量添加参数，比循环添加更简洁且保持代码可读性
        Collections.addAll(params, values);
    }

    /**
     * 设置分页（page = 分页参数配置）
     * @param cursorField 游标字段（通常为排序字段）
     * @param cursorValue 游标起始值
     * @param pageSize 每页数量
     */
    public ConditionBuilder page(String cursorField, Object cursorValue, int pageSize) {
        pagination.initPage(cursorField, cursorValue, pageSize);
        return this;
    }




    /* ------------------------ 外用方法 ------------------------ */
    /**
     * 获取WHERE子句（getWhereClause = 生成WHERE部分）
     * @return 包含所有条件的WHERE子句
     */
    public String getWhereClause() {
        StringBuilder finalSql = new StringBuilder(whereClause);
        pagination.appendPaginationCondition(finalSql);
        return finalSql.toString();
    }

    /**
     * 获取完整SQL（getFullSql = 生成完整查询语句）
     * @return 包含WHERE、ORDER BY和LIMIT的完整SQL
     */
    public String getFullSql() {
        StringBuilder sql = new StringBuilder(whereClause);
        pagination.appendPaginationCondition(sql); // 添加分页条件
        pagination.appendOrderAndLimit(sql); // 添加排序和限制条件
        return " where " + sql;
    }

    /**
     * 获取参数数组（getParams = 收集所有参数）
     * @return 按条件顺序的参数数组
     */
    public Object[] getParams() {
        // 创建新ArrayList保证原始参数列表不被修改，同时允许添加分页参数
        List<Object> finalParams = new ArrayList<>(params);
        pagination.addPaginationParams(finalParams);
        // ArrayList转数组时自动处理类型转换，保证返回正确的Object[]类型
        return finalParams.toArray();
    }
}