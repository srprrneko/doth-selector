package com.doth.selector.common.testbean.join;

import com.doth.selector.anno.Entity;
import com.doth.selector.anno.Id;

/**
 * @project: test02
 * @package: com.doth.stupidrefframe.testbean.join
 * @author: doth
 * @creTime: 2025-03-29  16:02
 * @desc: TODO
 * @v: 1.0
 */
@Entity
public class Company {
    @Id
    private Integer id;
    private String name;

    public Company() {
    }

    public Company(Integer id, String name) {
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
        return "Company{id = " + id + ", name = " + name + "}";
    }
}
