package com.doth.selector.testbean.join2;
import com.doth.selector.anno.Id;
import com.doth.selector.anno.Join;
// 位置实体
public class Location {
    @Id
    private Integer locId; // 数据库字段: loc_id

    private String locCity; // 数据库字段: location_city

    // 其他字段...
}