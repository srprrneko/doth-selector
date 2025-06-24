package com.doth.selector.supports.testbean.join2;

import com.doth.selector.anno.*;
import com.doth.selector.supports.testbean.join.Company;
import lombok.Data;

@Data
public class Employee {
    @Id
    private Integer employeeId;

    private String employeeName;


    @Join(fk = "d_id")
    @OneToOne
    private Department employeeDepartment;

    @Join(fk = "c_id")
    private Company company;


    @DTOConstructor(id = "baseEmpInfo")
    public Employee(
            @MainLevel
            Integer employeeId,
            String employeeName,

            @JoinLevel(clz = Department.class, attrName = "employeeDepartment")
            Integer _deptId,
            String _deptName,

            @Next(clz = Office.class, attrName = "departmentOffice")
            String _officeCode,

            @Next(clz = Location.class, attrName = "officeLocation")
            String _locCity,

            @JoinLevel(clz = Company.class, attrName = "company")
            String _name
    ) {}

}





