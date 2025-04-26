package com.doth.selector.coordinator.supports.sqlgenerator.tool.newgenerate;

import java.util.List;

public class AliasConvertUtil {

    public static String generateAliases(String originalSql) {
        SqlJoinParser.JoinParseResult joinInfo = SqlJoinParser.parse(originalSql);
        List<String> formattedFields = SelectAliasFormatter.formatSelectFields(
                originalSql, joinInfo.mainTableAlias, joinInfo.aliasToTable, joinInfo.fkColumns
        );

        return originalSql.replaceFirst(
                "(?is)SELECT\\s+.*?\\s+FROM",
                "SELECT " + String.join(", ", formattedFields) + " FROM"
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
