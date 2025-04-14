package com.doth.selector.coordinator.supports.convertor.join;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 类结构映射描述（内部类）
 *  映射指挥图, 第一次进来时开始创建, 第一次为主表所有非联查字段,
 *
 *  为什么类名不适用更为表示多级嵌套结构的类名 MetaMaps, 因为MetaMap 更能表示这是一个集体一个整体, 而加上了s更为表示这是多个独立的集, 而这里显然要强调整体, 因此采用整体
 *
 * 结构说明：
 * - fieldMeta : 普通字段映射（字段 -> 结果集列名）
 * - nestedMeta : 关联字段映射（字段 -> 嵌套类映射）
 * - fkColumns : 外键列名映射（关联字段 -> 外键列名） 引用
 * - refColumns : 引用列名映射（关联字段 -> 引用列名） 被引用
 */
class MetaMap {
    private final Map<Field, String> fieldMeta = new HashMap<>();
    private final Map<Field, MetaMap> nestedMeta = new HashMap<>();
    private final Map<Field, String> fkColumns = new HashMap<>();
    private final Map<Field, String> refColumns = new HashMap<>();

    public void addFieldMeta(Field field, String column) {
        fieldMeta.put(field, column);
    }

    public void addNestedMeta(Field field, MetaMap mapping, String fkColumn, String refColumn) {
        nestedMeta.put(field, mapping);
        fkColumns.put(field, fkColumn);
        refColumns.put(field, refColumn);
    }

    // Getter方法保持原样
    public Map<Field, String> getFieldMeta() { return fieldMeta; }
    public Map<Field, MetaMap> getNestedMeta() { return nestedMeta; }
    public String getFkColumn(Field field) { return fkColumns.get(field); }
    public String getRefColumn(Field field) { return refColumns.get(field); }
}