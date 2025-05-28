// 保持原始包路径不变
package com.doth.selector.executor.supports.builder;


import com.doth.selector.anno.Overload;
import com.doth.selector.executor.supports.lambda.LambdaFieldPathResolver;
import com.doth.selector.executor.supports.lambda.SFunction;
// import com.doth.selector.temp.MethodReferenceParserV1;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * SQL条件构建器，用于动态生成WHERE子句和分页查询
 * 使用示例：new EntityAdapter().eq("name", "Alice").page("id", 1000, 20).getFullSql()
 */
public class ConditionBuilder<T> {
    // where子句构建器
    private final StringBuilder whereClause = new StringBuilder();

    // sql参数集合
    private final List<Object> params = new ArrayList<>();


    // 分页处理器（组合模式） 隔离分页逻辑
    private final BuilderPage pagination = new BuilderPage();

    /**
     * 实体类型
     */
    private Class<T> entityClz;

    public void setEntityClz(Class<T> entityClz) {
        this.entityClz = entityClz;
    }

    public ConditionBuilder(){}

    public ConditionBuilder(Class<T> entityClz) {
        this.entityClz = entityClz;
    }

    /**
     * 等于条件（eq = Equal）
     * @param field 字段名
     * @param value 比较值
     */
    public ConditionBuilder<T> eq(String field, Object value) {
        appendCondition(field + " = ?", value);
        return this;
    }

    /**
     * 大于条件（gt = Greater Than）
     * @param field 字段名
     * @param value 比较值
     */
    public ConditionBuilder<T> gt(String field, Object value) {
        appendCondition(field + " > ?", value);
        return this;
    }

    /**
     * 小于条件（lt = Less Than）
     * @param field 字段名
     * @param value 比较值
     */
    public ConditionBuilder<T> lt(String field, Object value) {
        appendCondition(field + " < ?", value);
        return this;
    }

    /**
     * 区间条件（between = 范围查询）
     * @param field 字段名
     * @param start 起始值
     * @param end 结束值
     */
    public ConditionBuilder<T> between(String field, Object start, Object end) {
        appendCondition(field + " between ? and ?", start, end);
        return this;
    }

    /**
     * 模糊匹配（like = 模糊查询）
     * @param field 字段名
     * @param pattern 匹配模式（需包含%）
     */
    public ConditionBuilder<T> like(String field, String pattern) {
        appendCondition(field + " like ?", pattern);
        return this;
    }

    /**
     * IN查询（in = 多值匹配）
     * @param field 字段名
     * @param values 值数组
     */
    public ConditionBuilder<T> in(String field, Object... values) {
        // 创建与参数数量相同的"?"占位符列表（如["?","?","?"]）
        // 使用Collections.nCopies生成指定数量的占位符，配合String.join实现IN语句参数动态化
        String placeholders = String.join(",", Collections.nCopies(values.length, "?"));
        appendCondition(field + " in (" + placeholders + ")", values);
        return this;
    }

    /**
     * NOT IN查询（not in = 多值匹配）
     * @param field 字段名
     * @param values 值数组
     */
    public ConditionBuilder<T> nin(String field, Object... values) {
        // 创建与参数数量相同的"?"占位符列表（如["?","?","?"]）
        // 使用Collections.nCopies生成指定数量的占位符，配合String.join实现IN语句参数动态化
        String placeholders = String.join(",", Collections.nCopies(values.length, "?"));
        appendCondition(field + " not in (" + placeholders + ")", values);
        return this;
    }

    /**
     * 不等于（ne = Not Equal）
     * @param field 字段名
     * @param value 比较值
     */
    public ConditionBuilder<T> ne(String field, Object value) {
        appendCondition(field + " != ?", value);
        return this;
    }

    /**
     * 大于等于（ge = Greater than or Equal）
     * @param field 字段名
     * @param value 比较值
     */
    public ConditionBuilder<T> ge(String field, Object value) {
        appendCondition(field + " >= ?", value);
        return this;
    }

    /**
     * 小于等于（le = Less than or Equal）
     * @param field 字段名
     * @param value 比较值
     */
    public ConditionBuilder<T> le(String field, Object value) {
        appendCondition(field + " <= ?", value);
        return this;
    }

    /**
     * 空值检查（isNull = 字段为NULL）
     * @param field 字段名
     */
    public ConditionBuilder<T> isNull(String field) {
        appendCondition(field + " is null");
        return this;
    }

    /**
     * 非空检查（isNotNull = 字段不为NULL）
     * @param field 字段名
     */
    public ConditionBuilder<T> isNotNull(String field) {
        appendCondition(field + " is not null");
        return this;
    }

    /**
     * todo: 正在优化
     * 自定义条件（raw = 原生SQL）
     * @param rawClause 原生条件语句
     * @param values 对应参数
     */
    @Deprecated
    public ConditionBuilder<T> raw(String rawClause, Object... values) {
        appendCondition(rawClause, values);
        return this;
    }

    // region ============================== 重载 区域 ==============================
    @Overload
    public <R> ConditionBuilder<T> eq(SFunction<T, R> lambda, Object value) {
        String field = LambdaFieldPathResolver.resolve(lambda, entityClz);
        return eq(field, value);
    }

    @Overload
    public <R> ConditionBuilder<T> gt(SFunction<T, R> lambda, Object value) {
        String field = LambdaFieldPathResolver.resolve(lambda, entityClz);
        return gt(field, value);
    }

    @Overload
    public <R> ConditionBuilder<T> lt(SFunction<T, R> lambda, Object value) {
        String field = LambdaFieldPathResolver.resolve(lambda, entityClz);
        return lt(field, value);
    }

    @Overload
    public <R> ConditionBuilder<T> ge(SFunction<T, R> lambda, Object value) {
        String field = LambdaFieldPathResolver.resolve(lambda, entityClz);
        return ge(field, value);
    }

    @Overload
    public <R> ConditionBuilder<T> le(SFunction<T, R> lambda, Object value) {
        String field = LambdaFieldPathResolver.resolve(lambda, entityClz);
        return le(field, value);
    }

    @Overload
    public <R> ConditionBuilder<T> ne(SFunction<T, R> lambda, Object value) {
        String field = LambdaFieldPathResolver.resolve(lambda, entityClz);
        return ne(field, value);
    }

    @Overload
    public <R> ConditionBuilder<T> like(SFunction<T, R> lambda, String pattern) {
        String field = LambdaFieldPathResolver.resolve(lambda, entityClz);
        return like(field, pattern);
    }

    @Overload
    public <R> ConditionBuilder<T> between(SFunction<T, R> lambda, Object start, Object end) {
        String field = LambdaFieldPathResolver.resolve(lambda, entityClz);
        return between(field, start, end);
    }

    @Overload
    public <R> ConditionBuilder<T> in(SFunction<T, R> lambda, Object... values) {
        String field = LambdaFieldPathResolver.resolve(lambda, entityClz);
        return in(field, values);
    }

    @Overload
    public <R> ConditionBuilder<T> nin(SFunction<T, R> lambda, Object... values) {
        String field = LambdaFieldPathResolver.resolve(lambda, entityClz);
        return nin(field, values);
    }

    @Overload
    public <R> ConditionBuilder<T> isNull(SFunction<T, R> lambda) {
        String field = LambdaFieldPathResolver.resolve(lambda, entityClz);
        return isNull(field);
    }

    @Overload
    public <R> ConditionBuilder<T> isNotNull(SFunction<T, R> lambda) {
        String field = LambdaFieldPathResolver.resolve(lambda, entityClz);
        return isNotNull(field);
    }

    // endregion  ============================== 重载 区域 ==============================




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
     * 设置分页（page = 分页参数配置）todo: 去掉返回值
     * @param cursorField 游标字段（通常为排序字段）
     * @param cursorValue 游标起始值
     * @param pageSize 每页数量
     */
    public ConditionBuilder<T> page(String cursorField, Object cursorValue, int pageSize) {
        pagination.initPage(cursorField, cursorValue, pageSize);
        return this;
    }




    /* ------------------------ 底层处理方法 ------------------------ */
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
    public String getFullCause() {
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

        return finalParams.toArray();
    }


    public static Set<String> extractTableAliasesFromWhere(String whereClause) {
        Set<String> aliases = new HashSet<>();
        // 正则匹配所有形如 t1.name, t2.id 等字段
        Matcher matcher = Pattern.compile("(t\\d+)\\.\\w+").matcher(whereClause);
        while (matcher.find()) {
            aliases.add(matcher.group(1)); // 提取 t0, t1 ...
        }
        return aliases;
    }

}