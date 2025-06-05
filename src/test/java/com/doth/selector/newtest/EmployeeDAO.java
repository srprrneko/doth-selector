package com.doth.selector.newtest;

import com.doth.selector.anno.CreateDaoImpl;
import com.doth.selector.anno.UseDTO;
// import com.doth.selector.common.testbean.join.BaseEmpInfo;
import com.doth.selector.common.testbean.join.BaseEmpInfo;
import com.doth.selector.core.Selector;
import com.doth.selector.common.testbean.join.Employee;
import com.doth.selector.executor.supports.lambda.LambdaFieldPathResolver;
import com.doth.selector.executor.supports.lambda.SFunction;
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
 *
 *  ================================================================================================
 *
 *  总结自动dto遇到的问题
 *  1.同参数构造方法无法控制
 *      解决: 考虑同样支持方法
 *  2.
 *
 */
@CreateDaoImpl
public abstract class EmployeeDAO extends Selector<Employee> {

    public static void main(String[] args) {
        EmployeeDAO dao = new EmployeeDAOImpl();
        // List<Employee> impl = dao.impl();

        List<BaseEmpInfo> implDto = dao.dtoImpl();

        // System.out.println("impl.get(0).getClass() = " + impl.get(0).getClass());
        // System.out.println("impl = " + impl);
        implDto.forEach(e -> {
            System.out.println("================================================================================");
            System.out.println("e.getId() = " + e.getId());
            System.out.println("e.getName() = " + e.getName());
            System.out.println("e.getDepartmentName() = " + e.getDepartmentName());
            System.out.println("e.getCompanyName() = " + e.getCompanyName());
            System.out.println("================================================================================");
        });

        System.out.println("impl = " + implDto);


    }
    @Test
    public void testNew() {
        List<Employee> impl = this.impl();
        System.out.println("impl.get(0).getClass() = " + impl.get(0).getClass());
        System.out.println("impl = " + impl);
    }

    // @UseDTO(id = "empSimple")
    public List<Employee> impl() {
        return bud$().query2Lst(builder ->
                builder.eq(e -> e.getDepartment().getName(), "研发部")
        );
    }

    // @UseDTO(id = "baseEmpInfo")
    public List<BaseEmpInfo> dtoImpl() {
        /*
            现在有一个情况:
            1.当使用了 dto 模式的时候, 泛型不再共享, lambda 表达式也不再生效

            思路: 因为自动dto的缘故, dto始终只可能存在一层, 所以最终eq的重载只可能存在一个
         */
        // return Selector.bud$(BaseEmpInfo.class).query2Lst(builder ->
        //             builder.eq("t1.name", "研发部")
        // );
        return queryDtoList(BaseEmpInfo.class, builder -> {
            // 这时 builder 的泛型是 ConditionBuilder<Employee>，所以 e.getDepartment().getName() 可以提示
            builder.eq(e -> e.getDepartment().getName(), "研发部");
        });
    }

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
        System.out.println("result = " + result);

        System.out.println("result.getClass() = " + result.get(0).getClass());

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