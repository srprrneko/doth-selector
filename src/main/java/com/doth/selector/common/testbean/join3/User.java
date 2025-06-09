package com.doth.selector.common.testbean.join3;

import com.doth.selector.anno.Id;
import com.doth.selector.anno.Join;
import com.doth.selector.anno.OneToOne;

// 实体类定义
public class User {
    @Id
    private Integer id;

    private String name;

    @Join(fk = "department_id", refFK = "id")
    // @OneToOne
    private DepartmentInfo departmentInfo;

    // @Join(fk = "employee_id", refFK = "id")
    // private Employee employee;
    public User() {
    }

    public User(Integer id, String name, DepartmentInfo department) {
        this.id = id;
        this.name = name;
        this.departmentInfo = department;
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
    public DepartmentInfo getDepartment() {
        return departmentInfo;
    }

    /**
     * 设置
     * @param department
     */
    public void setDepartment(DepartmentInfo department) {
        this.departmentInfo = department;
    }

    public String toString() {
        return "User{id = " + id + ", name = " + name + ", department = " + departmentInfo + "}";
    }

    // 其他字段的getter/setter省略
}
