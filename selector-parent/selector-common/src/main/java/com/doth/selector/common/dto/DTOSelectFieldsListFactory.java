package com.doth.selector.common.dto;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DTO 字段路径选择器工厂：
 * 存储每个 dtoId 对应的字段列表（select 字段）。
 * 供 SQL 生成器在生成 SELECT 子句时使用。
 */
@Slf4j
public class DTOSelectFieldsListFactory {

    /**
     * <p>快速检索, 先找出已经注册过的 实体, 然后在内层 map 中通过 dtoId 区分不同的 查询列列表</p>
     * 1k：原始实体类
     * 2k：dtoId
     * 值：字段路径集合（如 t0.id, t1.depName）
     */
    private static final Map<Class<?>, Map<String, List<String>>> FIELDS_CONTAINER = new ConcurrentHashMap<>();

    /**
     * 注册 DTO 的查询字段路径列表
     *
     * @param entityClass 原始实体类
     * @param dtoId       DTO 构造方法标识
     * @param selectList  该 DTO 的查询字段列表，必须是完整的字段路径，如 t0.id
     */
    public static void register(Class<?> entityClass, String dtoId, List<String> selectList) {
        // System.out.println("selectList = " + selectList);
        log.info("查询字段正在注册.. 字段信息: {}", selectList);
        if (entityClass == null || dtoId == null || selectList == null) return;
        List<String> put = FIELDS_CONTAINER
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
        if (entityClass == null || dtoId == null) {
            List<String> strings = new ArrayList<>();
            log.warn("entity class or dtoId is null!!");
            return strings;
        }
        return FIELDS_CONTAINER
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
        return FIELDS_CONTAINER.containsKey(entityClass) &&
                FIELDS_CONTAINER.get(entityClass).containsKey(dtoId);
    }

    /**
     * 获取指定实体类的所有 DTOId 和对应的查询列列表
     * <p>旨在 获取 实体下的 map, 然后遍历这个map填充到一个 list<map<list>>> 里去</p>
     *
     * @param entityClass 实体类
     * @return 所有 DTOId 和查询字段列表的集合
     */
    public static List<Map<String, List<String>>> getFList4Entity(Class<?> entityClass) {
        if (entityClass == null) return Collections.emptyList();

        Map<String, List<String>> dtoSelectMap = FIELDS_CONTAINER.getOrDefault(entityClass, Collections.emptyMap());

        List<Map<String, List<String>>> result = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : dtoSelectMap.entrySet()) {
            Map<String, List<String>> dtoEntry = new HashMap<>();

            dtoEntry.put(entry.getKey(), entry.getValue());
            result.add(dtoEntry);
        }

        return result;
    }


    /**
     * 清除所有注册
     */
    public static void clearAll() {
        FIELDS_CONTAINER.clear();
    }
}
