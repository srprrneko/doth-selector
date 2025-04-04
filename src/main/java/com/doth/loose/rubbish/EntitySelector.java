package com.doth.loose.rubbish;


import com.doth.stupidrefframe_v1.selector.v1.core.coordinator.ExecuteCoordinator;
import com.doth.stupidrefframe_v1.selector.v1.supports.adapeter.EntityAdapter;
import com.doth.stupidrefframe_v1.selector.v1.supports.builder.ConditionBuilder;
import com.doth.stupidrefframe_v1.selector.v1.supports.convertor.ConvertorType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * 实体查询器 - 提供多种方式的数据库查询操作
 *
 * <p>使用示例：
 * <pre>{@code
 * User user = EntitySelector.query2T(User.class, userCondition);
 * List<User> users = EntitySelector.query2Lst(User.class, builder -> {...});
 * }</pre>
 */
@Deprecated
public class EntitySelector {
    private static final ExecuteCoordinator helper = new ExecuteCoordinator();

    // region ----------------- 单实体查询方法组 -----------------
    /**
     * 执行无条件的单实体查询（不推荐使用）
     *
     * @param beanClass 目标实体类类型
     * @param <T> 返回的实体类型
     * @return 单个查询结果或null
     * @deprecated 无明确查询条件可能返回意外结果，建议使用 {@link #query2T(Class, Object)}
     */
    @Deprecated
    public static <T> T query2T(Class<T> beanClass) {
        return helper.getSingleResult(query2Lst(beanClass));
    }

    /**
     * 通过条件对象查询单个实体
     *
     * @param beanClass 目标实体类类型
     * @param conditionBean 包含非空条件字段的JavaBean对象
     * @param <T> 返回的实体类型
     * @return 单个查询结果或null
     */
    public static <T> T query2T(Class<T> beanClass, Object conditionBean) {
        return helper.getSingleResult(query2Lst(beanClass, conditionBean));
    }

    /**
     * 通过条件构建器查询单个实体
     *
     * @param beanClass 目标实体类类型
     * @param conditionSetup 条件构建逻辑的Lambda表达式
     * @param <T> 返回的实体类型
     * @return 单个查询结果或null
     */
    public static <T> T query2T(Class<T> beanClass, Consumer<ConditionBuilder> conditionSetup) {
        return helper.getSingleResult(query2Lst(beanClass, conditionSetup));
    }

    /**
     * 通过Map条件查询单个实体
     *
     * @param beanClass 目标实体类类型
     * @param cond 条件键值对（字段名 → 值）
     * @param <T> 返回的实体类型
     * @return 单个查询结果或null
     */
    public static <T> T query2T(Class<T> beanClass, LinkedHashMap<String, Object> cond) {
        return helper.getSingleResult(query2Lst(beanClass, cond));
    }

    /**
     * 通过原生SQL条件查询单个实体
     *
     * @param beanClass 目标实体类类型
     * @param cond 参数键值对（字段名 → 值）
     * @param condClause 原生条件语句（可包含命名参数，如：age > :minAge）
     * @param <T> 返回的实体类型
     * @return 单个查询结果或null
     */
    public static <T> T query2T(Class<T> beanClass, LinkedHashMap<String, Object> cond, String condClause) {
        return helper.getSingleResult(query2Lst(beanClass, cond, condClause));
    }
    public static <T> T query2T(String sql, Class<T> beanClass, Consumer<ConditionBuilder> conditionSetup) {
        return helper.getSingleResult(query2Lst(sql, beanClass, conditionSetup));
    }
    // endregion

    // region ----------------- 列表查询方法组 -----------------

    /**
     * 无条件查询实体列表
     *
     * @param beanClass 目标实体类类型
     * @param <T> 返回的列表元素类型
     * @return 包含所有记录的实体列表
     */
    public static <T> List<T> query2Lst(Class<T> beanClass) {
        return helper.queryByMap(beanClass, (LinkedHashMap<String, Object>) null);
    }

    /**
     * 通过条件对象查询实体列表
     *
     * @param beanClass 目标实体类类型
     * @param conditionBean 包含非空条件字段的JavaBean对象
     * @param <T> 返回的列表元素类型
     * @return 匹配条件的实体列表
     */
    public static <T> List<T> query2Lst(Class<T> beanClass, Object conditionBean) {
        LinkedHashMap<String, Object> condMap = EntityAdapter.extractNonNullFields(conditionBean);
        return helper.queryByMap(beanClass, condMap);
    }

    /**
     * 通过条件构建器查询实体列表
     *
     * @param beanClass 目标实体类类型
     * @param conditionSetup 条件构建逻辑的Lambda表达式
     * @param <T> 返回的列表元素类型
     * @return 匹配条件的实体列表
     */
    public static <T> List<T> query2Lst(Class<T> beanClass, Consumer<ConditionBuilder> conditionSetup) {
        ConditionBuilder builder = new ConditionBuilder();
        conditionSetup.accept(builder);
        return helper.queryByBuilder(beanClass, builder);
    }

    /**
     * 通过Map条件查询实体列表
     *
     * @param beanClass 目标实体类类型
     * @param cond 条件键值对（字段名 → 值）
     * @param <T> 返回的列表元素类型
     * @return 匹配条件的实体列表
     */
    public static <T> List<T> query2Lst(Class<T> beanClass, LinkedHashMap<String, Object> cond) {
        return helper.queryByMap(beanClass, cond);
    }

    /**
     * 通过原生SQL条件查询实体列表
     *
     * @param beanClass 目标实体类类型
     * @param cond 参数键值对（字段名 → 值）
     * @param condClause 原生条件语句（可包含命名参数，如：age > :minAge）
     * @param <T> 返回的列表元素类型
     * @return 匹配条件的实体列表
     */
    public static <T> List<T> query2Lst(Class<T> beanClass, LinkedHashMap<String, Object> cond, String condClause) {
        return helper.queryByMapVzClause(beanClass, cond, condClause);
    }

    /**
     * 直接执行原生 SQL 查询
     *
     * @param sql 完整 SQL 语句（可包含 ? 占位符）
     * @param params SQL 参数数组（顺序需与占位符对应）
     * @param beanClass 结果集映射的目标类
     * @return 映射后的对象列表
     */
    public static <T> List<T> query2Lst(String sql, Class<T> beanClass, Object ...params) {
        return helper.queryByRaw(beanClass, sql, params);
    }

    public static <T> List<T> query2Lst(String sql, Class<T> beanClass, Consumer<ConditionBuilder> conditionSetup) {
        ConditionBuilder builder = new ConditionBuilder();
        conditionSetup.accept(builder);
        return helper.queryByBuilderVzRaw(beanClass, sql, builder);
        // return helper.mapResultSet(beanClass, sql, builder.getParams());
    }
    // endregion

    // region ----------------- 数据插入方法（待迁移） -----------------
    /*
     * 数据插入方法已标记为待重构，建议迁移至独立的 EntityInserter 类
     * 保持当前类专注于查询功能
     */

    // 十、设计一个方法，基于某个对象自动生成添加数据的 SQL 语句，并把该对象的数据插入到数据库中
    // public static <T> int insert(T t){
    //     // 功能实现...
    //     List<Object> values = getValuesFromObject(t);
    //     // 获取 sql
    //     String sql = SqlGenerate.generateInsert(t.getClass());
    //
    //     int rowsAffected = 0;
    //
    //     try {
    //         System.out.println(values);
    //         rowsAffected = DruidUtil.executeUpdate(sql, values.toArray());
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     } finally {
    //         // 关闭资源
    //         DruidUtil.CloseAll();
    //     }
    //
    //     return rowsAffected;
    // }
    // // 讲对象字段的值封装到 List 中
    // public static <T> List<Object> getValuesFromObject(T t) {
    //     List<Object> values = new ArrayList<>();
    //     Field[] fields = t.getClass().getDeclaredFields();
    //
    //     for (Field field : fields) {
    //         field.setAccessible(true);
    //         try {
    //             values.add(field.get(t));
    //         } catch (IllegalAccessException e) {
    //             e.printStackTrace();
    //         }
    //     }
    //
    //     return values;
    // }
    // endregion
}