package com.doth.selector.coordinator.supports.convertor.join;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 类结构映射描述 (仅限包内可以访问)
 *  映射指挥图, 第一次进来时开始创建, 第一次为主表所有非联查字段,
 *
 *  为什么类名不适用更为表示多级嵌套结构的类名 MetaMaps, 因为MetaMap 更能表示这是一个集体一个整体, 而加上了s更为表示这是多个独立的集, 而这里显然要强调整体, 因此采用整体
 *
 * 结构说明：
 * - fieldMeta : 普通字段映射（字段 -> 结果集列名）
 * - nestedMeta : 关联字段映射（字段 -> 嵌套类映射）
 * - fkColumns : 外键列名映射（关联字段 -> 外键列名） 引用
 * - refColumns : 引用列名映射（关联字段 -> 引用列名） 被引用
 *  jpa
 *          String sql = "SELECT " +
 *                 "e.id, " +
 *                 "e.name, " +
 *                 "e.d_id," + --> 外键
 *                 "d.name, " + --> as department_name
 *                 "d.com_id, " +
 *                 "c.name " + --> as company_name
 *                 "FROM employee e " +
 *                 "JOIN department d ON e.d_id = d.id " +
 *                 "JOIN company c on d.com_id = c.id " +
 *                 "where d.id = ?";
 */
class MetaMap {

    /* 普通字段元, <字段, 列名> */
    private final Map<Field, String> fieldMeta = new HashMap<>();

    /* 嵌套结构元, <字段, 结构元> */
    private final Map<Field, MetaMap> nestedMeta = new HashMap<>();

    /* 外键 例: emp.d_id, <字段, 外键列> */
    private final Map<Field, String> fkColumns = new HashMap<>();

    /* 引用主键 例: dep{id}, <字段, 引用主键> */
    private final Map<Field, String> refColumns = new HashMap<>();

    /**
     * 添加字段
     * @param field 字段
     * @param column 对应列
     */
    public void addFieldMeta(Field field, String column) {
        fieldMeta.put(field, column);
    }

    /**
     * 添加嵌套结构
     * @param field 字段
     * @param metaMap 结构元
     * @param fkColumn 外键
     * @param refColumn 引用主键
     */
    public void addNestedMeta(Field field, MetaMap metaMap, String fkColumn, String refColumn) {
        nestedMeta.put(field, metaMap);
        fkColumns.put(field, fkColumn);
        refColumns.put(field, refColumn);
    }


    /**
     * 获取字段与其元数据字符串的映射
     *
     * @return 包含字段和其对应元数据字符串的映射
     */
    public Map<Field, String> getFieldMeta() { return fieldMeta; }

    /**
     * 获取字段与嵌套元数据映射的映射
     *
     * @return 包含字段和其对应嵌套元数据映射的映射
     */
    public Map<Field, MetaMap> getNestedMeta() { return nestedMeta; }

    /**
     * 根据字段获取外键列名
     *
     * @param field 数据库字段对象
     * @return 对应字段的外键列名，如果不存在则返回null
     */
    public String getFkColumn(Field field) { return fkColumns.get(field); }

    /**
     * 根据字段获取引用列名
     *
     * @param field 数据库字段对象
     * @return 对应字段的引用列名，如果不存在则返回null
     */
    public String getRefColumn(Field field) { return refColumns.get(field); }

}