package com.doth.stupidrefframe.selector.v1.core;

import com.doth.loose.testbean.join.Employee;

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
 */
public class EmployeeDAO extends SelectorV2<Employee>{

    public static void main(String[] args) {
        // 模拟service层
        // EmployeeDAO dao = new EmployeeDAO();
        // List<Employee> employees = dao.queryAll();
        // System.out.println(employees);
    }
    public List<Employee> queryAll() {
        return dct$().query2Lst();
    }


    // public  List<Employee> queryByName(){};

}