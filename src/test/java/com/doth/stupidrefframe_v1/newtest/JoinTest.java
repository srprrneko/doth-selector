package com.doth.stupidrefframe_v1.newtest;

import com.doth.stupidrefframe_v1.selector.Selector;
import com.doth.stupidrefframe_v1.testbean.Employee;
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
        String sql = "select e.*, d.name from employee e join department d on e.d_id = d.id where d.id = ?";
        List<Employee> employees = Selector.raw(Employee.class).query2Lst(sql, 1);
        for (Employee employee : employees) {
            System.out.println(employee);
        }

        System.out.println(employees.size());
    }
    @Test
    public void testJoinConvert() throws Throwable {

    }
}
