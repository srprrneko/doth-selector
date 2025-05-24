package com.doth.selector.newtest;

import com.doth.selector.anno.CreateDaoImpl;
import com.doth.selector.core.Selector;
import com.doth.selector.common.testbean.join.Employee;
import org.junit.Test;

import java.util.List;

/**
 * @author 贤
 * @version 1.0
 * @date 2025/5/3 18:49
 * @description 测试员工查询接口
 */
@CreateDaoImpl
public abstract class EmployeeDAO extends Selector<Employee> {
    private Employee employee;


    @Test
    public void testNew() {
        // System.out.println("query() = " + query());
    }

    // public List<Employee> query() {
        // return (List<Employee>) budEnhanced()
        //         .like(Employee::getName, "张三")
        //         .in(e -> e.getDepartment().getId(), 1, 2, 3);
    // }

    
    /**
     * 根据员工姓名, 查询员工列表
     * @param name 员工姓名
     * @return 员工列表
     */
    public abstract List<Employee> queryByName(String name);


    @Test
    public void testQueryByName() {
        //

        long start = System.currentTimeMillis();
        List<Employee> result = this.queryByName("张三");
        long cost = System.currentTimeMillis() - start;
        System.out.println("queryByName 执行耗时: " + cost + "ms, 结果数量: " + (result != null ? result.size() : 0));
        System.out.println(result);
    }









    /**
     * 根据部门名称, 查询员工列表
     * @param name 部门姓名
     * @return 员列表
     */
    public abstract List<Employee> queryByDepartmentName(String name);
    
    @Test
    public void testQueryByDepartmentName() { // emp 包含 department,
        long start = System.currentTimeMillis(); // 电脑有点卡
        List<Employee> result = this.queryByDepartmentName("研发部");
        long cost = System.currentTimeMillis() - start;
        System.out.println("queryByDepartmentName 执行耗时: " + cost + "ms, 结果数量: " + (result != null ? result.size() : 0));
        System.out.println(result);
    }









    /**
     * 根据部门编号和员工姓名查询
     * @param id 部门编号
     * @param name 员工姓名
     * @return 员工列表
     */
    public abstract List<Employee> queryByDepartmentIdVzName(Integer id, String name); // 完全不需要写东西, 一个方法名搞定
    
    @Test
    public void testQueryByDepartmentIdVzName() { // Vz = with
        long start = System.currentTimeMillis();
        List<Employee> result = this.queryByDepartmentIdVzName(1, "李四"); // 框架用的是反射, 所以性能会块一点
        long cost = System.currentTimeMillis() - start;
        System.out.println("queryByDepartmentIdVzName 执行耗时: " + cost + "ms, 结果数量: " + (result != null ? result.size() : 0));
        System.out.println(result);
    }









    /**
     * 根据部门编号, 和员工姓名模糊查询
     * @param id 部门编号
     * @param name 员工姓名
     * @return 员工列表
     */
    public abstract List<Employee> queryByDepartmentIdAndNameLike(Integer id, String name); // 后续考虑使用注解解决方法名过长的问题
    
    @Test
    public void testQueryByDepartmentIdAndNameLike() { //
        long start = System.currentTimeMillis();
        List<Employee> result = this.queryByDepartmentIdAndNameLike(1, "张%");
        long cost = System.currentTimeMillis() - start;
        System.out.println("queryByDepartmentIdAndNameLike 执行耗时: " + cost + "ms, 结果数量: " + (result != null ? result.size() : 0));
        System.out.println(result);
    }









    /**
     * 根据部门名称和年龄大于某个值, 查询员工列表
     * @param departmentName 部门名称
     * @param age 年龄
     * @return 员工列表
     */
    public abstract List<Employee> queryByDepartmentNameLikeAndAgeGt(String departmentName, Integer age);
    @Test
    public void testQueryByDepartmentNameLikeAndAgeGt() {
        long start = System.currentTimeMillis();
        List<Employee> result = this.queryByDepartmentNameLikeAndAgeGt("研发%", 18); // 自动like gt = greater than
        long cost = System.currentTimeMillis() - start;
        System.out.println("queryByDepartmentNameLikeAndAgeGt 执行耗时: " + cost + "ms, 结果数量: " + (result != null ? result.size() : 0));
        System.out.println(result);
    }

}