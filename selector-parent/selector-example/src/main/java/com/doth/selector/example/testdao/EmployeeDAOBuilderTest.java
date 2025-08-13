package com.doth.selector.example.testdao;// EmployeeDAOBuilderTest.java 测试类
import com.doth.selector.example.testdao.EmployeeDAO;
import org.junit.Before;
import org.junit.Test;

public class EmployeeDAOBuilderTest {
    
    private EmployeeDAO dao;

    @Before
    public void setUp(){
        // dao = new EmployeeDAOImpl();
    }

    @Test
    public void testQueryByNameAndDepartmentIds5() { // 无数据
        // List<Employee> result = dao.queryByNameAndDepartmentIds5();
        // System.out.println("queryByNameAndDepartmentIds5 result: " + result);
    }

    @Test
    public void testQueryByNameAndDepartmentIds6() { // 同
        // List<Employee> result = dao.queryByNameAndDepartmentIds6();
        // System.out.println("queryByNameAndDepartmentIds6 result: " + result);
    }

    @Test
    public void testQueryByNameAndDepartmentIds7() {
        // List<Employee> result = dao.queryByNameAndDepartmentIds7();
        // System.out.println("queryByNameAndDepartmentIds7 result: " + result);
    } // 支持游标分页


}