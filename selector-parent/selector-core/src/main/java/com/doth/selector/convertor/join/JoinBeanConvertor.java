package com.doth.selector.convertor.join;

import com.doth.selector.anno.Join;
import com.doth.selector.common.exception.mapping.NonPrimaryKeyException;
import com.doth.selector.common.util.NamingConvertUtil;
import com.doth.selector.convertor.BeanConvertor;
import com.doth.selector.convertor.supports.ConvertDtoContext;
import com.doth.selector.convertor.supports.JoinConvertContext;
import com.doth.selector.common.exception.mapping.FailedToBuildDTOException;
import com.doth.selector.common.exception.mapping.FailedToBuildEntityException;
import com.doth.selector.common.exception.mapping.JoinConvertorException;
import lombok.extern.slf4j.Slf4j;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.doth.selector.convertor.supports.JoinConvertContext.*;
import static com.doth.selector.convertor.supports.ResultSetUtils.extractColumnLabels;

/**
 * JoinBeanConvertor: 核心工具类, 自动将 ResultSet 转换为包含关联属性的 Bean 或 DTO 实例
 *
 * <p>
 * <strong>职责</strong>
 * <ol>
 *   <li>基于 @Join 注解解析 Bean 类结构并生成关联字段元信息</li>
 *   <li>利用缓存和指纹识别复用元信息</li>
 *   <li>遍历 ResultSet 根据元信息构建对象并填充字段值</li>
 *   <li>支持实体与 DTO 之间的转换</li>
 * </ol>
 * </p>
 *
 * <p>后续可扩展支持更多注解类型和自定义映射策略</p>
 */
@Slf4j
public class JoinBeanConvertor implements BeanConvertor {

    /**
     * 核心 API: 将 ResultSet 转换为指定 Bean/DTO
     *
     * @param rs        查询结果集
     * @param beanClass 目标 Bean 或 DTO 类
     * @param <T>       泛型类型
     * @return 填充后的对象或 DTO 实例
     */
    @Override
    public <T> T convert(ResultSet rs, Class<T> beanClass) {
        try {
            // 1. 通过 @DependOn 解析原类型
            Class<?> actualClass = ConvertDtoContext.resolveActualClass(beanClass);

            // 3. 查询缓存
            Map<String, JoinConvertContext.MetaMap> metaGroup = JOIN_CACHE.computeIfAbsent(actualClass,
                    k -> new ConcurrentHashMap<>()
            );

            // 2. 提取结果集列名
            Set<String> columnSet = extractColumnLabels(rs.getMetaData());
            String fingerprint = ConvertDtoContext.getFingerprint(columnSet);

            JoinConvertContext.MetaMap metaMap = metaGroup.computeIfAbsent(fingerprint, fp -> {
                try {
                    return analyzeClzStruct(actualClass, columnSet, "");
                } catch (Exception e) {
                    throw new RuntimeException("解析联表结构失败: " + e.getMessage(), e);
                }
            });

            // 5. 通过缓存中的 类信息元 构建实际实体
            Object entity;
            try {
                entity = build(rs, actualClass, metaMap);
                // log.info("book info: {}", (BookCard) entity);
            } catch (Throwable e) {
                throw new FailedToBuildEntityException("构造实体对象失败: " + e.getMessage(), e);
            }

            // 6. DTO 构造
            if (!actualClass.equals(beanClass)) {
                try {
                    Constructor<T> ctor = ConvertDtoContext.getDtoConstructor(beanClass, actualClass);
                    MethodHandle mh = ConvertDtoContext.getConstructorHandle(ctor);

                    @SuppressWarnings("unchecked")
                    T t = (T) mh.invoke(entity);
                    return t;
                } catch (Throwable e) {
                    throw new FailedToBuildDTOException("创建 DTO 时出现了异常! 所处类: " + beanClass.getName(), e);
                }
            }

            return beanClass.cast(entity);
        } catch (Exception e) {
            // 总兜底
            throw (e instanceof JoinConvertorException)
                    ? (JoinConvertorException) e
                    : new JoinConvertorException("JoinBeanConvertor: 转换异常", e);
        }
    }

    /**
     * 解析类结构生成元数据信息
     *
     * <p>递归遍历字段, 对带 @Join 注解的字段生成嵌套 MetaMap, 对可映射字段添加字段元信息</p>
     *
     * @param clz       当前处理的类
     * @param columnSet 结果集列名集合
     * @param prefix    列名前缀
     * @return 解析后的 MetaMap 对象
     * @throws Exception 分析过程中的异常
     */
    private JoinConvertContext.MetaMap analyzeClzStruct(Class<?> clz, Set<String> columnSet, String prefix) throws Exception {
        JoinConvertContext.MetaMap metaMap = new JoinConvertContext.MetaMap();

        // 复用缓存字段
        Field[] fields = JoinConvertContext.getFields(clz);

        for (Field field : fields) {
            String _fName = NamingConvertUtil.camel2SnakeCase(field.getName());
            String colName = prefix + _fName;

            // 实际上赋值的分支在else if里, if-join 分支是用于递归进入else-if里
            if (field.isAnnotationPresent(Join.class)) {
                // 准备下层嵌套
                String nestedPrefix = _fName + "_";
                Class<?> joinClz = field.getType();

                JoinConvertContext.MetaMap nested =
                        analyzeClzStruct(joinClz, columnSet, nestedPrefix);

                // 完善类元信息
                Join join = field.getAnnotation(Join.class);
                String fk = join.fk();
                String refPk = join.refPK();

                // 添加类元信息
                metaMap.addNestedMeta(field, nested, fk, refPk);
            } else if (columnSet.contains(colName)) { // 如果结果集未涉及的字段, 则不处理
                // 添加字段元信息
                metaMap.addFieldMeta(field, colName);
            }
        }

        return metaMap;
    }


    /**
     * 根据元数据信息构建 对象实例 并填充字段, 一层层将孙对象填充到子对象, 最后挂到父对象成员
     *
     * @param rs        查询结果集
     * @param beanClass 对象类
     * @param metaMap   结构元信息
     * @param <T>       泛型类型
     * @return 填充后的对象实例
     * @throws Throwable 构造或填充过程中的异常
     */
    private <T> T build(ResultSet rs, Class<T> beanClass, JoinConvertContext.MetaMap metaMap) throws Throwable {
        // 1. 实例化最外层类, 无论多少递归都是补充当前类
        T bean = beanClass.getDeclaredConstructor().newInstance();  // 预填充

        // 2. 填充当前层普通字段
        this.populateScalarFields(rs, bean, metaMap);


        // 3. 处理每个 join 子对象
        for (Map.Entry<Field, JoinConvertContext.MetaMap> e : metaMap.getNestedMeta().entrySet()) {
            Field joinField = e.getKey();
            Class<?> joinClz = joinField.getType();
            JoinConvertContext.MetaMap childMeta = e.getValue();

            // ---- 收集本 join 所需列名
            String fkColumn = metaMap.getFkColumn(joinField);   // 主表外键列
            String refColumn = metaMap.getRefColumn(joinField);  // 子表主键/引用列

            // 4. 递归构建子对象
            Object child = build(rs, joinClz, childMeta);

            // 5. 判断子对象是否应丢弃
            if (isAllFieldsNull(child, childMeta)) continue;

            // 6. 外键 / 引用键值补齐
            Object fkValue = this.getObjectIgnorant(rs, fkColumn);
            Object refValue = this.getObjectIgnorant(rs, refColumn);

            // 统一主外键, 主/外键只要其一有值, 则赋值至从表主键
            if (fkValue != null || refValue != null) {
                // 找从表主键字段
                String joinKey = NamingConvertUtil.snake2CamelCase(refColumn);
                Field refField = JoinConvertContext.getField(
                        joinClz,
                        joinKey
                );
                if (refField != null) {
                    Object unifiedVal = (refValue != null) ? refValue : fkValue;
                    setFieldValue(child, refField, unifiedVal);
                } else {
                    log.error("对从表 '{}' 的主键 '{}' 赋值的时候发生了异常!", joinClz, joinKey);
                    throw new NonPrimaryKeyException("从表 {" + joinClz.getName() + "} 主键不存在! 请检查定义是否正确!");
                }
            }

            // 7. 将子对象挂到父对象
            setFieldValue(bean, joinField, child);
        }

        return bean;
    }

    /**
     * 为当前对象赋值
     *
     * @param rs   结果集
     * @param bean bean对象
     * @param meta 映射元
     * @param <T>  具体bean类型
     */
    private <T> void populateScalarFields(ResultSet rs, T bean, JoinConvertContext.MetaMap meta) throws Throwable {
        for (Map.Entry<Field, String> entry : meta.getFieldMeta().entrySet()) {
            Field f = entry.getKey();
            String column = entry.getValue();
            setFieldValue(bean, f, rs, column);
        }
    }

    /**
     * 粗鲁的获取值, 如果报异常直接返回null
     *
     * @param rs      查询结果集
     * @param colName 列名
     * @return 列值或 null
     */
    private Object getObjectIgnorant(ResultSet rs, String colName) {
        try {
            return rs.getObject(colName);
        } catch (SQLException ignored) {
            return null;
        }
    }

}
