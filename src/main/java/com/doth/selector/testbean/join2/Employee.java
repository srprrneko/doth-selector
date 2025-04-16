package com.doth.selector.testbean.join2;

import com.doth.selector.anno.Id;
import com.doth.selector.anno.Join;

// 员工实体（包含复杂关联关系）
public class Employee {
    @Id
    private Integer employeeId; // 数据库字段: e_id

    private String employeeName; // 数据库字段: employee_name

    @Join(fk = "d_id") // 关联到部门表的外键
    private Department employeeDepartment; // 驼峰字段名对应复杂条件段

}





