package com.doth.selector.common.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * <p>desc<p/>
 * 表示一次 JOIN 定义：表名、外键列、主键列、以及要用的别名
 * <p>服务于</p>
 * <ul>
 *     <li>sql生成</li>
 * </ul>
 */
@AllArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class JoinDefInfo {

    /**
     * 属于哪张表
     */
    private final String whereTable;

    /**
     * 从表外键
     */
    private final String fk;

    /**
     * 主键 
     */
    private final String pk;

    /**
     * 从表别名
     */
    private final String alias;

    /**
     * 主表主键
     */
    private final String mainTId;

}
