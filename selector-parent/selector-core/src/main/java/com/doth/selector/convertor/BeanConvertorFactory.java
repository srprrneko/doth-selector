package com.doth.selector.convertor;

import com.doth.selector.convertor.join.JoinBeanConvertor;
import com.doth.selector.convertor.strict.CommonBeanConvertorLv;
// import com.doth.selector.convertor.join.newJoin.JoinBeanConvertor;

import java.util.EnumMap;
import java.util.Map;

/**
 * 从 resultSet 到 entity 的转换器工厂
 */
public class BeanConvertorFactory {
    // 使用集合装在枚举, 一一对应转换器
    // EnumMap<> : 用于处理枚举类型的键值映射，相比之前的Map，EnumMap 在处理枚举键时减少性能和内存消耗
    private static final Map<ConvertorType, BeanConvertor> instances = new EnumMap<>(ConvertorType.class);

    static {
        // 绑定枚举与转换器
        instances.put(ConvertorType.LIGHT, new CommonBeanConvertorLv()); // 无join的转换器
        instances.put(ConvertorType.JOIN_MAP, new JoinBeanConvertor()); // 多表转换器

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
}