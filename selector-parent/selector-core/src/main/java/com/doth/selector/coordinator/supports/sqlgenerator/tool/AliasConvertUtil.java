package com.doth.selector.coordinator.supports.sqlgenerator.tool;

import java.util.*;
import java.util.regex.*;

public class AliasConvertUtil {

    public static String generateAliases(String originalSql) {
        // 不区分大小写匹配from, \\s+ 匹配一个或多个空格; (\\w+)匹配表名往后全部; 再次\\s匹配一个或多个空格, 准备截取别名; (\\w+)最后匹配表别名
        Matcher fromMatcher = Pattern.compile("from\\s+(\\w+)\\s+(\\w+)(\\s+|$)", Pattern.CASE_INSENSITIVE) // 枚举表示忽略大小写
                .matcher(originalSql); // fromMatcher会保留两组的匹配内容, 如果sql正确的话, 就可以捕获到 [from 空格+] 后面的\\w+表明(组1), 然后空格 \\w+别名(组2)?

        if (!fromMatcher.find()) throw new IllegalArgumentException("无效的 SQL!");
        String mainTableAlias = fromMatcher.group(2); // 获取第二组捕获内容 -> 主表别名

        // 解析所有JOIN表别名与ON条件
        Map<String, String> aliasToTable = new HashMap<>(); // 表别名对应表名的集合
        Set<String> fkColumns = new HashSet<>(); // 存储所有外键列


        // "FROM employee e JOIN department d ON ... JOIN company c ON ..."
        // 拆分结果: ["FROM employee e", "department d ON ...", "company c ON ..."]

        String[] joins = originalSql.split("(?i)\\s+(?:(?:LEFT|RIGHT|INNER|OUTER)\\s+)?JOIN\\s+");

        for (int i = 1; i < joins.length; i++) { // 跳过第一个非JOIN部分 [from 主表部分]
            String joinPart = joins[i]; // JOIN 往后部分

            // 提取表别名    假设去掉join: department d ON e.d_id = d.id AND d.active = 1
            Matcher tableMatcher = Pattern.compile("^(\\w+)\\s+(\\w+)\\s+(?:ON|on)\\s+(.+)", Pattern.CASE_INSENSITIVE) // 四个括号, 对应三组, 和一个非捕获组(?:ON|on) 代表匹配带有on的字符串, 自身不包含在内
                    .matcher(joinPart); // 此时join往后部分以on为拆分, 也就是[join与on之间]以及on之后, 获取 表名(组1w+) 与 表别名(组2+) 还有 组3(on之后的所有内容)
            if (!tableMatcher.find()) continue;

            String tableName = tableMatcher.group(1); // 表名
            String alias = tableMatcher.group(2); // 表别名
            aliasToTable.put(alias, tableName); // 此次循环从 从表开始, 存储从表的别名以及表名

            // 第三组, 没有了表名和表别名, on 部分全占
            // 从第三组中, 解析ON条件, 用于获取用于连接的外键列, 避免使用反射
            String onClause = tableMatcher.group(3).split("(?i)\\s+WHERE\\s+")[0]; // 提取on子句, 以where分割, 只取第一个(where前, on后的连接条件 [e.d_id = d.id])

            // 遍历on连接外键列
            for (String condition : onClause.split("(?i)\\s+(AND|OR)\\s+")) { // 排除on子句中的 AND/OR 进行遍历, 确保遍历的是on子句中的条件
                String[] parts = condition.split("\\s*=\\s*"); // 获取等号两侧连接的列,
                if (parts.length < 2) continue;
                // 使用去重的集合进行存储, 避免获取同一个外键
                fkColumns.add(parts[0].trim()); // parts[0]代表仅仅获取左侧, 也就是外键列, 所以当前方法必须符合规范: 左侧外键=右侧主键
            }
        }

        // 给 select 语句加别名
        // 处理SELECT字段

        // 大小写验证
        Pattern selectPattern = Pattern.compile("(?i)select\\s+(.*?)\\s+from");
        Matcher selectMatcher = selectPattern.matcher(originalSql);
        if (!selectMatcher.find()) throw new IllegalArgumentException("无效的 sqlgenerator!");
        // String selectColPart = originalSql.substring(originalSql.indexOf("select") + 6, originalSql.indexOf("from")).trim(); // 从select开始获取, 到from结束 (所有的查询字段)
        String selectColPart = selectMatcher.group(1).trim();
        List<String> processedFields = new ArrayList<>(); // 用于存储最终返回的字段: d.name -> department_name; 主表不处理: e.id -> e.id

        for (String field : selectColPart.split(",\\s*")) { // 通过逗号进行分割, 遍历处理每一个列
            field = field.trim(); // 清空左右空格
            if (!field.contains(".")) { // 简单字段
                processedFields.add(field); // 直接添加
                continue;
            }

            // 处理 代表别名和列名
            String[] parts = field.split("\\."); // 通过.进行分割
            String tableAlias = parts[0]; // 表别名
            String column = parts[1]; // 列名
            // 示例：字段d.name分割后：
            // parts[0] = "d"
            // parts[1] = "name"

            if (tableAlias.equals(mainTableAlias)) { // 如果是主表字段
                processedFields.add(field); // 直接添加 (不做处理)
            } else if (fkColumns.contains(field)) { // 如果是外键列
                processedFields.add(field); // 直接添加 (不做处理)
            } else { // 仅处理从表非外键字段
                String tableName = aliasToTable.get(tableAlias); // 通过表别名获取表名
                if (tableName != null) {
                    // 起别名
                    String aliasName = tableName.toLowerCase() + "_" + column; // 表名 + '_' + 列名
                    processedFields.add(field + " AS " + aliasName); // 存储
                } else {
                    // 代表不是 表别名, 直接添加 (通常不太可能, 防御手段)
                    processedFields.add(field);
                }
            }
        }

        return originalSql.replaceFirst(
                "(?is)SELECT\\s+.*?\\s+FROM", // 添加 (?s) 支持跨行，严格匹配空格
                "SELECT " + String.join(", \n\t", processedFields) + " FROM"
        );
    }

    public static void main(String[] args) {
        String originalSql = "select t0.id, t0.name, t0.d_id, t1.name, t1.com_id, t2.name\n" +
                "from employee t0\n" +
                "join department t1 ON t0.d_id = t1.id\n" +
                "join company t2 ON t1.com_id = t2.id";

        System.out.println(generateAliases(originalSql));
    }
}