

// MetaMap.java
package com.doth.selector.convertor.join;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class MetaMap {
    private final Map<Field, String> fieldMeta = new ConcurrentHashMap<>();
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
    public Map<Field, String> getFieldMeta() { return fieldMeta; }
    public Map<Field, MetaMap> getNestedMeta() { return nestedMeta; }
    public String getFkColumn(Field field) { return fkColumns.get(field); }
    public String getRefColumn(Field field) { return refColumns.get(field); }
}
