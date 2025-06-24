// src/main/java/com/doth/selector/supports/sqlgenerator/dto/JoinDef.java
package com.doth.selector.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 表示一次 JOIN 定义：表名、外键列、主键列、以及要用的别名。
 */
@AllArgsConstructor
@Getter
public class JoinDef {
    private final String relationTable;
    private final String foreignKeyColumn;
    private final String primaryKeyColumn;
    private final String alias;

}
