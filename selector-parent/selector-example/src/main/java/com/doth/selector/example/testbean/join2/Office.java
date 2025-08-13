package com.doth.selector.example.testbean.join2;

import com.doth.selector.anno.Pk;
import com.doth.selector.anno.Join;
import lombok.Data;

// 办公地点实体
@Data
public class Office {
    @Pk
    private String officeCode; // 数据库字段: office_code

    @Join(fk = "loc_id") // 关联到位置表的外键
    private Location officeLocation; // 深度嵌套对象

}