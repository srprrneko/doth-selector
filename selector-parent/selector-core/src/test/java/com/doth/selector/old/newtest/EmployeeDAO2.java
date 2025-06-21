package com.doth.selector.old.newtest;

import com.doth.selector.core.Selector;
import com.doth.selector.supports.testbean.join.Company;
import com.doth.selector.supports.testbean.join.Department;
import com.doth.selector.supports.testbean.join.Employee;

import java.util.List;

/**
 * @project: test02
 * @package: com.doth.stupidrefframe.newtest
 * @author: doth
 * @creTime: 2025-03-30  19:36
 * @desc: TODO
 * @v: 1.0
 */
// data access object
public class EmployeeDAO2 extends Selector<Employee> {
    public static void main(String[] args) {
        // service
        EmployeeDAO2 dao = new EmployeeDAO2();
        // List<Employee> employees = dao.queryEmployees();
        // System.out.println(employees);

        long start = System.currentTimeMillis();
        List<Employee> employees = dao.queryAll();
        long end = System.currentTimeMillis();
        System.out.println("employees = " + employees);
        System.out.println("(end - start) = " + (end - start));
        //
        // List<Employee> employees = dao.queryByName();
        // System.out.println("employees = " + employees);

    }

    // public List<Employee> queryEmployees() {
    //     // String sqlgenerator = "SELECT e.id, e.name, "
    //     //         + "e.d_id, " +
    //     //         "d.name, d.com_id," +
    //     //         "c.name "
    //     //         + "FROM employee e " +
    //     //         "JOIN department d ON e.d_id = d.id " +
    //     //         "JOIN company c on d.com_id = c.id " +
    //     //         "where d.id = ?";
    //     String sqlgenerator = "SELECT e.id, e.name, "
    //             + "e.d_id, " +
    //             "d.name, d.com_id," +
    //             "c.name " // c.name: company_name
    //             + "FROM employee e " +
    //             "JOIN department d ON e.d_id = d.id " +
    //             "JOIN company c on d.com_id = c.id " +
    //             "where d.id = ?";
    //     // 2: two -> to; 4: four -> for
    //     // queryToListForJoin
    //     return raw().query2Lst(sqlgenerator,1);
    //     // return raw().query2Lst4Join(sqlgenerator,true, 1);
    // }

    public List<Employee> queryAll() {
        return dct$().query2Lst();
    }


    public List<Employee> queryByName() {
        Employee employee = new Employee();
        Department department = new Department();
        department.setId(2);
        Company company = new Company();
        company.setId(2);

        department.setCompany(company);
        employee.setDepartment(department);


        return dct$().query2Lst(employee);
    }
}
