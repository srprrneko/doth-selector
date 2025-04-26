package com.doth.selector.coordinator.supports.sqlgenerator.tool.newgenerate;

import java.util.*;
import java.util.regex.*;

public class SelectAliasFormatter {

    public static List<String> formatSelectFields(String originalSql, String mainAlias,
                                                  Map<String, String> aliasToTable,
                                                  Set<String> fkColumns) {

        Pattern selectPattern = Pattern.compile("(?i)select\\s+(.*?)\\s+from", Pattern.DOTALL);
        Matcher selectMatcher = selectPattern.matcher(originalSql);
        if (!selectMatcher.find()) {
            throw new IllegalArgumentException("无效的 SQL：无法识别 SELECT-FROM 部分");
        }

        String selectColPart = selectMatcher.group(1).trim();
        List<String> processedFields = new ArrayList<>();

        for (String field : selectColPart.split(",\\s*")) {
            field = field.trim();
            if (!field.contains(".")) {
                processedFields.add(field);
                continue;
            }

            String[] parts = field.split("\\.");
            String tableAlias = parts[0];
            String column = parts[1];

            if (tableAlias.equals(mainAlias) || fkColumns.contains(field)) {
                processedFields.add(field);
            } else {
                String tableName = aliasToTable.get(tableAlias);
                if (tableName != null) {
                    String aliasName = tableName.toLowerCase() + "_" + column;
                    processedFields.add(field + " AS " + aliasName);
                } else {
                    processedFields.add(field);
                }
            }
        }

        return processedFields;
    }
}
