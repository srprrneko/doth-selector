package com.doth.stupidrefframe_v1.newtest;

import com.doth.stupidrefframe_v1.selector.Selector_v1;
import com.doth.loose.testbean.join.Employee;

import java.util.List;

/**
 * @project: test02
 * @package: com.doth.stupidrefframe_v1.newtest
 * @author: doth
 * @creTime: 2025-03-30  19:36
 * @desc: TODO
 * @v: 1.0
 */
public class EmployeeDAO extends Selector_v1<Employee> {
    public static void main(String[] args) {
        EmployeeDAO dao = new EmployeeDAO();
        List<Employee> employees = dao.queryEmployees();
        System.out.println(employees);
    }

    public List<Employee> queryEmployees() {
        String sql = "SELECT e.id, e.name, "
                + "e.d_id," +
                "d.id as department_id, d.name AS department_name, d.com_id, " +
                "c.name AS company_name "
                + "FROM employee e " +
                "JOIN department d ON e.d_id = d.id " +
                "JOIN company c on d.com_id = c.id " +
                "where d.id = ?";
        return raw().query2Lst(sql,1);
    }
}
