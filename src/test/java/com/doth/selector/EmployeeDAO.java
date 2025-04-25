package com.doth.selector;

import com.doth.selector.anno.CreateDaoImpl;
import com.doth.selector.core.Selector;
import com.doth.selector.testbean.join.Employee;

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

    // 模拟 service 聚合
    // private EmployeeDAO dao = new EmployeeDAOImpl();

    ////////////////////////////////////////////////// START 固定, 模版方式查询 START //////////////////////////////////////////////////
    /**
     * 查询全部
     * @return 员工列表
     */
    public List<Employee> queryAll() {
        return dct$().query2Lst();
    }




    /**
     * 根据ID查询员工
     * @return 员工列表
     */
    public List<Employee> queryById1() {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("t0.id", 1);

        return dct$().query2Lst(params);
    }




    /**
     * 根据ID查询员工
     * @return 员工列表
     */
    public List<Employee> queryById2() {
        Employee employee = new Employee();
        employee.setId(1);

        return dct$().query2Lst(employee);
    }

    /**
     * 根据姓名和部门ID查询员工
     * @return 员工列表
     */
    public List<Employee> queryByNameAndDepartmentId1() {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();

        params.put("t0.name", "张三"); // >> t0 代表主表 todo: 可能考虑后续通过别名替换的方式, 增加自由度提高阅读性, 但底层执行的还是t0., 修改这个缺点: emp.id -> t0.id
        params.put("t1.id", 1); // >> t1 代表第一张从表

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

    /**
     * 根据姓名和部门ID列表查询员工
     * @return 员工列表
     */
    public List<Employee> queryByNameAndDepartmentIds3() {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();

        params.put("t0.name", "张三%"); // 自动对应 like 张三%
        params.put("t1.id", Arrays.asList(1, 2, 3)); // 自动对应 and t1.id in(1,2,3)

        return dct$().query2Lst(params);
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
     * @param id 部门编号
     * @param name 员工姓名
     * @return 员工列表
     */
    public abstract List<Employee> queryByDepartmentIdVzName(Integer id, String name);

    // @EnhanceBuilder
    public abstract List<Employee> queryByIdGtVzNameLike(int how, String name);
    ////////////////////////////////////////////////// END 固定, 模版方式查询 END //////////////////////////////////////////////////




    ////////////////////////////////////////////////// START 以条件构建者(ConditionBuilder)为核心, 结合模版 查询 START //////////////////////////////////////////////////
    // builder演示比较, 等于空, 不等于空..
    public List<Employee> queryByNameAndDepartmentIds5() {
        return bud$().query2Lst(builder ->
                builder.like("t0.name", "张%")
                        .in("t1.id", Arrays.asList(1, 2, 3))
                        .isNotNull("t0.name")
                );
    }
    // builder 演示大于, 小于, 大于等于...
    public List<Employee> queryByNameAndDepartmentIds6() {

        // then(employee::getName, LIKE);
        return bud$().query2Lst(builder ->
                builder.
                        like("t0.name", "张%")
                        .in("t1.id", Arrays.asList(1, 2, 3))
                        .gt("t0.id", 1)  // greater than: >
                        .lt("t0.id", 10) // less than: <
                        .ge("t0.id", 1)  // greater equal: >=
                        .le("t0.id", 10) // less equal: <=
        );
        /*
            return builder
            .then(emp.getDepartmentName(), LIKE) // emp.department -> 从表 -> t1
            .then(emp.getName(), EQ)          // emp -> 主表 -> t0

         */

        /*
        目前问题: 手动别名, 看着疑惑
            select ... from ... join ... on
            where t0.like = ?
            t1.id in (?,?)

        增强builder
            builder.
                then(emp.getName(), LIKE)

                then(Method)
                    bean = method.getClz()

                    bean.for:fields[]
                        if (f.isAnnoPresent(@Join.clz)) {
                            t++
                        }
                    Object n = method.invoke



         */
        /*
        使用例:
        .then(pojo.getProperty(), LK)
        ...

        clz EnhanceBuilder extends Selector<T> // 继承Selector共享泛型

            static EnhanceBuilder then(Method getter, String equalStrategy) {
            }

            static void end(){
            }





         */
    }
    // builder 演示分页
    public List<Employee> queryByNameAndDepartmentIds7() {
        return bud$().query2Lst(builder ->
                builder
                        // .in("t1.id", Arrays.asList(1, 2, 3))
                        .page("t0.id", 1, 5) // 游标分页, todo: 传统分页
        );
    }
    // builder 演示自定义子句实现排序 todo : 待完善
    public List<Employee> queryByNameAndDepartmentIds8() {
        return bud$().query2Lst(builder ->
                builder.like("t0.name", "张%")
                        // .in("t1.id", Arrays.asList(1, 2, 3))
                        .raw("order by t0.id DESC") // 自定义子句
                // 注意: raw 仅仅支持固定的条件子句, 例如排序, 其他的带参数的无法实现, 下面有替代方式
        );
    }

    ////////////////////////////////////////////////// END 以条件构建者为核心, 结合模版 查询 END //////////////////////////////////////////////////



    ////////////////////////////////////////////////// START 以自定义sql为核心 START //////////////////////////////////////////////////
    // 自定义语法规则
    // 1: 查询列表中, 不能包含从表主键, 而是使用主表外键替代, 这是因为内部仅对主表外键做了处理,
    // 虽然可以使用从表主键, 但会自动替换为主表外键, 这是没有意义的
    // 2. 确保连接条件 (on) 中, 左侧是外键, 这是sql的规范, 也是底层映射的强制约束
    // 3. 强制不能通过 as 对列起别名, 这是因为内部做了自动 列别名的处理, 否则会报错
    // 演示raw 查询
    public List<Employee> queryByNameAndDepartmentIds9() {
        return raw$().query2Lst(
                // 自定义不依赖固定表别名, 但是依赖固定表列别名
                // "SELECT t0.id, t0.name, t0.d_id, " + // 查询从表主键使用主表外键替代
                //         "t1.name FROM employee t0 " +
                //         "LEFT JOIN department t1 ON t0.d_id = t1.id " +
                //         "WHERE t0.name LIKE ? AND t1.id IN (?, ?, ?) " +
                //         "ORDER BY t0.id DESC",
                "SELECT emp.id, emp.name, emp.d_id, " + // 查询从表主键使用主表外键替代
                        "dep.name FROM employee emp " +
                        "JOIN department dep ON emp.d_id = dep.id " +
                        "WHERE emp.name LIKE ? AND dep.id IN (?, ?, ?) " +
                        "ORDER BY emp.id DESC",

                "张%", 1, 2, 3
        );
    }

    ////////////////////////////////////////////////// END 以自定义sql为核心 END //////////////////////////////////////////////////
}
