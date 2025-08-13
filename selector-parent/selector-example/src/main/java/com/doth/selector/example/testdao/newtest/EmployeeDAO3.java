package com.doth.selector.example.testdao.newtest;

// import com.doth.stupidrefframe.selector.v1.core.EmployeeDAOImpl;
import com.doth.selector.anno.AutoImpl;
import com.doth.selector.core.Selector;
import com.doth.selector.example.testbean.join.Employee;

import java.util.List;

@AutoImpl
public abstract class EmployeeDAO3 extends Selector<Employee> {

    public static void main(String[] args) {
        // 模拟service层
        // EmployeeDAO3 dao = new EmployeeDAO3Impl();

        // List<Employee> employees = dao.queryByDepartmentId(1);
        // System.out.println(employees);
    }


    // public abstract List<Employee> queryById(Integer id);

    // {id} :id
    public abstract List<Employee> queryByDepartmentName(String name);

    // 懒加载
    // @LazyLoad(Department.class)
    public abstract List<Employee> queryByDepartmentId(Integer id);


    public abstract List<Employee> queryByDepartmentNameVzId(String name, Integer id);


    public List<Employee> queryAll() {
        return dct$().query();
    }




}