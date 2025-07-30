// ResultSetUtils.java
package com.doth.selector.convertor.supports;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * 结果集处理工具
 */
public class ResultSetUtils {

    // 提取列文本信息
    public static Set<String> extractColumnLabels(ResultSetMetaData meta) throws SQLException {
        int count = meta.getColumnCount();

        Set<String> labels = new HashSet<>(count);
        for (int i = 1; i <= count; i++) {
            labels.add(meta.getColumnLabel(i).toLowerCase());
        }
        return labels;
    }
}
