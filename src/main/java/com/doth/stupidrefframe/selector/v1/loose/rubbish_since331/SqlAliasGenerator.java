package com.doth.stupidrefframe.selector.v1.loose.rubbish_since331;

import java.util.*;
import java.util.regex.*;

@Deprecated
public class SqlAliasGenerator {

    /**
     * 生成自动别名的SQL
     * @param originalSql 原始SQL
     * @param mainTableAlias 主表别名（如"e"）
     * @param joinConfig JOIN表的配置：key=表别名，value=对应的类名（如 {"d": "Department", "c": "Company"}）
     * @param fkColumns 外键字段配置：key=表别名，value=该表的外键字段集合（如 {"d": ["com_id"]}）
     */
    public static String generate(String originalSql, 
                                  String mainTableAlias, 
                                  Map<String, String> joinConfig,
                                  Map<String, Set<String>> fkColumns) {
        // 1. 提取SELECT部分
        String[] parts = originalSql.split("(?i)\\s+FROM\\s+", 2);
        String selectClause = parts[0].replace("SELECT", "").trim();
        String fromClause = "FROM " + parts[1];

        // 2. 分割所有查询字段
        List<String> columns = new ArrayList<>();
        Matcher matcher = Pattern.compile("([^,]+(\\s+\\$[^)]+\\$)?)").matcher(selectClause);
        while (matcher.find()) {
            columns.add(matcher.group(1).trim());
        }

        // 3. 处理每个字段
        StringBuilder newSelect = new StringBuilder("SELECT ");
        for (String col : columns) {
            if (col.contains(".")) { // 处理带别名的字段
                String[] partsCol = col.split("\\.");
                String tableAlias = partsCol[0];
                String field = partsCol[1].replaceAll("\\s+.*", ""); // 去除函数等复杂表达式

                if (tableAlias.equals(mainTableAlias)) { 
                    // 主表字段：直接保留
                    newSelect.append(col).append(", ");
                } else if (joinConfig.containsKey(tableAlias)) { 
                    // JOIN表字段：检查是否外键
                    boolean isFk = fkColumns.getOrDefault(tableAlias, Collections.emptySet()).contains(field);
                    if (isFk) {
                        newSelect.append(col).append(", "); // 外键字段保留原样
                    } else {
                        // 生成别名：类名蛇形化 + "_" + 字段名
                        String className = joinConfig.get(tableAlias);
                        String snakeName = camelToSnake(className);
                        newSelect.append(col).append(" AS ").append(snakeName).append("_").append(field).append(", ");
                    }
                } else {
                    newSelect.append(col).append(", "); // 未知表保留原样
                }
            } else { 
                // 无别名字段（如聚合函数）：保留原样
                newSelect.append(col).append(", ");
            }
        }

        // 4. 拼接最终SQL
        newSelect.delete(newSelect.length()-2, newSelect.length()); // 删除末尾逗号
        return newSelect.toString() + " " + fromClause;
    }

    // 驼峰转蛇形命名（如 "Department" -> "department"）
    private static String camelToSnake(String str) {
        return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    // 示例用法
    // public static void core(String[] args) {
    //     String originalSql = "SELECT e.id, e.name, e.d_id, d.name, d.com_id, c.name " +
    //                          "FROM employee e " +
    //                          "JOIN department d ON e.d_id = d.id " +
    //                          "JOIN company c ON d.com_id = c.id " +
    //                          "WHERE d.id = ?";
    //
    //     // JOIN表配置
    //     Map<String, String> joinConfig = new HashMap<>();
    //     joinConfig.put("d", "Department");
    //     joinConfig.put("c", "Company");
    //
    //     // 外键字段配置（d表的com_id是外键）
    //     Map<String, Set<String>> fkColumns = new HashMap<>();
    //     fkColumns.put("d", new HashSet<>(Arrays.asList("com_id")));
    //
    //     String newSql = generate(originalSql, "e", joinConfig, fkColumns);
    //     System.out.println(newSql);
    // }
}