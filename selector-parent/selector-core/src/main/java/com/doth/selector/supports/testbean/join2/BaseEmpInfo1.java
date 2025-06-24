package com.doth.selector.supports.testbean.join2;

import com.doth.selector.anno.DependOn;
import com.doth.selector.common.dto.DTOJoinInfo;
import com.doth.selector.common.dto.DTOJoinInfoFactory;
import com.doth.selector.common.dto.DTOSelectFieldsListFactory;
import com.doth.selector.common.dto.JoinDef;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

@DependOn(
        clzPath = "com.doth.selector.supports.testbean.join2.Employee"
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseEmpInfo1 {

    static {
        // 注册 DTOJoinInfo：company 用别名 t1，对应 c_id → id
        DTOJoinInfoFactory.register(
                com.doth.selector.supports.testbean.join2.Employee.class,
                "baseEmpInfo1",
                new DTOJoinInfo(
                        List.of(
                                new JoinDef("company", "c_id", "id", "t1")
                        )
                )
        );

        // 注册 select fields 列表
        List<String> __select = Arrays.asList(
                "t0.employee_id",
                "t0.employee_name",
                "t1.name"
        );
        DTOSelectFieldsListFactory.register(
                com.doth.selector.supports.testbean.join2.Employee.class,
                "baseEmpInfo1",
                __select
        );
    }

    private Integer employeeId;

    private String employeeName;

    private String name;

    public BaseEmpInfo1(Employee employee) {
        this.employeeId = employee.getEmployeeId();
        this.employeeName = employee.getEmployeeName();
        this.name = employee.getCompany().getName();
    }
}
