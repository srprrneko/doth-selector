package com.doth.selector.convertor.supports;

import com.doth.selector.convertor.supports.fieldconvertor.FieldConvertor;
import com.doth.selector.convertor.supports.fieldconvertor.FieldConvertorFactory;
import lombok.Getter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 优化后的上下文类，消除字段缓存冗余
 */
public class JoinConvertContext {

    public static final Map<Class<?>, Map<String, MetaMap>> JOIN_CACHE = new ConcurrentHashMap<>();

    // 缓存：字段 setter
    static final Map<Field, MethodHandle> SETTER_CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    // 仅保留字段名到字段的缓存
    public static final Map<Class<?>, Map<String, Field>> FIELD_NAME_CACHE = new ConcurrentHashMap<>();

    /**
     * 所有的字段是否都是空, 浅探测
     */
    public static boolean isAllFieldsNull(Object bean, MetaMap metaMap) {
        try {
            Set<Field> fields = metaMap.getFieldMeta().keySet();
            for (Field f : fields) {
                if (f.get(bean) != null) {
                    return false;
                }
            }
        } catch (IllegalAccessException ignored) {
        }
        return true;
    }

    /**
     * 通过 MethodHandle 给字段赋值
     * 使用标准查找 (findSetter / privateLookupIn) 替换 unreflectSetter，减少反射中间层开销。
     */
    public static void setFieldValue(Object target, Field field, Object value) throws Throwable {
        MethodHandle setter = SETTER_CACHE.computeIfAbsent(field, f -> {
            try {
                // 确保可访问 主要为了 getModifiers / privateLookupIn 前不抛异常
                if (!f.canAccess(target)) {
                    f.setAccessible(true);
                }
                // 准备所处类 满足非游离字段规则, 反射转换底层实际也做了对应的逻辑
                Class<?> declaring = f.getDeclaringClass();
                MethodHandles.Lookup lookup = MethodHandles.lookup();

                // ---- 提升lookup视角: 当前类 >> 所属类
                int mods = f.getModifiers();
                // 如果类或字段不是 public, 则切换到"声明类本身的 Lookup"以获得私有/包级访问权限
                boolean needPrivate =
                        !Modifier.isPublic(declaring.getModifiers()) ||
                                !Modifier.isPublic(mods);

                if (needPrivate) {
                    lookup = MethodHandles.privateLookupIn(declaring, lookup); // 获取访问锁
                }
                // ---- 结束

                // 直接标准方式查找 setter (比 unreflectSetter 少一层 Field -> MH 的转换逻辑)
                return lookup.findSetter(declaring, f.getName(), f.getType());
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException("创建字段setter句柄失败: " + f, e);
            }
        });
        // 使用通用 invoke, 自动适配装拆箱（避免 invokeExact 要求精确签名）
        setter.invoke(target, value);
    }

    /**
     * 带类型转换工厂逻辑 的字段赋值重载
     *
     * @param target      目标类
     * @param field       目标字段
     * @param rs          结果集
     * @param columnLabel 当前处理的查询列列文本
     */
    public static void setFieldValue(Object target, Field field, ResultSet rs, String columnLabel) throws Throwable {
        FieldConvertor convertor = FieldConvertorFactory.getConvertor(field.getType(), field);
        Object value = convertor.convert(rs, columnLabel);
        setFieldValue(target, field, value);
    }

    /**
     * 根据class对象和字段名获取Field对象
     */
    public static Field getField(Class<?> clz, String name) {
        Map<String, Field> map = FIELD_NAME_CACHE.get(clz);
        return (map != null ? map.get(name) : null);
    }

    /**
     * 根据类对象获取Field数组（动态获取，避免额外缓存）
     */
    public static Field[] getFields(Class<?> clz) {
        return FIELD_NAME_CACHE.computeIfAbsent(clz, c -> {
                    Map<String, Field> map = new ConcurrentHashMap<>();

                    for (Field f : c.getDeclaredFields()) {
                        f.setAccessible(true);

                        map.put(f.getName(), f);
                    }
                    return map;
                })
                .values() // Map<String, 'Field'>
                .toArray(new Field[0]); // 使用 T 构造返回 Field[]
    }

    /**
     * 实体类的信息元
     */
    public static class MetaMap {

        @Getter
        private final Map<Field, String> fieldMeta = new ConcurrentHashMap<>();

        @Getter
        private final Map<Field, MetaMap> nestedMeta = new ConcurrentHashMap<>();

        private final Map<Field, String> fkColumns = new ConcurrentHashMap<>();

        private final Map<Field, String> refColumns = new ConcurrentHashMap<>();

        public void addFieldMeta(Field field, String column) {
            fieldMeta.put(field, column);
        }

        public void addNestedMeta(Field field, MetaMap metaMap, String fkColumn, String refColumn) {
            nestedMeta.put(field, metaMap);
            fkColumns.put(field, fkColumn);
            refColumns.put(field, refColumn);
        }

        public String getFkColumn(Field field) {
            return fkColumns.get(field);
        }

        public String getRefColumn(Field field) {
            return refColumns.get(field);
        }
    }
}
