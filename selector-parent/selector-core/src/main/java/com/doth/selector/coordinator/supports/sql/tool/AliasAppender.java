package com.doth.selector.coordinator.supports.sql.tool;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * sql 的别名附着器, 不建议外部直接调用
 */
public class AliasAppender {

    // 预编译正则模式
    /**
     * 用于捕获主表别名
     * <p>例: FROM employee t0 >> "t0"</p>
     */
    private static final Pattern FROM_PAT = Pattern.compile(
            "FROM\\s+\\w+\\s+(\\w+)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 遍历并提取所有 JOIN ... ON ... 段的信息
     * <p>捕获组:
     *   <ol>
     *     <li>group(1): 从表名, 如 "department"</li>
     *     <li>group(2): 从表别名, 如 "d"</li>
     *     <li>group(3): ON 后的全部</li>
     *   </ol:
     * </p>
     * <p>示例匹配:</p>
     * "INNER JOIN users u ON u.id = o.user_id"
     * <p>
     * <p>(1) "users"</p>
     * <p>(2) "u"<p/>
     * <p>(3) "u.id = o.user_id"</p>
     * <p>关系: 在 FROM_PAT 之后执行, 解析出从表信息和 ON 条件, 结果被 COND_SPLIT/EQ_SPLIT 处理提取外键列</p>
     */
    private static final Pattern JOIN_PAT = Pattern.compile(
            "(?:LEFT|RIGHT|INNER|OUTER)?JOIN\\s+" +
                    // employee' 't0' '
                    "(\\w+)\\s+" +
                    "(\\w+)\\s+" +
                    "ON\\s+" + "([^;]+?)" + // t0.user_id = t1.id
                    "(?=" +
                    "(?:LEFT|RIGHT|INNER|OUTER)?" +
                    "JOIN|$" +
                    ")", // 直到下一个 JOIN 或 SQL 末尾
            Pattern.CASE_INSENSITIVE
    );


    /**
     * 提取 SELECT 与 FROM 之间的所有字段列表
     * <p>捕获组: group(1) 返回查询列列表, 如 "t0.id, t1.name, ..."</p>
     * <p>关系: 与 JOIN_PAT 并行执行, 产出查询列列表供后续处理并重写 SELECT 子句</p>
     */
    private static final Pattern SELECT_PAT = Pattern.compile(
            "SELECT\\s+(.*?)\\s+FROM",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL // DOTALL 支持换行\缩进
    );


    /**
     * 将 ON 子句中的复合条件按 <strong>AND/OR</strong> 进行切分
     * <p>目的: 把 <strong>t0.id = t1.id AND t1.active = 1</strong> 等复合条件拆成单条条件, 方便后续用 EQ_SPLIT 提取等号左侧</p>
     */
    private static final Pattern COND_SPLIT = Pattern.compile(
            "\\s+(?:AND|OR)\\s+",
            Pattern.CASE_INSENSITIVE
    );


    /**
     * 在单条 ON 条件中, 将等号两侧拆分成数组
     * <p>用途: 提取等号左侧字段 (默认视作外键), 例如 "t0.a_id = t1.id" 会分为 ["t0.a_id", "t1.id"]</p>
     * <p>关系: 在 COND_SPLIT 拆分后的每个子条件上使用, 最终得到可去重的外键列集合</p>
     */
    private static final Pattern EQ_SPLIT = Pattern.compile(
            "\\s*=\\s*",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * API入口: 为 sql 查询列列表中的非主表, 非外键字段自动添加别名
     */
    public static String generateAliases(String sql) {
        Matcher fromM = FROM_PAT.matcher(sql);
        if (!fromM.find()) {
            throw new IllegalArgumentException("无效的 SQL!");
        }
        String mainTAlias = fromM.group(1);

        Map<String, String> aliasToTable = new HashMap<>();
        Set<String> fkCols = new HashSet<>();

        Matcher joinM = JOIN_PAT.matcher(sql);
        while (joinM.find()) {
            // 获取信息
            String joinTable = joinM.group(1),
                    toAlias = joinM.group(2),
                    onClause = joinM.group(3)
                            // .split("(?i)\\s+WHERE\\s+")[0]
                            ;
            // 存储正则截取的 别名>表名
            aliasToTable.put(toAlias, joinTable);
            for (String cond : COND_SPLIT.split(onClause)) {
                String[] parts = EQ_SPLIT.split(cond);
                // if (parts.length >= 2) {
                fkCols.add(parts[0].trim());
                // }
            }
        }

        Matcher selectM = SELECT_PAT.matcher(sql);
        if (!selectM.find()) {
            throw new IllegalArgumentException("无效的 SQL!");
        }
        String rewritten = Arrays.stream(
                        selectM.group(1).trim().split(","))
                .map(String::trim)
                // for example -> f: t1.name
                .map(f -> processField(f, mainTAlias, aliasToTable, fkCols))
                .collect(Collectors.joining(", \n\t"));

        return sql.replaceFirst("(?is)SELECT\\s+.*?\\s+FROM", "SELECT " + rewritten + " FROM");
    }

    /**
     * 单列处理: 主表和外键列不变, 其他表字段追加 AS 别名
     * <p></p>
     */
    private static String processField(
            String fullColName, String mainTAlias,
            Map<String, String> aliasToTable, Set<String> fkCols
    ) {
        if (!fullColName.contains(".")) return fullColName;

        String[] parts = fullColName.split("\\.", 2); // 只取一个.左右
        String alias = parts[0],
                column = parts[1];

        if (alias.equals(mainTAlias)
                || fkCols.contains(fullColName)
            // || !aliasToTable.containsKey(alias)
        ) {
            return fullColName;
        }
        String tableName = aliasToTable.get(alias).toLowerCase();
        return fullColName + " AS " + tableName + "_" + column;
    }

    public static void main(String[] args) {
        String originalSql = "select " +
                "t0.id, " +
                "t0.name, " +
                "t0.d_id, " +
                "t1.name, " +
                "t1.com_id, " +
                "t2.name\n" +
                "from employee t0\n" +
                "join department t1 ON t0.d_id = t1.id\n" +
                "join company t2 ON t1.com_id = t2.id";

        long start = System.currentTimeMillis();
        // for (int i = 0; i < 50000; i++) {
        String s = generateAliases(originalSql);
        System.out.println("s = " + s);
        // }
        System.out.println("Elapsed: " + (System.currentTimeMillis() - start) + " ms");
    }
}
