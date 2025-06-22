package com.doth.selector.common.dto;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DTO 字段路径选择器工厂：
 * 存储每个 dtoId 对应的字段列表（select 字段）。
 * 供 SQL 生成器在生成 SELECT 子句时使用。
 */
public class DTOSelectFieldsListFactory {

    /**
     * 一级 key：原始实体类
     * 二级 key：dtoId
     * 值：字段路径集合（如 t0.id, t1.depName）
     */
    private static final Map<Class<?>, Map<String, List<String>>> SELECT_FIELD_REGISTRY = new ConcurrentHashMap<>();

    /**
     * 注册 DTO 的查询字段路径列表
     *
     * @param entityClass 原始实体类
     * @param dtoId       DTO 构造方法标识
     * @param selectList  该 DTO 的查询字段列表，必须是完整的字段路径，如 t0.id
     */
    public static void register(Class<?> entityClass, String dtoId, List<String> selectList) {
        // System.out.println("selectList = " + selectList);
        // System.out.println("触发了哦");
        if (entityClass == null || dtoId == null || selectList == null) return;
        List<String> put = SELECT_FIELD_REGISTRY
                .computeIfAbsent(entityClass, k -> new ConcurrentHashMap<>())
                .put(dtoId, Collections.unmodifiableList(new ArrayList<>(selectList)));
        // System.out.println("put = " + put);
    }

    /**
     * 获取 DTO 的字段路径列表
     *
     * @param entityClass 原始实体类
     * @param dtoId       DTO 构造方法标识
     * @return 查询字段路径列表；如果未找到则返回空列表
     */
    public static List<String> resolveSelectList(Class<?> entityClass, String dtoId) {
        if (entityClass == null || dtoId == null) return Collections.emptyList();
        return SELECT_FIELD_REGISTRY
                .getOrDefault(entityClass, Collections.emptyMap())
                .getOrDefault(dtoId, Collections.emptyList());
    }

    /**
     * 判断是否存在该 DTO 的字段列表（用于校验）
     *
     * @param entityClass 实体类
     * @param dtoId       DTO 标识
     * @return 是否存在
     */
    public static boolean contains(Class<?> entityClass, String dtoId) {
        return SELECT_FIELD_REGISTRY.containsKey(entityClass) &&
               SELECT_FIELD_REGISTRY.get(entityClass).containsKey(dtoId);
    }

    /**
     * 清除所有注册（一般用于测试）
     */
    public static void clearAll() {
        SELECT_FIELD_REGISTRY.clear();
    }
}
