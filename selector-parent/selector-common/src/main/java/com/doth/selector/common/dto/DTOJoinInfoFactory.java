// src/main/java/com/doth/selector/supports/sqlgenerator/dto/DTOJoinInfoFactory.java
package com.doth.selector.common.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 存储所有由注解处理器在编译期自动生成的 DTOJoinInfo。
 */
public class DTOJoinInfoFactory {
    // outer key: entityClass.getName(), inner key: dtoName
    private static final Map<String, Map<String, DTOJoinInfo>> registry = new HashMap<>();

    /**
     * 编译期注解处理器生成的注册调用会变成：
     * DTOJoinInfoFactory.register(Employee.class, "baseEmpInfo", new DTOJoinInfo(...));
     */
    public static void register(Class<?> entityClass,
                                String dtoName,
                                DTOJoinInfo info) {
        registry
                .computeIfAbsent(entityClass.getName(), k -> new HashMap<>())
                .put(dtoName, info);
    }

    public static DTOJoinInfo getJoinInfo(Class<?> entityClass, String dtoName) {
        Map<String, DTOJoinInfo> m = registry.get(entityClass.getName());
        return (m != null ? m.get(dtoName) : null);
    }

    /**
     * 只读视图，用于调试或测试。
     */
    public static Map<String, Map<String, DTOJoinInfo>> all() {
        return Collections.unmodifiableMap(registry);
    }
}
