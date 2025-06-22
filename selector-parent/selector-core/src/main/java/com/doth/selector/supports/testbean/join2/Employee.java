package com.doth.selector.supports.testbean.join2;

import com.doth.selector.anno.*;
import com.doth.selector.supports.testbean.join.Company;
import lombok.Data;

// 员工实体（包含复杂关联关系）
@Data
public class Employee {
    @Id
    private Integer employeeId; // 数据库字段: e_id

    private String employeeName; // 数据库字段: employee_name

    @Join(fk = "d_id") // 关联到部门表的外键
    @OneToOne
    private Department employeeDepartment; // 驼峰字段名对应复杂条件段


    @Join(fk = "c_id")
    private Company company;

    @DTOConstructor(id = "baseEmpInfo")
    public Employee(
            @MainLevel
            Integer employeeId,
            String employeeName,

            // @JoinLevel(clz = Department.class, attrName = "employeeDepartment")
            // Integer _deptId,
            // String _deptName,
            //
            // @Next(clz = Office.class, attrName = "departmentOffice")
            // String _officeCode,
            //
            // @Next(clz = Location.class, attrName = "officeLocation")
            // String _locCity,

            @JoinLevel(clz = Company.class, attrName = "company")
            String _name
    ) {}

}





