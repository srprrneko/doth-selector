package com.doth.selector.newtest;

import com.doth.selector.anno.CreateDaoImpl;
import com.doth.selector.core.Selector;
import com.doth.selector.supports.testbean.join.BaseEmpDep;
import com.doth.selector.supports.testbean.join.BaseEmpInfo;
import com.doth.selector.supports.testbean.join.Employee;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.List;


@Slf4j
@CreateDaoImpl()
public abstract class EmployeeDAO extends Selector<Employee> {


    // public static void main(String[] args) throws InterruptedException {
    //
    //
    //     // System.out.println("hello world");
    //     long start = System.currentTimeMillis();
    //     Thread.sleep(5000);
    //     long end = System.currentTimeMillis();
    //     System.out.println("Sleep 5秒，实际过去：" + (end - start) + " ms");
    // }

    public static void main(String[] args) {
        EmployeeDAO dao = new EmployeeDAOImpl();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 50000; i++) {
            List<Employee> result = dao.queryByDepartmentName("研发部");
            // System.out.println("result = " + result);
        }
        long cost = System.currentTimeMillis() - start;
        System.out.println("queryByDepartmentName 执行耗时: " + cost);
    }

    // public static void main(String[] args) {
    //     // System.out.println("实际运行时版本: " + System.getProperty("java.version"));
    //     // System.out.println("JVM供应商: " + System.getProperty("java.vendor"));
    //     // System.out.println("JIT编译器: " + System.getProperty("java.vm.name"));
    //     // System.out.println("JIT版本: " + System.getProperty("java.vm.version"));
    //     // System.out.println("泥嚎");
    //
    //     // EmployeeDAO dao = new EmployeeDAOImpl();
    //     // long start = System.currentTimeMillis();
    //     // for (int i = 0; i < 10000; i++) {
    //     //     dao.queryByDepartmentName("研发部");
    //     // }
    //     // long cost = System.currentTimeMillis() - start;
    //     // System.out.println("queryByDepartmentName 执行耗时: " + cost);
    //
    //
    //     // EmployeeDAO dao = new EmployeeDAOImpl();
    //     //
    //     // // List<BaseEmpDep> implDto = dao.dtoImpl();
    //     //
    //     // long start = System.currentTimeMillis();
    //     // for (int i = 0; i < 50000; i++) {
    //     //     dao.impl();
    //     //     // dao.dtoImpl();
    //     //     // dao.dtoImpl2();
    //     //     // dao.testNew();
    //     // }
    //     // long end = System.currentTimeMillis();
    //     // System.out.println("(end - start) = " + (end - start));
    //
    //     // System.out.println("impl.get(0).getClass() = " + impl.get(0).getClass());
    //     // System.out.println("impl = " + impl);
    //     // dtoImpl.forEach(e -> {
    //     //     System.out.println("================================================================================");
    //     //     System.out.println("e.getId() = " + e.getId());
    //     //     System.out.println("e.getName() = " + e.getName());
    //     //
    //     //     // System.out.println("e.getDepartmentId() = " + e.getDepartmentId());
    //     //     System.out.println("e.getDepartmentName() = " + e.getDepartmentName());
    //     //     // System.out.println("e.getCompanyName() = " + e.getCompanyName());
    //     //     System.out.println("================================================================================");
    //     // });
    //     // System.out.println("impl = " + implDto);
    //     /*
    //
    //
    //      */
    // }

    // @Test
    // public void testNew() {
    //     Employee impl = this.impl();
    //     // System.out.println("impl.get(0).getClass() = " + impl.get(0).getClass());
    //     System.out.println("impl = " + impl);
    // }

    // public Employee impl() {
    //     return bud$().query(builder ->
    //             builder.eq(Employee::getId, 1)
    //     ).toOne();
    // }

    @Test
    public void testDTOIMPL() {
        EmployeeDAO dao = new EmployeeDAOImpl();

        long start = System.currentTimeMillis();
        List<BaseEmpDep> baseEmpDeps = dao.dtoImpl();
        long end = System.currentTimeMillis();

        System.out.println("baseEmpDeps = " + baseEmpDeps);
        System.out.println("end - start = " + (end - start));


    }

    public List<BaseEmpDep> dtoImpl() {
        return queryDtoList(BaseEmpDep.class, builder -> {
            builder.eq(e -> e.getDepartment().getCompany().getName(), "公司A");
        });
    }
    public List<BaseEmpInfo> dtoImpl2(String depName) {
        return bud$().query( builder ->
            builder.eq(e -> e.getDepartment().getName(), depName)
        ).toDto(BaseEmpInfo.class);
    }

    @Test
    public void testDtoImpl2() {
        EmployeeDAO dao = new EmployeeDAOImpl();
        List<BaseEmpInfo> list = dao.dtoImpl2("市场部");
        log.info("data: {}", list);

    }

    /**
     * 根据员工姓名, 查询员工列表
     *
     * @return 员工列表
     */
    // public abstract Employee getById(Integer id);


    // @Test
    // public void testQueryByName() {
    //     //
    //
    //     long start = System.currentTimeMillis();
    //     for (int i = 0; i < 50000; i++) {
    //         Employee employees = this.getById(1);
    //     }
    //     long cost = System.currentTimeMillis() - start;
    //     System.out.println("cost = " + cost);
    //     // System.out.println("queryByName 执行耗时: " + cost + "ms, 结果数量: " + (result != null ? result.size() : 0));
    //     //
    //     // System.out.println("result.getClass() = " + result.get(0).getClass());
    //
    // }


    /**
     * 根据部门名称, 查询员工列表
     *
     * @param name 部门姓名
     * @return 员列表
     */
    public abstract List<Employee> queryByDepartmentName(String name);

    @Test
    public void testQueryByDepartmentName() {
        EmployeeDAO dao = new EmployeeDAOImpl();
        long start = System.currentTimeMillis();
        // for (int i = 0; i < 50000; i++) {
            var res = dao.queryByDepartmentName("研发部");
        System.out.println("res = " + res);
        // }
        long cost = System.currentTimeMillis() - start;
        System.out.println("queryByDepartmentName 执行耗时: " + cost);
        // System.out.println(result);
    }


    /**
     * 根据部门编号和员工姓名查询
     *
     * @param id   部门编号
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
     *
     * @param id   部门编号
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
     *
     * @param departmentName 部门名称
     * @param age            年龄
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