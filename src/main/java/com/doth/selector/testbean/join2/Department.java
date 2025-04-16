package com.doth.selector.testbean.join2;
import com.doth.selector.anno.Id;
import com.doth.selector.anno.Join;
// 部门实体
public class Department {
    @Id
    private Integer deptId; // 数据库字段: d_id

    private String deptName; // 数据库字段: department_name

    @Join(fk = "office_code") // 关联到办公地点表的外键
    private Office departmentOffice; // 嵌套关联对象

    // 其他字段...
}