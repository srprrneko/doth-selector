package com.doth.stupidrefframe_v1.newtest;

import com.doth.stupidrefframe_v1.selector.Selector;
import com.doth.stupidrefframe_v1.testbean.join.Department;
import com.doth.stupidrefframe_v1.testbean.join.Employee;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @project: test02
 * @package: com.doth.stupidrefframe_v1.newtest
 * @author: doth
 * @creTime: 2025-03-27  11:16
 * @desc: TODO
 * @v: 1.0
 */
public class JoinTest {


    @Test
    public void test1() {
        // 员工 部门id -> 部门

        String sql = "SELECT e.id, e.name, "
                + "e.d_id," +
                "d.id as department_id, d.name AS department_name, d.com_id, " +
                "c.name AS company_name "
                + "FROM employee e " +
                "JOIN department d ON e.d_id = d.id " +
                "JOIN company c on d.com_id = c.id " +
                "where d.id = ?";

        // String sql = "SELECT d.id, d.name, d.com_id, " +
        //         "c.name AS company_name "
        //         + "FROM department d " +
        //         "JOIN company c on d.com_id = c.id " +
        //         "where d.id = ?";
        // d: id

        // e: {id, name, department{id, }}
        List<Employee> employees = Selector.raw(Employee.class).query2Lst(sql,1);
        for (Employee employee : employees) {
            System.out.println(employee);
        }

        System.out.println(employees.size());
    }
    @Test
    public void testJoinConvert() throws Throwable {

    }
}
