package com.doth.selector.supports.testbean.join2;
import com.doth.selector.anno.Id;
import com.doth.selector.anno.Join;
import lombok.Data;

// 位置实体
@Data
public class Location {
    @Id
    private Integer locId; // 数据库字段: loc_id

    private String locCity; // 数据库字段: location_city

}