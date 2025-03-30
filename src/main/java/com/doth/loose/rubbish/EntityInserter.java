package com.doth.loose.rubbish;


import com.doth.stupidrefframe_v1.selector.util.DruidUtil;
import com.doth.stupidrefframe_v1.anno.ColumnName;
import com.doth.stupidrefframe_v1.anno.TableName;
import com.doth.stupidrefframe_v1.exception.DataAccessException;

import java.beans.Transient;
import java.lang.reflect.Field;
import java.util.*;

@Deprecated
public class EntityInserter<T> {

    private T entity;

    // 空值处理策略标志位，默认包含null值 [当插入全部时不使用]
    private boolean ignoreNull = false;

    // 使用Set集合存储排除字段，确保去重
    private Set<String> excludeFields = new HashSet<>();

    // 返回本类, 支持链式调用
    public EntityInserter<T> setEntity(T entity) {
        // 空对象检查，确保数据有效
        if (entity == null) {
            throw new IllegalArgumentException("实体对象不能为NULL!");
        }
        this.entity = entity;
        return this;
    }

    /**
     * 设置空值忽略策略
     */
    public EntityInserter<T> ignoreNull(boolean ignore) {
        // 设置布尔标志位，控制后续字段处理逻辑
        this.ignoreNull = ignore;
        return this;  // 返回this支持链式调用
    }

    /**
     * 添加排除字段
     */
    public EntityInserter<T> exclude(String... fields) {
        // 将可变参数转换为集合元素
        Collections.addAll(this.excludeFields, fields);
        return this;
    }

    /**
     * 执行插入操作
     */
    public int execute() {
        // 获取实体类的Class对象
        Class<?> clazz = entity.getClass();
        try {
            // 存储字段值的动态数组
            List<Object> values = new ArrayList<>();

            // 解析表名（带缓存可优化点）
            String tableName = resolveTableName(clazz);

            // 解析有效列信息并收集参数值
            List<String> columns = resolveColumns(clazz, values);

            // 构建完整SQL语句
            String sql = buildSQL(tableName, columns);

            // 执行数据库操作（假设DbUtil已实现）
            return DruidUtil.executeUpdate(sql, values.toArray());
        } catch (Exception e) {
            // 统一异常封装，便于上层处理
            throw new DataAccessException("插入失败!", e);
        }
    }

    /**
     * 解析字段生成列信息
     */
    private List<String> resolveColumns(Class<?> clazz, List<Object> values) {
        // 存储有效列名的动态数组
        List<String> columns = new ArrayList<>();

        // 遍历所有声明字段（包括私有字段）
        for (Field field : clazz.getDeclaredFields()) {
            try {
                // 允许访问私有字段（破坏封装性，但框架需要）
                field.setAccessible(true);

                // 反射获取字段值
                Object value = field.get(entity);

                // 判断是否需要跳过该字段
                if (shouldSkip(field, value)) continue;

                // 获取数据库列名
                String columnName = getColumnName(field);

                // 收集列名和对应值
                columns.add(columnName);
                values.add(value);
            } catch (IllegalAccessException e) {
                throw new DataAccessException("Field access failed", e);
            }
        }
        return columns;
    }

    /**
     * 字段跳过判断逻辑
     */
    private boolean shouldSkip(Field field, Object value) {
        // 三个跳过条件用逻辑或连接
        return excludeFields.contains(field.getName()) // 字段在排除列表
            || (ignoreNull && value == null) // 开启空值忽略且值为空
            || field.isAnnotationPresent(Transient.class
        );    // 标注瞬态不持久化
    }

    /**
     * 获取字段对应列名
     */
    private String getColumnName(Field field) {
        // 优先使用@Column注解配置的名称
        if (field.isAnnotationPresent(ColumnName.class)) {
            ColumnName column = field.getAnnotation(ColumnName.class);
            // 处理注解值为空的情况，使用字段名
            return column.name().isEmpty() ?
                    camelToSnake(field.getName()) :
                    column.name();
        }
        // 默认转换驼峰为下划线
        return camelToSnake(field.getName());
    }

    /**
     * 解析表名
     */
    private String resolveTableName(Class<?> clazz) {
        // 优先使用@Table注解配置的表名
        if (clazz.isAnnotationPresent(TableName.class)) {
            return clazz.getAnnotation(TableName.class).value();
        }
        // 默认使用类名转换
        return camelToSnake(clazz.getSimpleName());
    }

    /**
     * 构建SQL语句
     */
    private String buildSQL(String tableName, List<String> columns) {
        // 拼接列名部分，如：name, age
        String columnsStr = String.join(", ", columns);

        // 生成占位符部分，如：?, ?
        String placeholders = String.join(", ",
                Collections.nCopies(columns.size(), "?"));

        // 组合完整INSERT语句
        return String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columnsStr, placeholders);
    }

    /**
     * 驼峰转下划线命名
     */
    private String camelToSnake(String str) {
        // 正则表达式匹配小写字母后接大写字母的情况
        // 添加下划线并转小写，如：userName → user_name
        return str.replaceAll("([a-z])([A-Z])", "$1_$2")
                .toLowerCase();
    }
}