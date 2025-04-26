package com.doth.selector.coordinator.supports.sqlgenerator.tool.newgenerate;

import java.util.*;
import java.util.regex.*;

public class SqlJoinParser {

    public static class JoinParseResult {
        public final String mainTableAlias;
        public final Map<String, String> aliasToTable;
        public final Set<String> fkColumns;

        public JoinParseResult(String mainAlias, Map<String, String> aliasMap, Set<String> fkCols) {
            this.mainTableAlias = mainAlias;
            this.aliasToTable = aliasMap;
            this.fkColumns = fkCols;
        }
    }

    public static JoinParseResult parse(String originalSql) {
        Matcher fromMatcher = Pattern.compile("from\\s+(\\w+)\\s+(\\w+)(\\s+|$)", Pattern.CASE_INSENSITIVE)
                .matcher(originalSql);
        if (!fromMatcher.find()) throw new IllegalArgumentException("无效的 SQL：未找到 FROM 主表部分");

        String mainTableAlias = fromMatcher.group(2);
        Map<String, String> aliasToTable = new HashMap<>();
        Set<String> fkColumns = new HashSet<>();

        String[] joins = originalSql.split("(?i)\\s+(?:(?:LEFT|RIGHT|INNER|OUTER)\\s+)?JOIN\\s+");
        for (int i = 1; i < joins.length; i++) {
            String joinPart = joins[i];

            Matcher tableMatcher = Pattern.compile("^(\\w+)\\s+(\\w+)\\s+(?:ON|on)\\s+(.+)", Pattern.CASE_INSENSITIVE)
                    .matcher(joinPart);
            if (!tableMatcher.find()) continue;

            String tableName = tableMatcher.group(1);
            String alias = tableMatcher.group(2);
            aliasToTable.put(alias, tableName);

            String onClause = tableMatcher.group(3).split("(?i)\\s+WHERE\\s+")[0];
            for (String condition : onClause.split("(?i)\\s+(AND|OR)\\s+")) {
                String[] parts = condition.split("\\s*=\\s*");
                if (parts.length >= 2) {
                    fkColumns.add(parts[0].trim());
                }
            }
        }

        return new JoinParseResult(mainTableAlias, aliasToTable, fkColumns);
    }
}
