package com.doth.selector;

import com.doth.selector.anno.CreateDaoImpl;
import com.doth.selector.core.Selector;
import com.doth.selector.common.testbean.join.Employee;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;


/**
 * @author 贤
 * @version 1.0
 * @date 2025/4/11 22:05
 * @description todo
 */
@CreateDaoImpl
public abstract class EmployeeDAO extends Selector<Employee> {


    /**
     * 根据ID查询员工
     * @return 员工列表
     */
    public List<Employee> queryById1() {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("t0.id", 1);

        return dct$().query2Lst(params); // map为参数
    }



    /*
        ================================================================================================
        我们一般 开发的时候 都会出现 一种 条件组合的形式, 导致我们的报错原因 或是
        ================================================================================================
     */


    /**
     * 根据ID查询员工
     * @return 员工列表
     */
    public List<Employee> queryById2() {
        // 模拟controller自动解析json结构
        Employee employee = new Employee();
        employee.setId(1);

        return dct$().query2Lst(employee); // 这是带实体参数的 ,重载 表示前端需要传递一个 实体条件, 解决了map字符串繁琐手写键值对的问题
    }



    /**
     * 根据姓名和部门ID列表查询员工
     * @return 员工列表
     */
    public List<Employee> queryByNameAndDepartmentIds3() {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();

        params.put("t0.name", "张三%"); // 自动对应 like 张三% 条件为识别到 '%'
        params.put("t1.id", Arrays.asList(1, 2, 3)); // 自动对应 and t1.id in(1,2,3) 条件是识别到 Collection

        return dct$().query2Lst(params);
    }



    /**
     * 根据姓名和部门ID查询员工
     * @return 员工列表
     */
    public List<Employee> queryByNameAndDepartmentId1() {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();

        params.put("t0.name", "张三"); // >> t0 代表主表 todo: 可能考虑后续通过注解标注别名替换的方式, 增加自由度提高阅读性, 但底层执行的还是t0., 修改这个缺点: emp.id -> t0.id
        params.put("t1.id", 1); // >> t1 代表第一张从表 // tN 方式太过于难看, 所以第二版进行了优化 ↓

        return dct$().query2Lst(params);
    }



    /**
     * 根据员工对象查询员工
     * @param employee 员工对象，包含查询条件
     * @return 员工列表
     */
    public List<Employee> queryByNameAndDepartmentId2(Employee employee) {
        return dct$().query2Lst(employee);
    }



    // builder演示比较, 不等于空..
    public List<Employee> queryByNameAndDepartmentIds5() {
        return bud$().query2Lst(builder ->
                builder.like("t0.name", "张三")
                        .in("t1.id", 1, 2, 3)
                        .isNotNull("t0.name") // 这样就有数据了 IsNull 是没有的
        );
    }



    // builder 演示大于, 小于, 大于等于...
    public List<Employee> queryByNameAndDepartmentIds6() {
        return bud$().query2Lst(builder -> // 复杂条件也没有问题, 十分灵活
                builder.
                    like("t0.name", "张%")
                    .in("t1.id", Arrays.asList(1, 2, 3))
                    .gt("t0.id", 1)  // greater than: >
                    .lt("t0.id", 10) // less than: <
                    .ge("t0.id", 1)  // greater equal: >=
                    .le("t0.id", 10) // less equal: <=
        );

    }



    // builder 演示分页
    public List<Employee> queryByNameAndDepartmentIds7() {
        return bud$().query2Lst(builder ->
                builder
                    .page("t0.id", 1, 5) // 游标分页, todo: 传统分页
        );
    }


    // 自定义查询语法规则
    // 1: 查询列表中, 不能包含从表主键, 而是使用主表外键替代, 这是因为内部仅对主表外键做了处理,
    // 虽然可以使用从表主键, 但会自动替换为主表外键, 这是没有意义的
    // 2. 确保连接条件 (on) 中, 左侧是外键, 这是sql的规范, 也是底层映射的强制约束
    // 3. 强制不能通过 as 对列起别名, 这是因为内部做了自动 列别名的处理, 否则会报错
    // 演示raw 查询
    public List<Employee> queryByNameAndDepartmentIds9() {

        return raw$().query2Lst( // 对于不查询的列赋值为空
                   "SELECT t0.id, t0.name, t0.age, t0.d_id, t1.name, t2.id, t2.name FROM employee t0 " +
                   "join department t1 ON t0.d_id = t1.id " +
                   "join company t2 ON t1.com_id = t2.id where t1.id = ? and t0.name = ?",

                1, "李四"
                // , List.of(1,2,3) // 底层自动展开参数
        );
    }
























    /**
     * 通过 员工的 部门名称 查询员工
     * @param name 部门名称
     * @return 员工列表
     */
    public abstract List<Employee> queryByDepartmentName(String name);

    /**
     * 通过 员工姓名 查询员工
     * @param name 员工姓名
     * @return 员工列表
     */
    public abstract List<Employee> queryByName(String name);

    /**
     * 通过部门编号和员工姓名 查询员工
     *
     * @param id   部门编号 where name
     * @param name 员工姓名
     * @return 员工列表
     */
    public abstract List<Employee> queryByDepartmentIdGtVzNameLike(Integer id, String name);

    // @EnhanceBuilder
    public abstract List<Employee> queryByIdGtVzNameLike(int how, String name);
    ////////////////////////////////////////////////// END 固定, 模版方式查询 END //////////////////////////////////////////////////




    ////////////////////////////////////////////////// START 以条件构建者(ConditionBuilder)为核心, 结合模版 查询 START //////////////////////////////////////////////////


    ////////////////////////////////////////////////// END 以条件构建者为核心, 结合模版 查询 END //////////////////////////////////////////////////



    ////////////////////////////////////////////////// START 以自定义sql为核心 START //////////////////////////////////////////////////


    ////////////////////////////////////////////////// END 以自定义sql为核心 END //////////////////////////////////////////////////
}
