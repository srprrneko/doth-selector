// 保持原始包路径不变
package com.doth.selector.executor.supports.builder;


import com.doth.selector.anno.Overload;
import com.doth.selector.executor.supports.lambda.Lambda2FieldNameResolver;
import com.doth.selector.executor.supports.lambda.SFunction;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * SQL条件构建器，用于动态生成WHERE子句和分页查询
 */
public class ConditionBuilder<T> {


    private final StringBuilder whereClause = new StringBuilder();

    // sql参数集合
    private final List<Object> params = new ArrayList<>();

    // 分页处理器
    private final BuilderPage pagination = new BuilderPage();

    // 获取所有字段
    @Getter
    private final Set<String> usedFieldPaths = new HashSet<>();


    /**
     * 实体类型
     */
    @Setter
    private Class<T> entityClz;

    public ConditionBuilder() {
    }

    public ConditionBuilder(Class<T> entityClz) {
        this.entityClz = entityClz;
    }


    /**
     * 记录字段路径
      * @param field
     */
    private void recordField(String field) {
        if (field != null && field.contains(".")) {
            usedFieldPaths.add(field);
        }
    }

    /**
     * 提取 join 表别名前缀集合，例: t1, t2
     * @return 不重复的 set 集合
     */
    public Set<String> extractJoinTablePrefixes() {
        Set<String> result = new HashSet<>();
        for (String path : usedFieldPaths) {
            int dot = path.indexOf('.');
            if (dot > 0) result.add(path.substring(0, dot));
        }
        return result;
    }


    /**
     * 等于条件（eq = Equal）
     * @param field 字段名
     * @param value 比较值
     */
    public ConditionBuilder<T> eq(String field, Object value) {
        recordField(field);

        appendCondition(field + " = ?", value);
        return this;
    }

    /**
     * 大于条件（gt = Greater Than）
     * @param field 字段名
     * @param value 比较值
     */
    public ConditionBuilder<T> gt(String field, Object value) {
        recordField(field);

        appendCondition(field + " > ?", value);
        return this;
    }

    /**
     * 小于条件（lt = Less Than）
     * @param field 字段名
     * @param value 比较值
     */
    public ConditionBuilder<T> lt(String field, Object value) {
        recordField(field);

        appendCondition(field + " < ?", value);
        return this;
    }

    /**
     * 区间条件（between = 范围查询）
     * @param field 字段名
     * @param start 起始值
     * @param end   结束值
     */
    public ConditionBuilder<T> between(String field, Object start, Object end) {
        recordField(field);

        appendCondition(field + " between ? and ?", start, end);
        return this;
    }

    /**
     * 模糊匹配（like = 模糊查询）
     * @param field   字段名
     * @param pattern 匹配模式（需包含%）
     */
    public ConditionBuilder<T> like(String field, String pattern) {
        recordField(field);

        appendCondition(field + " like ?", pattern);
        return this;
    }

    /**
     * IN查询 条件匹配
     * @param field  字段名
     * @param values 值数组
     */
    public ConditionBuilder<T> in(String field, Object... values) {
        recordField(field);

        String placeholders = String.join(",", Collections.nCopies(values.length, "?"));
        appendCondition(field + " in (" + placeholders + ")", values);
        return this;
    }

    /**
     * NOT IN 条件匹配
     * @param field  字段名
     * @param values 值数组
     */
    public ConditionBuilder<T> nin(String field, Object... values) {
        recordField(field);

        String placeholders = String.join(",", Collections.nCopies(values.length, "?"));
        appendCondition(field + " not in (" + placeholders + ")", values);
        return this;
    }

    /**
     * 不等于 ne = not equal
     * @param field 字段名
     * @param value 比较值
     */
    public ConditionBuilder<T> ne(String field, Object value) {
        recordField(field);

        appendCondition(field + " != ?", value);
        return this;
    }

    /**
     * 大于等于 ge = greater than or equal
     * @param field 字段名
     * @param value 比较值
     */
    public ConditionBuilder<T> ge(String field, Object value) {
        recordField(field);

        appendCondition(field + " >= ?", value);
        return this;
    }

    /**
     * 小于等于 le = less than or equal
     * @param field 字段名
     * @param value 比较值
     */
    public ConditionBuilder<T> le(String field, Object value) {
        recordField(field);

        appendCondition(field + " <= ?", value);
        return this;
    }

    /**
     * 空值检查 where ... name isNull
     *
     * @param field 字段名
     */
    public ConditionBuilder<T> isNull(String field) {
        recordField(field);

        appendCondition(field + " is null");
        return this;
    }

    /**
     * 非空检查 where ... name isNotNull
     * @param field 字段名
     */
    public ConditionBuilder<T> isNotNull(String field) {
        recordField(field);

        appendCondition(field + " is not null");
        return this;
    }

    /**
     * todo: 未来可能考虑丢弃
     * 自定义条件（raw = 原生SQL）
     *
     * @param rawClause 原生条件语句
     * @param values    对应参数
     */
    @Deprecated
    public ConditionBuilder<T> raw(String rawClause, Object... values) {
        appendCondition(rawClause, values);
        return this;
    }

    // region !!============================== lambda条件式 的重载区域 ==============================!!
    @Overload
    public <R> ConditionBuilder<T> eq(SFunction<T, R> lambda, Object value) {
        String field = Lambda2FieldNameResolver.resolve(lambda, entityClz);
        return eq(field, value);
    }

    @Overload
    public <R> ConditionBuilder<T> gt(SFunction<T, R> lambda, Object value) {
        String field = Lambda2FieldNameResolver.resolve(lambda, entityClz);
        return gt(field, value);
    }

    @Overload
    public <R> ConditionBuilder<T> lt(SFunction<T, R> lambda, Object value) {
        String field = Lambda2FieldNameResolver.resolve(lambda, entityClz);
        return lt(field, value);
    }

    @Overload
    public <R> ConditionBuilder<T> ge(SFunction<T, R> lambda, Object value) {
        String field = Lambda2FieldNameResolver.resolve(lambda, entityClz);
        return ge(field, value);
    }

    @Overload
    public <R> ConditionBuilder<T> le(SFunction<T, R> lambda, Object value) {
        String field = Lambda2FieldNameResolver.resolve(lambda, entityClz);
        return le(field, value);
    }

    @Overload
    public <R> ConditionBuilder<T> ne(SFunction<T, R> lambda, Object value) {
        String field = Lambda2FieldNameResolver.resolve(lambda, entityClz);
        return ne(field, value);
    }

    @Overload
    public <R> ConditionBuilder<T> like(SFunction<T, R> lambda, String pattern) {
        String field = Lambda2FieldNameResolver.resolve(lambda, entityClz);
        return like(field, pattern);
    }

    @Overload
    public <R> ConditionBuilder<T> between(SFunction<T, R> lambda, Object start, Object end) {
        String field = Lambda2FieldNameResolver.resolve(lambda, entityClz);
        return between(field, start, end);
    }

    @Overload
    public <R> ConditionBuilder<T> in(SFunction<T, R> lambda, Object... values) {
        String field = Lambda2FieldNameResolver.resolve(lambda, entityClz);
        return in(field, values);
    }

    @Overload
    public <R> ConditionBuilder<T> nin(SFunction<T, R> lambda, Object... values) {
        String field = Lambda2FieldNameResolver.resolve(lambda, entityClz);
        return nin(field, values);
    }

    @Overload
    public <R> ConditionBuilder<T> isNull(SFunction<T, R> lambda) {
        String field = Lambda2FieldNameResolver.resolve(lambda, entityClz);
        return isNull(field);
    }

    @Overload
    public <R> ConditionBuilder<T> isNotNull(SFunction<T, R> lambda) {
        String field = Lambda2FieldNameResolver.resolve(lambda, entityClz);
        return isNotNull(field);
    }

    // endregion  ============================== 重载 区域 ==============================


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
     * @param pageSize    每页数量
     */
    @Deprecated // todo: 待优化
    public ConditionBuilder<T> page(String cursorField, Object cursorValue, int pageSize) {
        pagination.initPage(cursorField, cursorValue, pageSize);
        return this;
    }




    /* ------------------------ 服务于底层的方法区域 ------------------------ */

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


    public static Set<String> getAllTAliasesFromWhere(String whereClause) {
        Set<String> aliases = new HashSet<>();
        // 正则匹配所有形如 t1.name, t2.id 等字段
        Matcher matcher = Pattern.compile("(t\\d+)\\.\\w+").matcher(whereClause);
        while (matcher.find()) {
            aliases.add(matcher.group(1)); // 提取 t0, t1 ...
        }
        return aliases;
    }


}