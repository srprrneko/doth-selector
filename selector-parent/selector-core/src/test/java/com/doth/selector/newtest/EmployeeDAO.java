package com.doth.selector.newtest;

import com.doth.selector.anno.CreateDaoImpl;
// import com.doth.selector.supports.testbean.join.BaseEmpDep;
// import com.doth.selector.supports.testbean.join.BaseEmpInfo;
import com.doth.selector.core.Selector;
// import com.doth.selector.supports.testbean.join.BaseEmpDep;
// import com.doth.selector.supports.testbean.join.BaseEmpInfo;
import com.doth.selector.executor.supports.QueryList;
import com.doth.selector.supports.testbean.join.Employee;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.List;

/**
 * @author 贤
 * @version 1.0
 * @date 2025/5/3 18:49
 * @description 测试员工查询接口
 *
 *  后续的 优化思路:
 *  1.自动 dto
 *      难点: 动态选择该方法返回 的 dto 类
 *
 *  2.懒加载忽略
 *      不加载不需要的类
 *      思路: 在sql生成的 时候拦截
 *      难点: 注解是在方法里加的, 怎么识别到?
 *      解决1: 在类中 标记懒加载策略注解, 方法里通过注解引用 (有些类似于JPA的做法)
 *          缺点: 实体类有了不该有的东西, 有点为了实现而实现了, 再加上 自己设置的构造方法缘由, 那么实体中的信息一定会太多太多的
 *          不过我有个方法
 *              首先我调用的方法 不就可以拿到 字节码, 然后判断获取注解吗
 *                  接着我就可以 走分支了, 然后把我的sql生成类 用工厂或者策略模式 进行 逻辑切换, 不就行了?
 *  ================================================================================================
 *  总结自动dto遇到的问题
 *  1.同参数构造方法无法控制
 *      解决: 考虑同样支持方法
 *  2.
 *
 */
@Slf4j
@CreateDaoImpl(springSupport = true)
public abstract class EmployeeDAO extends Selector<Employee> {

    public static void main(String[] args) {
        // EmployeeDAO dao = new EmployeeDAOImpl();
        //
        // // List<BaseEmpDep> implDto = dao.dtoImpl();
        //
        // long start = System.currentTimeMillis();
        // for (int i = 0; i < 50000; i++) {
        //     dao.impl();
        //     // dao.dtoImpl();
        //     // dao.dtoImpl2();
        //     // dao.testNew();
        // }
        // long end = System.currentTimeMillis();
        // System.out.println("(end - start) = " + (end - start));

        // System.out.println("impl.get(0).getClass() = " + impl.get(0).getClass());
        // System.out.println("impl = " + impl);
        // dtoImpl.forEach(e -> {
        //     System.out.println("================================================================================");
        //     System.out.println("e.getId() = " + e.getId());
        //     System.out.println("e.getName() = " + e.getName());
        //
        //     // System.out.println("e.getDepartmentId() = " + e.getDepartmentId());
        //     System.out.println("e.getDepartmentName() = " + e.getDepartmentName());
        //     // System.out.println("e.getCompanyName() = " + e.getCompanyName());
        //     System.out.println("================================================================================");
        // });
        // dtoImpl2.forEach(e -> {
        //     System.out.println("================================================================================");
        //     System.out.println("e.getId() = " + e.getId());
        //     System.out.println("e.getName() = " + e.getName());
        //
        //     System.out.println("e.getDepartmentId() = " + e.getDepartmentId());
        //     System.out.println("e.getDepartmentName() = " + e.getDepartmentName());
        //     System.out.println("e.getCompanyName() = " + e.getCompanyName());
        //     System.out.println("================================================================================");
        // });

        // System.out.println("impl = " + implDto);
        /*

        执行的方法:

         List<BaseEmpInfo> dtoImpl2 = dao.dtoImpl2();

        dtoImpl2.forEach(e -> {
            System.out.println("================================================================================");
            System.out.println("e.getId() = " + e.getId());
            System.out.println("e.getName() = " + e.getName());

            System.out.println("e.getDepartmentId() = " + e.getDepartmentId());
            System.out.println("e.getDepartmentName() = " + e.getDepartmentName());
            System.out.println("e.getCompanyName() = " + e.getCompanyName());
            System.out.println("================================================================================");
        });

        输出:
        旧版本
        e.getId() = 15
        e.getName() = 蒋十七
        e.getDepartmentId() = 1
        e.getDepartmentName() = 研发部
        e.getCompanyName() = 公司A
        新版本

         */
    }
    @Test
    public void testNew() {
        Employee impl = this.impl();
        // System.out.println("impl.get(0).getClass() = " + impl.get(0).getClass());
        System.out.println("impl = " + impl);
    }

    public Employee impl() {
        return bud$().query(builder ->
                builder.eq(Employee::getId, 1)
        ).toOne();
    }

    // public List<BaseEmpDep> dtoImpl() {
    //     return queryDtoList(BaseEmpDep.class, builder -> {
    //         // builder.eq(e -> e.getDepartment().getName(),
    //         builder.eq("t1.name",
    //                 "研发部");
    //
    //     });
    // }
    // public List<BaseEmpInfo> dtoImpl2() {
    //     return queryDtoList(BaseEmpInfo.class, builder -> {
    //         builder.eq(e -> e.getDepartment().getName(),
    //                 "市场部");
    //
    //     });
    // }
    //
    // @Test
    // public void testDtoImpl2() {
    //     EmployeeDAO dao = new EmployeeDAOImpl();
    //     List<BaseEmpInfo> list = dao.dtoImpl2();
    //     log.info("data: {}", list);
    // }

    /**
     * 根据员工姓名, 查询员工列表
     *
     * @return 员工列表
     */
    public abstract Employee getById(Integer id);


    @Test
    public void testQueryByName() {
        //

        long start = System.currentTimeMillis();
        // for (int i = 0; i < 50000; i++) {
            Employee employees = this.getById(1);
        // }
        long cost = System.currentTimeMillis() - start;
        System.out.println("cost = " + cost);
        // System.out.println("queryByName 执行耗时: " + cost + "ms, 结果数量: " + (result != null ? result.size() : 0));
        //
        // System.out.println("result.getClass() = " + result.get(0).getClass());

    }









    /**
     * 根据部门名称, 查询员工列表
     * @param name 部门姓名
     * @return 员列表
     */
    public abstract List<Employee> queryByDepartmentName(String name);

    @Test
    public void testQueryByDepartmentName() {
        long start = System.currentTimeMillis();
        // for (int i = 0; i < 50000; i++) {
        List<Employee> result = this.queryByDepartmentName("研发部");
        // }
        long cost = System.currentTimeMillis() - start;
        System.out.println("queryByDepartmentName 执行耗时: " + cost);
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
    public abstract List<Employee> queryByDepartmentIdAndNameLike(Integer id, String name);

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