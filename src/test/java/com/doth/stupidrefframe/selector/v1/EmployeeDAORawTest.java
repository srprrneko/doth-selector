package com.doth.stupidrefframe.selector.v1;// EmployeeDAORawTest.java 测试类
import com.doth.stupidrefframe.selector.v1.loose.testbean.join.Employee;
import org.junit.Before;
import org.junit.Test;
import java.util.List;

public class EmployeeDAORawTest {
    
    private EmployeeDAO dao;

    @Before
    public void setUp() {
        dao = new EmployeeDAOImpl();
    }

    @Test
    public void testQueryByNameAndDepartmentIds9() {
        List<Employee> result = dao.queryByNameAndDepartmentIds9();
        System.out.println("queryByNameAndDepartmentIds9 result: " + result);
    }
}