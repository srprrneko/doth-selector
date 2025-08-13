package com.doth.selector.example.testdao.newtest;

import com.doth.selector.core.Selector;
import com.doth.selector.example.testbean.join.Company;
import com.doth.selector.example.testbean.join.Department;
import com.doth.selector.example.testbean.join.Employee;

import java.util.List;

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
    //     // String sql = "SELECT e.id, e.name, "
    //     //         + "e.d_id, " +
    //     //         "d.name, d.com_id," +
    //     //         "c.name "
    //     //         + "FROM employee e " +
    //     //         "JOIN department d ON e.d_id = d.id " +
    //     //         "JOIN company c on d.com_id = c.id " +
    //     //         "where d.id = ?";
    //     String sql = "SELECT e.id, e.name, "
    //             + "e.d_id, " +
    //             "d.name, d.com_id," +
    //             "c.name " // c.name: company_name
    //             + "FROM employee e " +
    //             "JOIN department d ON e.d_id = d.id " +
    //             "JOIN company c on d.com_id = c.id " +
    //             "where d.id = ?";
    //     // 2: two -> to; 4: four -> for
    //     // queryToListForJoin
    //     return raw().query2Lst(sql,1);
    //     // return raw().query2Lst4Join(sql,true, 1);
    // }

    public List<Employee> queryAll() {
        return dct$().query();
    }


    public List<Employee> queryByName() {
        Employee employee = new Employee();
        Department department = new Department();
        department.setId(2);
        Company company = new Company();
        company.setId(2);

        department.setCompany(company);
        employee.setDepartment(department);


        return dct$().query(employee);
    }
}
