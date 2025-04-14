package com.doth.selector.coordinator.supports.convertor;

import com.doth.selector.coordinator.supports.convertor.join.JoinBeanConvertor;
import com.doth.selector.coordinator.supports.convertor.strict.CommonBeanConvertorLv;
import com.doth.selector.coordinator.supports.convertor.strict.CommonBeanConvertor;

import java.util.EnumMap;
import java.util.Map;

/**
 * 转换器工厂 - 采用工厂模式管理不同类型的Bean转换器
 */
public class BeanConvertorFactory {
    // 使用集合装在枚举, 一一对应转换器
    // EnumMap<> : 用于处理枚举类型的键值映射，相比于其他常见的 Map ，EnumMap 在处理枚举键时提供了更好的性能和更少的内存占用
    private static final Map<ConvertorType, BeanConvertor> instances = new EnumMap<>(ConvertorType.class);

    static {
        // 绑定枚举与转换器
        // instances.put(ConvertorType.MULTI_JOIN, new JoinBeanConvertorPro()); // 多表连接
        instances.put(ConvertorType.LIGHT, new CommonBeanConvertorLv()); // 轻量级 light_version
        instances.put(ConvertorType.JOIN_MAP, new JoinBeanConvertor()); // 多表转换器
        instances.put(ConvertorType.STRICT, new CommonBeanConvertor());
        instances.put(ConvertorType.LENIENT, null); // 或许可扩展

    }

    /**
     * 根据类型获取转换器
     * @param type 转换器类型（严格/宽松...）
     * @return 对应的转换器实例
     */
    public static BeanConvertor getConvertor(ConvertorType type) {
        BeanConvertor convertor = instances.get(type);
        if (convertor == null) { // 防御
            throw new IllegalArgumentException("不支持的转换类型: " + type);
        }
        return convertor;
    }

    // 默认获取严格转换器
    // public static BeanConvertor getDefaultConvertor() {
    //     return getConvertor(ConvertorType.STRICT);
    // }
}