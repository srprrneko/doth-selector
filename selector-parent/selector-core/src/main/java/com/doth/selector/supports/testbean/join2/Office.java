package com.doth.selector.supports.testbean.join2;

import com.doth.selector.anno.Id;
import com.doth.selector.anno.Join;

// 办公地点实体
public class Office {
    @Id
    private String officeCode; // 数据库字段: office_code

    @Join(fk = "loc_id") // 关联到位置表的外键
    private Location officeLocation; // 深度嵌套对象

    // 其他字段...
}