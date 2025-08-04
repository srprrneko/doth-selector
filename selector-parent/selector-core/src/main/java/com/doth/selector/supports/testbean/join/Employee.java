package com.doth.selector.supports.testbean.join;

import com.doth.selector.anno.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@CheckE
public class Employee {

    @Pk
    private Integer id;

    private String name;

    private Integer age;

    @Join(fk = "d_id", refPK = "id")
    private Department department;

    @MorphCr(id = "empSimple")
    private Employee(Integer id, String name, Integer age) {}

    @MorphCr(id = "baseEmpInfo", autoPrefix = false)
    private Employee(
                @MainLevel
                    Integer id,
                    String name,
                @JoinLevel(clz = Department.class)
                    String de_name,
                    Integer de_id,
                    @Next(clz = Company.class)
                        String mngCompany_name
            // ,
            //             @Next(clz = DepartmentInfo.class, attrName = "department")
            //                 Integer di_id,
            //                 String di_departmentInfoName,
            //                 @Next(clz = User.class, attrName = "manager1")
            //                     Integer du_id,
            //                     String du_name
    ) {}

    @MorphCr(id = "baseEmpDep")
    private Employee(
                @MainLevel
                    Integer id, String name,
                @JoinLevel(clz = Department.class, attrName = "department")
                    @Name(value = "xhField")
                    String dep_name
    ) {}
}
