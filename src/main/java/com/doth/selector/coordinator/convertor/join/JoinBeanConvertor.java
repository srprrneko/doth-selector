package com.doth.selector.coordinator.convertor.join;

import com.doth.selector.anno.DependOn;
import com.doth.selector.anno.Join;
import com.doth.selector.common.convertor.ValueConverterFactory;
import com.doth.selector.common.testbean.join.Employee;
import com.doth.selector.coordinator.convertor.BeanConvertor;
import com.doth.selector.common.util.TypeResolver;
// import com.doth.selector.dto.DtoStackResolver;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * JoinBeanConvertor 已支持 @DependOn 注解：
 * 1. 若 beanClass 标注了 @DependOn(clzPath="...")，则先反射出真正的实体类；
 * 2. 按照原逻辑把 ResultSet 映射到该实体；
 * 3. 构建完实体后，若存在 @DependOn，就寻找 beanClass 中形参为实体类型的构造方法，
 *    并用刚才映射出的实体作为参数新建 DTO，最后返回 DTO 实例。
 */
public class JoinBeanConvertor implements BeanConvertor {

    /** 缓存：原始 beanClass 对应的映射元信息（最终用于 buildJoinBean） */
    private static final Map<Class<?>, MetaMap> JOIN_CACHE = new HashMap<>();

    /** 缓存：字段对应的 setter MethodHandle（与原逻辑一致） */
    private static final Map<Field, MethodHandle> SETTER_CACHE = new HashMap<>();

    /**
     * 新增：DTO 构造器缓存，Key 为 DTO 类型，Value 为接收实体的单参构造器
     */
    private static final Map<Class<?>, Constructor<?>> DTO_CTOR_CACHE = new HashMap<>();

    @Override
    public <T> T convert(ResultSet rs, Class<T> beanClass) throws Throwable {
        System.out.println("beanClass = " + beanClass);
        try {
            Class<?> effectiveClass = beanClass;
            boolean isDto = false;

            if (beanClass.isAnnotationPresent(DependOn.class)) {
                DependOn depend = beanClass.getAnnotation(DependOn.class);
                effectiveClass = Class.forName(depend.clzPath());
                System.out.println("effectiveClass = " + effectiveClass);
                isDto = true;
            }

            ResultSetMetaData meta = rs.getMetaData();
            MetaMap metaMap = JOIN_CACHE.computeIfAbsent(effectiveClass, clz -> {
                try {
                    return analyzeClzStruct(clz, meta, "");
                } catch (Exception e) {
                    throw new RuntimeException("实体结构分析失败: " + clz.getName(), e);
                }
            });

            @SuppressWarnings("unchecked")
            Object entityInstance = buildJoinBean(rs, effectiveClass, metaMap);

            if (!isDto) {
                @SuppressWarnings("unchecked")
                T result = (T) entityInstance;
                return result;
            }
            Employee emp = (Employee)entityInstance;
            System.out.println("emp = " + emp);

            // ====== 以下部分改为先查缓存，再反射 ======
            Constructor<?> ctor = DTO_CTOR_CACHE.get(beanClass);
            if (ctor == null) {
                // 第一次才去 getConstructor，找不到会抛异常
                ctor = beanClass.getConstructor(effectiveClass);
                // 强制可访问，以防构造器不是 public
                ctor.setAccessible(true);
                DTO_CTOR_CACHE.put(beanClass, ctor);
            }
            @SuppressWarnings("unchecked")
            T dtoInstance = (T) ctor.newInstance(entityInstance);
            return dtoInstance;
        } catch (RuntimeException e){
            throw new RuntimeException("result >> 实体 转换失败!" + e);
        }
    }

    /**
     * 解析类字段结构，支持嵌套结构
     */
    private MetaMap analyzeClzStruct(Class<?> clz, ResultSetMetaData meta, String prefix) throws Exception {
        MetaMap metaMap = new MetaMap();

        for (Field field : clz.getDeclaredFields()) {
            field.setAccessible(true);

            Join join = field.getAnnotation(Join.class);
            if (join != null) {
                String fkColumn = join.fk();
                String refColumn = join.refFK();

                if (columnExists(meta, fkColumn)) {
                    Class<?> refClass = field.getType();
                    String nestedPrefix = field.getName() + "_";
                    MetaMap refMapping = analyzeClzStruct(refClass, meta, nestedPrefix);
                    metaMap.addNestedMeta(field, refMapping, fkColumn, refColumn);
                }
            } else {
                String columnName = prefix + field.getName();
                if (columnExists(meta, columnName)) {
                    metaMap.addFieldMeta(field, columnName);
                }
            }
        }

        return metaMap;
    }

    /**
     * 字段是否存在于结果集
     */
    private boolean columnExists(ResultSetMetaData meta, String columnName) throws SQLException {
        int columnCount = meta.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            if (meta.getColumnLabel(i).equalsIgnoreCase(columnName)) {
                return true;
            }
        }
        return false;
    }


    /**
     * 构建嵌套对象（递归）
     */
    private <T> T buildJoinBean(ResultSet rs, Class<T> beanClass, MetaMap metaMap) throws Throwable {
        // // 1. 通过调用栈查找是否启用了 UseDTO 注解
        // String dtoId = DtoStackResolver.resolveDTOIdFromStack();
        //
        // // 2. 尝试使用 DTOFactory 获取子类，如果没有，则退回使用原类型
        // Class<?> actualClass = DTOFactory.resolve(beanClass, dtoId);
        // T bean = (T) actualClass.getDeclaredConstructor().newInstance();

        T bean = beanClass.getDeclaredConstructor().newInstance(); // 初始化主表

        // System.out.println("actualClass = " + actualClass);


        for (Map.Entry<Field, String> entry : metaMap.getFieldMeta().entrySet()) {
            Object val = rs.getObject(entry.getValue());
            setFieldValue(bean, entry.getKey(), val);
        }

        for (Map.Entry<Field, MetaMap> entry : metaMap.getNestedMeta().entrySet()) {
            Field field = entry.getKey();
            MetaMap nestedMeta = entry.getValue();

            String fkColumn = metaMap.getFkColumn(field);
            String refColumn = metaMap.getRefColumn(field);
            Object fkValue = rs.getObject(fkColumn);

            Object refBean = buildJoinBean(rs, field.getType(), nestedMeta);
            Field refField = getField(field.getType(), refColumn);
            if (refField != null) {
                setFieldValue(refBean, refField, fkValue);
            }
            setFieldValue(bean, field, refBean);
        }

        return bean;
    }

    /**
     * 字段赋值：支持默认值容错
     */
    private void setFieldValue(Object target, Field field, Object value) {
        MethodHandle setter = SETTER_CACHE.computeIfAbsent(field, f -> {
            try {
                return MethodHandles.lookup().unreflectSetter(f);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            setter.invoke(target, value);
        } catch (Throwable e1) {
            try {
                setter.invoke(target, ValueConverterFactory.convertIfPossible(field.getType(), value));
            } catch (Throwable e2) {
                try {
                    setter.invoke(target, TypeResolver.getDefaultValue(field.getType()));
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /**
     * 获取字段
     */
    private Field getField(Class<?> clz, String name) {
        for (Field field : clz.getDeclaredFields()) {
            if (field.getName().equals(name)) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }
}
