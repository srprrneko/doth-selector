package com.doth.selector.supports.testbean.join2;
import com.doth.selector.anno.Pk;
import com.doth.selector.anno.Join;
import lombok.Data;

// 部门实体
@Data
public class Department {
    @Pk
    private Integer deptId; // 数据库字段: d_id

    private String deptName; // 数据库字段: department_name

    @Join(fk = "office_code") // 关联到办公地点表的外键
    private Office departmentOffice; // 嵌套关联对象

    @Join(fk = "manager_id")
    // @OneTo1Breaker
    private Employee manager;


    // 其他字段...
}