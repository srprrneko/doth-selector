package com.doth.selector.testbean.join;

import com.doth.selector.anno.Entity;
import com.doth.selector.anno.Id;
import com.doth.selector.anno.Join;

/**
 * @project: test02
 * @package: com.doth.stupidrefframe.testbean
 * @author: doth
 * @creTime: 2025-03-27  11:35
 * @desc:
 * n+1
 *  select from emp...
 *      -> d_id
 *      select from dept where d_id = ?
 * @v: 1.0
 */
@Entity
public class Employee {

    @Id
    private Integer id; // e_id

    private String name; // as employee_name

    // dept_id -> 1
    // Department -> null

    @Join(fk = "d_id", refFK = "id")
    private Department department;
    /*
        student {
            id
            class_id
        }

        class {
            id
            ...
        }
     */

    public Employee() {
    }

    public Employee(Integer id, String name, Department department) {
        this.id = id;
        this.name = name;
        this.department = department;
    }

    /**
     * 获取
     * @return id
     */
    public Integer getId() {
        return id;
    }

    /**
     * 设置
     * @param id
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * 获取
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * 设置
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取
     * @return department
     */
    public Department getDepartment() {
        return department;
    }

    /**
     * 设置
     * @param department
     */
    public void setDepartment(Department department) {
        this.department = department;
    }

    public String toString() {
        return "Employee{id = " + id + ", name = " + name + ", department = " + department + "}";
    }

}
