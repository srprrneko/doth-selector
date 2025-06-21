package com.doth.selector;// EmployeeDAOBuilderTest.java 测试类
import org.junit.Before;
import org.junit.Test;

public class EmployeeDAOBuilderTest {
    
    private EmployeeDAO dao;

    @Before
    public void setUp(){
        // dao = new EmployeeDAOImpl();
    }

    @Test
    public void testQueryByNameAndDepartmentIds5() { // 没有
        // List<Employee> result = dao.queryByNameAndDepartmentIds5();
        // System.out.println("queryByNameAndDepartmentIds5 result: " + result);
    }

    @Test
    public void testQueryByNameAndDepartmentIds6() { // 这个也是没有
        // List<Employee> result = dao.queryByNameAndDepartmentIds6();
        // System.out.println("queryByNameAndDepartmentIds6 result: " + result);
    }

    @Test
    public void testQueryByNameAndDepartmentIds7() {
        // List<Employee> result = dao.queryByNameAndDepartmentIds7();
        // System.out.println("queryByNameAndDepartmentIds7 result: " + result);
    } // 支持游标分页


}