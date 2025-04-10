package com.doth.stupidrefframe.selector.v1.core;

import com.doth.loose.testbean.join.Employee;
import com.doth.stupidrefframe.anno.CreateDaoImpl;

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
public abstract class EmployeeDAO extends SelectorV3<Employee>{

    public static void main(String[] args) {
        // 模拟service层
        EmployeeDAO dao = new EmployeeDAOImpl();

        List<Employee> employees = dao.queryById(1);
        System.out.println(employees);
    }


    public abstract List<Employee> queryById(Integer id);



    // todo : 测试失败, 暂时无法解决
    // public abstract List<Employee> queryByDepartmentName(String name);


    public List<Employee> queryAll() {
        return dct$().query2Lst();
    }




}