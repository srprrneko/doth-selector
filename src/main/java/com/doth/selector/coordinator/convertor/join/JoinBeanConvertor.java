
package com.doth.selector.coordinator.convertor.join;
import com.doth.selector.anno.Join;
import com.doth.selector.coordinator.convertor.BeanConvertor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

/**
 * 联查结果转换器
 *
 * 功能 ResultSet 转换转化为嵌套实体
 * 支持说明
 *   1.@Join 注解处理关联字段
 *   2.使用 MethodHandle 实现高效字段赋值
 *   3.类结构元缓存提升转换性能
 *   4.通过 JOIN_CACHE 缓存 MetaMap 对象，避免每次转换时重复解析类的关联结构
 */
@Deprecated
public class JoinBeanConvertor implements BeanConvertor {



    /**
     * 类结构元缓存器
     *
     * Key: bean的Class对象, 主对象和嵌套对象, 避免多次映射
     * Value: 解析完成的字段映射关系（包含普通字段和关联字段）
     */
    private static final Map<Class<?>, MetaMap> JOIN_CACHE = new HashMap<>();

    /**
     * 字段赋值器缓存, 避免多次创建 MH 对象
     *
     * Key: 需要赋值的字段对象
     * Value: 通过MethodHandle生成的字段赋值器
     */
    private static final Map<Field, MethodHandle> SETTER_CACHE = new HashMap<>();

    /**
     * 核心转换方法, 入口
     *
     * @param rs 包含联表查询结果的数据集（必须包含所有需要的字段）
     * @param beanClass 要转换的目标Bean类型
     * @return 包含完整嵌套结构的JavaBean
     *
     * 实现流程：
     * 1. 获取结果集元数据
     * 2. 从缓存获取或解析类结构映射
     * 3. 根据映射关系构建对象实例
     *
     * 注意:
     * 1. 顶层对象无前缀, 所以传递前缀参数为"",
     * 2. 递归的起点是主表，其字段不需要前缀。嵌套对象才会触发前缀生成。
     */
    @Override
    public <T> T convert(ResultSet rs, Class<T> beanClass) throws Throwable {
        ResultSetMetaData meta = rs.getMetaData();

        // 通过元缓存器 获取结构元
        MetaMap metaMap = JOIN_CACHE.computeIfAbsent(beanClass, clz -> {
            try {
                // 1.解析类结构
                return analyzeClzStruct(clz, meta, ""); // 主表无前缀
            } catch (Exception e) {
                throw new RuntimeException("解析联表结构失败: " + e.getMessage(), e);
            }
        });

        // 根据映射关系构建对象实例
        return buildJoinBean(rs, beanClass, metaMap);
    }

    /**
     * 递归解析类结构
     *
     * @param clz 当前解析的目标类
     * @param meta 结果集元数据（用于字段存在性验证）
     * @param prefix 字段前缀（用于处理嵌套对象的字段别名）
     */
    private MetaMap analyzeClzStruct(Class<?> clz, ResultSetMetaData meta, String prefix) throws Exception {
        // 初始化元数据结构
        MetaMap metaMap = new MetaMap();

        // 直接遍历类的所有字段
        for (Field field : clz.getDeclaredFields()) {
            field.setAccessible(true); // 开启访问权限

            // 处理关联字段（带@JoinColumn注解）
            // Join joinColumn = field.getAnnotation(Join.class); // 直接通过该方式获取注解, 无需使用iAnnotationPresent进行判断, 后续再通过!=null来区分开来

            // if (joinColumn != null) {
            if (field.isAnnotationPresent(Join.class)) { // 第二种方式, 先判断在获取, 避免由运行时期的注解自动返回的动态代理类, 注解代理类, 带来的空指针异常
                // 判断当前关联的对象 是否和主表的关联 形成一对一关联  防止一对一死递归的问题



                Join join = field.getAnnotation(Join.class);

                String fkColumn = join.fk();  // 获取外键列名
                // 处理引用列（默认为"id"）
                // String refColumn = join.referencedColumn().isEmpty() ? "id" : join.referencedColumn(); // 去除没有必要的防御性编程
                String refColumn = join.refFK(); // 直接获取


                // 验证外键列是否存在
                if (columnExists(meta, fkColumn)) { // 是自定义对象
                    Class<?> refClass = field.getType();  // 获取关联类
                    String nestedPrefix = field.getName() + "_";  // 生成嵌套字段前缀 department_, department_name
                    // 递归解析关联类结构
                    MetaMap refMapping = analyzeClzStruct(refClass, meta, nestedPrefix); // nested: 嵌套
                    // 存储嵌套映射关系
                    metaMap.addNestedMeta(field, refMapping, fkColumn, refColumn);
                }

            // 处理普通字段
            } else {
                String columnName = prefix + field.getName();  // 构造完整列名
                if (columnExists(meta, columnName)) {
                    metaMap.addFieldMeta(field, columnName);  // 存储字段映射
                }
            }
        }

        return metaMap;
    }

    /**
     * 验证结果集中是否存在指定列
     * 遍历结果元中的所有列, 验证是否存在指定列名
     */
    private boolean columnExists(ResultSetMetaData meta, String columnName) throws SQLException {
        int columnCount = meta.getColumnCount();
        // 遍历所有列进行匹配（不区分大小写）
        for (int i = 1; i <= columnCount; i++) {
            String colName = meta.getColumnLabel(i);
            if (colName.equalsIgnoreCase(columnName)) { // d_id -> d_id
                return true;
            }
        }
        return false;
    }

    /**
     * 构建JavaBean实例（递归方法）
     * @param rs 结果集
     * @param beanClass 最终目标类
     * @param metaMap 类结构元映射 信息
     */
    private <T> T buildJoinBean(ResultSet rs, Class<T> beanClass, MetaMap metaMap) throws Throwable {
        // 初始化目标类 准备进行填充
        T bean = beanClass.getDeclaredConstructor().newInstance();

        // 通过字段缓存器 设置主表值
        for (Map.Entry<Field, String> entry : metaMap.getFieldMeta().entrySet()) {
            Field k = entry.getKey();  // 获取字段键
            Object v = rs.getObject(entry.getValue()); // 获取字段对应的列名

            setFieldValue(bean, k, v);  // 使用缓存setter + 方法句柄 进行赋值`
        }

        // 处理关联对象
        for (Map.Entry<Field, MetaMap> entry : metaMap.getNestedMeta().entrySet()) {
            Field field = entry.getKey();
            MetaMap nestedMeta = entry.getValue();

            // 获取外键信息
            String fkColumn = metaMap.getFkColumn(field);
            String refColumn = metaMap.getRefColumn(field);

            // 获取外键值
            Object fkValue = rs.getObject(fkColumn);
            // 递归获取嵌套对象
            Object refBean = buildJoinBean(rs, field.getType(), nestedMeta);

            // 设置关联对象的外键字段值
            Field refField = getField(field.getType(), refColumn);
            if (refField != null) {
                setFieldValue(refBean, refField, fkValue);
            }

            // 将关联对象设置到当前Bean
            setFieldValue(bean, field, refBean);
        }

        return bean;
    }

    /**
     * 字段赋值方法
     *  <p>实现步骤<p/>
     *  <p>1. computeIfAbsent: 有则获取, 没则计算并创建存储, 一参键二参值<p/>
     *  <p>2. 缓存区一参有则返回复用没则走二参创建, 最终返回值<p/>
     */
    private void setFieldValue(Object target, Field field, Object value) throws Throwable {
        MethodHandle setter = SETTER_CACHE.computeIfAbsent(field, f -> {
            try {
                // 生成MethodHandle赋值器
                return MethodHandles.lookup().unreflectSetter(f);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("赋值失败! " + e.getMessage());
            }
        });
        setter.invoke(target, value);  // 赋值
    }

    /**
     * 查找类中的指定字段（支持私有字段）
     */
    private Field getField(Class<?> clz, String name) {
        for (Field field : clz.getDeclaredFields()) {
            if (field.getName().equals(name)) {
                field.setAccessible(true);  // 允许访问私有字段
                return field;
            }
        }
        return null;
    }


}