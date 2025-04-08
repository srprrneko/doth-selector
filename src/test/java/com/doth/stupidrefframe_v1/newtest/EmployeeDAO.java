package com.doth.stupidrefframe_v1.newtest;

import com.doth.loose.testbean.join.Company;
import com.doth.loose.testbean.join.Department;
import com.doth.loose.testbean.join.Employee;
import com.doth.stupidrefframe_v1.selector.v1.core.SelectorV2;

import java.util.List;

/**
 * @project: test02
 * @package: com.doth.stupidrefframe_v1.newtest
 * @author: doth
 * @creTime: 2025-03-30  19:36
 * @desc: TODO
 * @v: 1.0
 */
// data assert object
public class EmployeeDAO extends SelectorV2<Employee> {
    public static void main(String[] args) {
        // service
        EmployeeDAO dao = new EmployeeDAO();
        // List<Employee> employees = dao.queryEmployees();
        // System.out.println(employees);

        List<Employee> employees = dao.queryAll();
        System.out.println("employees = " + employees);
        //
        // List<Employee> employees = dao.queryByName();
        // System.out.println("employees = " + employees);

    }

    public List<Employee> queryEmployees() {
        // String sqlgenerator = "SELECT e.id, e.name, "
        //         + "e.d_id, " +
        //         "d.name, d.com_id," +
        //         "c.name "
        //         + "FROM employee e " +
        //         "JOIN department d ON e.d_id = d.id " +
        //         "JOIN company c on d.com_id = c.id " +
        //         "where d.id = ?";
        String sql = "SELECT e.id, e.name, "
                + "e.d_id, " +
                "d.name, d.com_id," +
                "c.name " // c.name: company_name
                + "FROM employee e " +
                "JOIN department d ON e.d_id = d.id " +
                "JOIN company c on d.com_id = c.id " +
                "where d.id = ?";
        // 2: two -> to; 4: four -> for
        // queryToListForJoin
        return raw().query2Lst(sql,1);
        // return raw().query2Lst4Join(sqlgenerator,true, 1);
    }

    public List<Employee> queryAll() {
        return direct_join().query2Lst();
    }

    public List<Employee> queryByName() {
        Employee employee = new Employee();
        Department department = new Department();
        department.setId(2);
        Company company = new Company();
        company.setId(2);


        department.setCompany(company);
        employee.setDepartment(department);
        return direct_join().query2Lst(employee);
    }
}
