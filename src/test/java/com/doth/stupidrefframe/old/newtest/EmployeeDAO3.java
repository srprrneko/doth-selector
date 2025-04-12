package com.doth.stupidrefframe.old.newtest;

// import com.doth.stupidrefframe.selector.v1.core.EmployeeDAOImpl;
import com.doth.stupidrefframe.selector.v1.core.SelectorV3;
import com.doth.stupidrefframe.selector.v1.loose.testbean.join.Employee;
import com.doth.stupidrefframe.selector.v1.anno.CreateDaoImpl;

import java.util.List;

/**
 * @author 贤
 * @version 1.0
 * @date 2025/4/8 19:11
 * @description todo
 *
 *   想法: 通过在dao层定义抽象方法, 识别方法名, 动态生成sql
 *       例如, 假设主表是employee: 方法名getByName = select e.name, e.d_id, d.name, d.com_id, c.name from employee join... where e.name = ?
 *
 *       无法实现 : 抽象类无法被实例化 -> 解决方式, 使用代理
 *
 *       其他想法 : 后续可以使用代理类生成实体类的 public entity setter(xx), 然后通过
 *          query.get()
 *              .setName(value)
 *              .setAge(value)
 *              .setAge(value)
 *              .
 *              ...
 *          通过这样的方式代替方法名拆解的方式, 或者说, 两种方式都可以用?
 *
 */
@CreateDaoImpl
public abstract class EmployeeDAO3 extends SelectorV3<Employee> {

    public static void main(String[] args) {
        // 模拟service层
        EmployeeDAO3 dao = new EmployeeDAO3Impl();

        List<Employee> employees = dao.queryByDepartmentId(1);
        System.out.println(employees);
    }


    // public abstract List<Employee> queryById(Integer id);

    // {id} :id
    public abstract List<Employee> queryByDepartmentName(String name);

    // 懒加载
    // @LazyLoad(Department.class)
    public abstract List<Employee> queryByDepartmentId(Integer id);


    public abstract List<Employee> queryByDepartmentNameVzId(String name, Integer id);


    public List<Employee> queryAll() {
        return dct$().query2Lst();
    }




}