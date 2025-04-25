package com.doth.selector;// EmployeeDAODctTest.java 测试类
import com.doth.selector.testbean.join.Employee;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class EmployeeDAODctTest {
    
    private EmployeeDAO dao;
    //
    @Before
    public void setUp() {
        dao = new EmployeeDAOImpl(); // 模拟service层注入
    }

    @Test
    public void testQueryAll() {
        // 也可以直接调用
        long start = System.currentTimeMillis();
        List<Employee> employees = dao.dct$().query2Lst();
        // List<Employee> result = dao.queryAll(); // 连接查询总共 20 条数据的性能消耗
        long end = System.currentTimeMillis();

        System.out.println("queryAll result: " + employees);
        System.out.println("总耗时" + (end - start));
    }



    //
    // @Test
    // public void testQueryById1() {
    //     long start = System.currentTimeMillis();
    //     List<Employee> result = dao.queryById1();
    //     long end = System.currentTimeMillis();
    //
    //     System.out.println("queryById1 result: " + result);
    //     System.out.println("总耗时" + (end - start));
    // }
    //
    //
    //
    //
    // @Test
    // public void testQueryById2() {
    //     long start = System.currentTimeMillis();
    //     List<Employee> result = dao.queryById2();
    //     long end = System.currentTimeMillis();
    //
    //     System.out.println("queryById2 result: " + result);
    //     System.out.println("总耗时" + (end - start));
    // }
    //
    // @Test
    // public void testQueryByNameAndDepartmentId1() {
    //
    //     long start = System.currentTimeMillis();
    //     List<Employee> result = dao.queryByNameAndDepartmentId1();
    //     long end = System.currentTimeMillis();
    //
    //     System.out.println("queryByNameAndDepartmentId1 result: " + result);
    //     System.out.println("总耗时" + (end - start));
    // }
    //
    //
    //
    //
    //
    // @Test
    // public void testQueryByNameAndDepartmentId2() {
    //     // 模拟controller自动解析json结构
    //     Employee param = new Employee();
    //     param.setName("张三");
    //     Department department = new Department();
    //     department.setId(1);
    //     param.setDepartment(department);
    //
    //     long start = System.currentTimeMillis();
    //     List<Employee> result = dao.queryByNameAndDepartmentId2(param);
    //     long end = System.currentTimeMillis();
    //
    //     System.out.println("queryByNameAndDepartmentId2 result: " + result);
    //     System.out.println("总耗时" + (end - start));
    // }
    //
    // @Test
    // public void testQueryByNameAndDepartmentIds3() {
    //     long start = System.currentTimeMillis();
    //     List<Employee> result = dao.queryByNameAndDepartmentIds3();
    //     long end = System.currentTimeMillis();
    //
    //     System.out.println("queryByNameAndDepartmentIds3 result: " + result);
    //     System.out.println("总耗时" + (end - start));
    // }
    //
    // @Test
    // public void testQueryByDepartmentName() {
    //     long start = System.currentTimeMillis();
    //     List<Employee> result = dao.queryByDepartmentName("研发部");
    //     long end = System.currentTimeMillis();
    //
    //     System.out.println("queryByDepartmentName result: " + result);
    //     System.out.println("总耗时" + (end - start));
    // }
    //
    // @Test
    // public void testQueryByName() {
    //     long start = System.currentTimeMillis();
    //     List<Employee> result = dao.queryByName("李四");
    //     long end = System.currentTimeMillis();
    //
    //     System.out.println("queryByName result: " + result);
    //     System.out.println("总耗时" + (end - start));
    // }
    //
    @Test
    public void testQueryByDepartmentIdVzName() {
        long start = System.currentTimeMillis();
        List<Employee> employees = dao.queryByDepartmentIdVzName(1, "张三");
        long end = System.currentTimeMillis();
        System.out.println("employees = " + employees);
        System.out.println("总耗时" + (end - start));
    }
}