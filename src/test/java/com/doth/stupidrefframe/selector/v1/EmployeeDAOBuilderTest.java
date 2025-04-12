package com.doth.stupidrefframe.selector.v1;// EmployeeDAOBuilderTest.java 测试类
import com.doth.stupidrefframe.selector.v1.loose.testbean.join.Employee;
import org.junit.Before;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;

public class EmployeeDAOBuilderTest {
    
    private EmployeeDAO dao;

    @Before
    public void setUp(){
        dao = new EmployeeDAOImpl();
    }

    @Test
    public void testQueryByNameAndDepartmentIds5() {
        List<Employee> result = dao.queryByNameAndDepartmentIds5();
        System.out.println("queryByNameAndDepartmentIds5 result: " + result);
    }

    @Test
    public void testQueryByNameAndDepartmentIds6() {
        List<Employee> result = dao.queryByNameAndDepartmentIds6();
        System.out.println("queryByNameAndDepartmentIds6 result: " + result);
    }

    @Test
    public void testQueryByNameAndDepartmentIds7() {
        List<Employee> result = dao.queryByNameAndDepartmentIds7();
        System.out.println("queryByNameAndDepartmentIds7 result: " + result);
    }


}