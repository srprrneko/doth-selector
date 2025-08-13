package com.doth.selector.example.testdao;

import com.doth.selector.example.testbean.join.Department;
import com.doth.selector.example.testbean.join.Employee;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class EmployeeDAODctTest {

    private EmployeeDAO dao;

    public static void main(String[] args) {
        // EmployeeDAO dao = new EmployeeDAOImpl();

        // List<Employee> result = dao.queryByNameAndDepartmentIds9();
        // System.out.println("queryByNameAndDepartmentIds9 result: " + result);
    }
    //
    @Before
    public void setUp() {
        // dao = new EmployeeDAOImpl(); // 模拟service层注入


    }

    @Test
    public void testQueryAll() {
        long start = System.currentTimeMillis();

        // 总共20条
        // List<Employee> employees = dao.dct$().query2Lst();
        // System.out.println("queryAll result: " + employees);

        long end = System.currentTimeMillis();
        System.out.println("总耗时" + (end - start));
    }






    @Test
    public void testQueryById1() {
        long start = System.currentTimeMillis();
        List<Employee> result = dao.queryById1();
        long end = System.currentTimeMillis();

        System.out.println("queryById1 result: " + result);
        System.out.println("总耗时" + (end - start));
    }






    @Test
    public void testQueryById2() {
        long start = System.currentTimeMillis();
        List<Employee> result = dao.queryById2();
        long end = System.currentTimeMillis();

        System.out.println("queryById2 result: " + result);
        System.out.println("总耗时" + (end - start));
    }






    @Test
    public void testQueryByNameAndDepartmentIds3() {
        long start = System.currentTimeMillis();
        List<Employee> result = dao.queryByNameAndDepartmentIds3();
        long end = System.currentTimeMillis();

        System.out.println("queryByNameAndDepartmentIds3 result: " + result);
        System.out.println("总耗时" + (end - start));
    }






    @Test
    public void testQueryByNameAndDepartmentId1() {
        long start = System.currentTimeMillis();
        List<Employee> result = dao.queryByNameAndDepartmentId1();
        long end = System.currentTimeMillis();

        System.out.println("queryByNameAndDepartmentId1 result: " + result);
        System.out.println("总耗时" + (end - start));
    }






    @Test
    public void testQueryByNameAndDepartmentId2() {
        // 模拟controller自动解析json结构 (实际开发不会这样写)
        Employee param = new Employee();
        param.setName("张三");
        Department department = new Department();
        department.setId(1);
        param.setDepartment(department);

        long start = System.currentTimeMillis();
        List<Employee> result = dao.queryByNameAndDepartmentId2(param);
        long end = System.currentTimeMillis();

        System.out.println("queryByNameAndDepartmentId2 result: " + result);
        System.out.println("总耗时" + (end - start));
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
    } // 支持游标分页






    @Test
    public void testQueryByNameAndDepartmentIds9() {
        List<Employee> result = dao.queryByNameAndDepartmentIds9();
        System.out.println("queryByNameAndDepartmentIds9 result: " + result);
    }


















    @Test
    public void testQueryByDepartmentName() {
        long start = System.currentTimeMillis();
        List<Employee> result = dao.queryByDepartmentName("研发部");
        long end = System.currentTimeMillis();

        System.out.println("queryByDepartmentName result: " + result);
        System.out.println("总耗时" + (end - start));
    }




    @Test
    public void testQueryByName() {
        long start = System.currentTimeMillis();
        List<Employee> result = dao.queryByName("李四");
        long end = System.currentTimeMillis();

        System.out.println("queryByName result: " + result);
        System.out.println("总耗时" + (end - start));
    }

    @Test
    public void testQueryByDepartmentIdVzName() {
        List<Employee> employees = dao.queryByDepartmentIdGtVzNameLike(1, "张三%");
        System.out.println("employees = " + employees);
    }
}