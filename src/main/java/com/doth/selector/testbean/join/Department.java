package com.doth.selector.testbean.join;

import com.doth.selector.anno.Id;
import com.doth.selector.anno.Join;

/**
 * @project: test02
 * @package: com.doth.stupidrefframe.testbean
 * @author: doth
 * @creTime: 2025-03-27  11:35
 * @desc: TODO
 * @v: 1.0
 */
public class Department {
    @Id
    private Integer id;
    private String name;

    @Join(fk = "com_id")
    private Company company;


    public Department() {
    }

    public Department(Integer id, String name, Company company) {
        this.id = id;
        this.name = name;
        this.company = company;
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
     * @return company
     */
    public Company getCompany() {
        return company;
    }

    /**
     * 设置
     * @param company
     */
    public void setCompany(Company company) {
        this.company = company;
    }

    public String toString() {
        return "Department{id = " + id + ", name = " + name + ", company = " + company + "}";
    }
}
