package com.doth.selector.convertor.join;

import com.doth.selector.anno.Join;
import com.doth.selector.common.util.NamingConvertUtil;
import com.doth.selector.convertor.BeanConvertor;
import com.doth.selector.convertor.supports.ConvertDtoContext;
import com.doth.selector.convertor.supports.JoinConvertContext;
import com.doth.selector.supports.exception.JoinConvertorException;
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
 *   <li>利用缓存和指纹识别复用元信息以优化性能</li>
 *   <li>遍历 ResultSet 根据元信息构建对象并填充字段值</li>
 *   <li>支持实体与 DTO 之间的转换, 实现转换逻辑隔离</li>
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
     * <p>流程:
     * <ol>
     *   <li>解析实际类, 支持 @DependOn 注解决定是否构造 DTO</li>
     *   <li>提取结果集列名并生成 查询指纹</li>
     *   <li>从缓存获取或分析 类结构元信息</li>
     *   <li>根据元信息构建实体对象并填充字段</li>
     *   <li>若目标类型为 DTO, 调用构造方法生成 DTO 实例</li>
     * </ol>
     *
     * @param rs        查询结果集
     * @param beanClass 目标 Bean 或 DTO 类
     * @param <T>       泛型类型
     * @return 填充后的对象或 DTO 实例
     * @throws Throwable 转换或构造过程中的异常
     */
    @Override
    public <T> T convert(ResultSet rs, Class<T> beanClass) throws Throwable {
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
                entity = buildJoinBean(rs, actualClass, metaMap);
                // log.info("book info: {}", (BookCard) entity);
            } catch (Throwable e) {
                throw new RuntimeException("构造实体对象失败: " + e.getMessage(), e);
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
                    throw new RuntimeException("DTO 构造失败: " + beanClass.getName(), e);
                }
            }

            return beanClass.cast(entity);
        } catch (Exception e) {
            // 总兜底，确保异常不会漏掉
            throw (e instanceof JoinConvertorException) ? (JoinConvertorException) e : new JoinConvertorException("JoinBeanConvertor: 转换异常", e);
        }
    }

/*
现在我要解读这个类的所有流程, 请你帮我验证:
1. 首先是 convert 方法, 这里我有疑问的是 处理并没有将dto模式和普通模式 分成两个处理分支, 而是合在一起处理, 使得语义及其不清晰, 不过这也是dto 模式的特性, 必须先构造实体再创建dto, 从始至终有两个对象要创建, 如果这一块的性能问题得以解决的话, 那么sql懒加载才真正起到作用, 另外当前的JoinBeanConvertor 需要依赖sql中的 别名机制才能正确将 值赋值到对应的字段上, 但实际上有了不同的前缀别名, 就算列名重复也没有关系, 依然可以赋值, 所以后续可以通过这一思路进行改进, 避免对别名的强依赖性

 2. 不过还是先理解为好, 首先你通过了 resolveActualClass 方法获取实际的类对象, 这个方法里面通过DEPENDON_ACTUAL_CACHE缓存dto 的原实体类, 但是我想 这里为什么不共用一个实体的缓存呢? 原来是没有, 这样的话 这个缓存的命名就有些容易引起歧义了, 因为这本身就是原实体的缓存信息, 却变成了 dto 的原实体缓存信息, 明明普通模式下的映射也是需要用到这个缓存的没错吧

2.1 然后你通过extractColumnLabels 方法获取了 单例列名, 这里面的方法很明了:
  public static Set<String> extractColumnLabels(ResultSetMetaData meta) throws SQLException {
        int count = meta.getColumnCount();
        Set<String> labels = new HashSet<>(count);
        for (int i = 1; i <= count; i++) {
            labels.add(meta.getColumnLabel(i).toLowerCase());
        }
        return labels;
    }
获取 结果集元 总列数, 然后初始化一个长度为该总列数的 集合, 接着循环从一开始, 全部添加进该单例集合里去, 这不重要, 我要看这个单例集合什么时候才会被使用到, 此处获取的结果集列名只是做准备, 还没有实际使用, 看来我要调整它 的顺序

然后是查询缓存, 这里有一个从缓存那取出的 元信息组, 其中键是一个String, 我不知道这将放什么东西, 继续往下阅读

然后我看到从单例列集合变成指纹, 通过指纹获取 元信息, 从缓存中获取元信息确保不会重复计算, 接下来看看analyzeClzStruct方法
 这个方法首先创建了 一个 空的元映射信息, 接着通过类对象获取字段数组, 这里将获取缓存的逻辑封装起来了, 接着我看到了FIELD_NAME_CACHE这个map, 键是类对象, 值又是一个map, 我有些不明所以为什么要这样设计, 你遍历了类对象的 所有字段并一一开启了字段访问权限, 接着将字段put进 了             Map<String, Field> map = new ConcurrentHashMap<>();
里, 键是字段名, 值是field对象, 看来后续的映射依赖命名这个键(字段名),
接着进入到 循环快里面,         for (Field field : fields) {
这里很好的将嵌套映射逻辑集中在了analyzeClzStruct 方法里面, 没有让前缀字段映射的硬编码逻辑影响到其他的方法, 后续的非依赖命名映射改动也应该通过这个地方进行更改,
继续解读, 将字段名 转换成驼峰命名, 接着将前缀与该命名拼接在了一起, 最终会判断是否存在 columnSet 中放到metamap里, 这里是否多余? 我换成了else报错了我要分析以下原因, 这个columnset 里面记录的是所有的 结果集列信息, 到头来却还要在实体中遍历, 加上前缀, 再与 columnSet 集合进行比对, 来分析以下报错原因 " Column 'age' not found." 说明主表进来的时候t0.age!=age, 那么如果将入口的""换成"t0", 应该就可以了, 结果还是报错, 因为压根就没有出现 t0.age 这个字段:

SELECT t0.id,
	t0.name,
	t1.id AS department_id,
	t1.name AS department_name,
	t2.name AS company_name FROM employee t0
join department t1 ON t0.d_id = t1.id
join company t2 ON t1.com_id = t2.id
 where t1.name = ?
这是一个只涉及部分信息的dto类, 可是age列此时却被遍历了, 看来此时的 columnSet 集合就是为了避免解决这个问题, 当sql不涉及的字段, 就不处理 (那么这里为什么不else里给当前字段一个默认值, 按道理来讲应该会的) 接着继续解读

字段遍历里分成了两个分支, 实际上赋值的分支实在else if里, join 是做嵌套处理, 让我看看, 首先一上来就获取了 join里面的两个属性, 这里我在想, join注解里是否一个主表外键属性就足够了? 可以让开发者减少一些跳页确认的负担, 但是这样的话处理起来又要复杂一些, 那还是算了吧而且这样也能给开发者心里上的警告"一定要写对主外键的字段名", 后来又获取了当前从表的字段数组, 接着获取了nestedPrefix应该是, 应该是用来给后面的字段做拼接的, 不过这里还这有点渗人, 本身就是一个循环里面的if块 又套了一个循环, 不过没关系因为缓存后查询耗时基本为0-1ms, 现在我关心的是上下文的问题, 因为当前的上下文在定义后没有立即使用而是等一会后才使用让我非常的不爽, 我要看看join的两个属性什么时候才做处理;

继续看定义了一个布尔值anyPresent, 这里=false是为了解决下面的if块报错的问题, 但是纯属多余, 我认为java官方应该处理这种神经病似的对空值的执着,

这个布尔值是用来.. 等等我似乎之前误解了, 之前我认为 "字段遍历里分成了两个分支, 实际上赋值的分支实在else if里, join 是做嵌套处理" 但实际上从表的赋值逻辑全部交给了join分支?
这里遍历每一个从表字段, 在里面拼接了从表前缀+字段名, 接着又与查询列单例集合columnSet比对, 但是这里的比对似乎有些多余, 因为这个方法体里面本身就是 "还原" 查询列的信息的, 如果说从表的字段'没有', 也会被 else-if 块给忽略掉, 所以这个循环似乎本身都没有意义, 这个布尔值也没有意义, 先这样, 等下我再进入下一步

 */
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

        // 缓存并复用字段
        Field[] fields = JoinConvertContext.getFields(clz);

        for (Field field : fields) {
            String snakeName = NamingConvertUtil.camel2SnakeCase(field.getName());
            String colName = prefix + snakeName;

            // 实际上赋值的分支在else if里, if-join 分支是用于递归进入else-if里
            if (field.isAnnotationPresent(Join.class)) {
                Join join = field.getAnnotation(Join.class);
                String fkColumn = join.fk();
                String refColumn = join.refFK();

                Field[] subFields = JoinConvertContext.getFields(clz);


                String nestedPrefix = snakeName + "_";
                // boolean anyPresent = false;
                // for (Field subField : subFields) {
                //     String formatFName = NamingConvertUtil.camel2SnakeCase(subField.getName());
                //     String finalColName = nestedPrefix + formatFName;
                //
                //     // if (columnSet.contains(finalColName)) {
                //         anyPresent = true;
                //         break;
                //     // }
                // }
                //
                // if (anyPresent) {
                    JoinConvertContext.MetaMap nested = analyzeClzStruct(field.getType(), columnSet, nestedPrefix);
                    metaMap.addNestedMeta(field, nested, fkColumn, refColumn);
                // }

            } else if (columnSet.contains(colName)) { // 如果结果集未涉及的字段, 则不处理
                metaMap.addFieldMeta(field, colName);
            }
        }

        return metaMap;
    }


    /**
     * 根据元数据信息构建对象实例并填充字段
     *
     * @param rs        查询结果集
     * @param beanClass 对象类
     * @param metaMap   结构元信息
     * @param <T>       泛型类型
     * @return 填充后的对象实例
     * @throws Throwable 构造或填充过程中的异常
     */
    private <T> T buildJoinBean(ResultSet rs, Class<T> beanClass, JoinConvertContext.MetaMap metaMap) throws Throwable {
        T bean = beanClass.getDeclaredConstructor().newInstance();

        for (Map.Entry<Field, String> entry : metaMap.getFieldMeta().entrySet()) {
            setFieldValue(bean, entry.getKey(), rs, entry.getValue());
        }

        for (Map.Entry<Field, JoinConvertContext.MetaMap> entry : metaMap.getNestedMeta().entrySet()) {
            Field field = entry.getKey();
            JoinConvertContext.MetaMap nestedMeta = entry.getValue();
            String fkCol = metaMap.getFkColumn(field);
            String refCol = metaMap.getRefColumn(field);

            Object fkValue = safeGetObject(rs, fkCol);
            Object refBean = buildJoinBean(rs, field.getType(), nestedMeta);
            if (isAllFieldsNull(refBean, nestedMeta)) continue;

            Field refField = getField(field.getType(), NamingConvertUtil.snake2CamelCase(refCol));
            String nestedCol = NamingConvertUtil.camel2SnakeCase(field.getName()) + "_" + NamingConvertUtil.camel2SnakeCase(refField.getName());

            Object refVal = safeGetObject(rs, nestedCol);
            if (fkValue != null || refVal != null) {
                setFieldValue(refBean, refField, rs, nestedCol);
            }

            setFieldValue(bean, field, refBean);
        }

        return bean;
    }

    /**
     * 安全获取列值
     *
     * <p>尝试从 ResultSet 获取列值, 若抛出 SQLException 则返回 null</p>
     *
     * @param rs      查询结果集
     * @param colName 列名
     * @return 列值或 null
     */
    private Object safeGetObject(ResultSet rs, String colName) {
        try {
            return rs.getObject(colName);
        } catch (SQLException ignored) {
            return null;
        }
    }

}
