package com.doth.stupidrefframe.selector.v1;// EmployeeDAODctTest.java 测试类
import com.doth.stupidrefframe.selector.v1.loose.testbean.join.Department;
import com.doth.stupidrefframe.selector.v1.loose.testbean.join.Employee;
import org.junit.Before;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;

public class EmployeeDAODctTest {
    
    private EmployeeDAO dao;

    @Before
    public void setUp() {
        dao = new EmployeeDAOImpl(); // 模拟service层注入
    }

    @Test
    public void testQueryAll() {
        List<Employee> result = dao.queryAll();
        System.out.println("queryAll result: " + result);
    }

    @Test
    public void testQueryById1() {
        List<Employee> result = dao.queryById1();
        System.out.println("queryById1 result: " + result);
    }

    @Test
    public void testQueryById2() {
        List<Employee> result = dao.queryById2();
        System.out.println("queryById2 result: " + result);
    }

    @Test
    public void testQueryByNameAndDepartmentId1() {
        List<Employee> result = dao.queryByNameAndDepartmentId1();
        System.out.println("queryByNameAndDepartmentId1 result: " + result);
    }

    @Test
    public void testQueryByNameAndDepartmentId2() {
        // 模拟controller自动解析json结构
        Employee param = new Employee();
        param.setName("张三");
        Department department = new Department();
        department.setId(1);
        param.setDepartment(department);
        
        List<Employee> result = dao.queryByNameAndDepartmentId2(param);
        System.out.println("queryByNameAndDepartmentId2 result: " + result);
    }

    @Test
    public void testQueryByNameAndDepartmentIds3() {
        List<Employee> result = dao.queryByNameAndDepartmentIds3();
        System.out.println("queryByNameAndDepartmentIds3 result: " + result);
    }

    @Test
    public void testQueryByNameAndDepartmentIds4() {
        List<Employee> result = dao.queryByNameAndDepartmentIds4();
        System.out.println("queryByNameAndDepartmentIds4 result: " + result);
    }

    @Test
    public void testQueryByDepartmentName() {
        List<Employee> result = dao.queryByDepartmentName("研发部");
        System.out.println("queryByDepartmentName result: " + result);
    }

    @Test
    public void testQueryByName() {
        List<Employee> result = dao.queryByName("李四");
        System.out.println("queryByName result: " + result);
    }
}