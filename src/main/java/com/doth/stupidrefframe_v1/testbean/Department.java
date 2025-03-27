package com.doth.stupidrefframe_v1.testbean;

/**
 * @project: test02
 * @package: com.doth.stupidrefframe_v1.testbean
 * @author: doth
 * @creTime: 2025-03-27  11:35
 * @desc: TODO
 * @v: 1.0
 */
public class Department {
    private Integer id;
    private String name;

    public Department() {
    }

    public Department(Integer id, String name) {
        this.id = id;
        this.name = name;
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

    public String toString() {
        return "Department{id = " + id + ", name = " + name + "}";
    }
}
