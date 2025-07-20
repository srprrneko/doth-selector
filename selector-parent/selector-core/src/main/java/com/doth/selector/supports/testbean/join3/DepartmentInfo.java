package com.doth.selector.supports.testbean.join3;

import com.doth.selector.anno.Pk;
import com.doth.selector.anno.Join;
import com.doth.selector.anno.OneTo1Breaker;

public class DepartmentInfo {
    @Pk
    private Integer id;

    private String departmentInfoName;

    @Join(fk = "manager_id", refPK = "id")
    @OneTo1Breaker
    private User manager;

    public DepartmentInfo() {
    }

    public DepartmentInfo(Integer id, String departmentName, User manager) {
        this.id = id;
        this.departmentInfoName = departmentName;
        this.manager = manager;
    }

   /**
    * @author: awe fie
    * @date: asefkoef
    * @return java.lang.Integer
    * @description:  adsfsadf
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
     * @return departmentInfoName
     */
    public String getDepartmentInfoName() {
        return departmentInfoName;
    }

    /**
     * 设置
     * @param departmentInfoName
     */
    public void setDepartmentInfoName(String departmentInfoName) {
        this.departmentInfoName = departmentInfoName;
    }

    /**
     * 获取
     * @return user
     */
    public User getManager() {
        return manager;
    }

    /**
     * 设置
     * @param manager
     */
    public void setManager(User manager) {
        this.manager = manager;
    }

    public String toString() {
        return "DepartmentInfo{id = " + id + ", departmentInfoName = " + departmentInfoName + ", user = " + manager + "}";
    }


    // 其他字段的getter/setter省略
}