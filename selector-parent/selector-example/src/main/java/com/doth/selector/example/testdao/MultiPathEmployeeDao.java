package com.doth.selector.example.testdao;

import com.doth.selector.anno.AutoImpl;
import com.doth.selector.core.Selector;
import com.doth.selector.example.testbean.join2.Employee;

import java.util.List;

/**
 * @author 贤
 * @version 1.0
 * @date 2025/4/15 0:19
 * @description todo
 */
@AutoImpl
public abstract class MultiPathEmployeeDao extends Selector<Employee> {

    //  测试用例1: 简单驼峰嵌套
    // 预期生成的列键: t1.dept_name (部门名称)
    public abstract List<Employee> queryByEmployeeDepartmentDeptName(String name);

    // 测试用例2: 多级驼峰嵌套（3层关联）
    // 条件段解析: DepartmentOffice → departmentOffice（部门办公地点）
    // 预期生成的列键: t2.office_code (办公地点代码)
    public abstract List<Employee> queryByEmployeeDepartmentDepartmentOfficeOfficeCode(String code);


    public abstract List<Employee> queryByEmployeeDepartmentDepartmentOfficeOfficeCodeVzCompanyName(String code, String name);

    // 测试用例3: 复杂驼峰嵌套（4层关联）
    // 条件段解析: DepartmentOfficeLocationCity → departmentOffice.officeLocation.locCity
    // 预期生成的列键: t3.location_city (所在城市)
    public abstract List<Employee> queryByEmployeeDepartmentDepartmentOfficeOfficeLocationLocCity(String city);

    // 测试用例4: 混合条件段分割（With/Vz）
    // 分割后条件段: DepartmentId, Name
    // 预期生成的列键: t1.d_id (部门ID), t0.employee_name (员工姓名)
    public abstract List<Employee> queryByEmployeeDepartmentDeptIdWithEmployeeName(Integer id, String name);

    // 错误测试用例: 无效字段路径
    // 预期行为: 编译器报错"无法找到字段: InvalidField"
    public abstract List<Employee> queryByDepartmentInvalidField(String invalid);



}