package com.doth.selector.common.dto;

import java.util.*;

@Deprecated // 迭代遗留, 后续移除
public class DTOFactory {
    private static final Map<Class<?>, Map<String, Class<?>>> REGISTRY = new HashMap<>();

    public static void register(Class<?> entityClass, String id, Class<?> dtoClass) {
        REGISTRY.computeIfAbsent(entityClass, k -> new HashMap<>()).put(id, dtoClass);
    }

    /**
     * 注册dto并获取
     * @param entityClass 原类
     * @param id id
     * @return dto
     */
    public static Class<?> resolve(Class<?> entityClass, String id) {
        if (id == null || id.isBlank()) return entityClass;

        Map<String, Class<?>> idMap = REGISTRY.get(entityClass);
        if (idMap == null || !idMap.containsKey(id)) {
            // 强制尝试加载 class 并触发 static 注册
            try {
                String fullName = entityClass.getName() + "$" + id + "DTO";
                ClassLoader cl = entityClass.getClassLoader(); // 使用从属类的加载器
                Class.forName(fullName, true, cl);
                // Class<?> generatedDTO = Class.forName(fullName, true, cl);

                // idMap = REGISTRY.get(entityClass);
                // if (idMap == null || !idMap.containsKey(id)) {
                //     throw new IllegalStateException("DTO类加载成功但未注册，请确认 static 注册代码是否写入: " + fullName);
                // }
                // System.out.println("generatedDTO = " + generatedDTO);
                // return generatedDTO;

            } catch (ClassNotFoundException e) {
                throw new RuntimeException("DTO类未找到: " + entityClass.getName() + " id=" + id, e);
            }
        }

        return REGISTRY.getOrDefault(entityClass, Collections.emptyMap()).getOrDefault(id, entityClass); // 退回实体类型
    }


}
