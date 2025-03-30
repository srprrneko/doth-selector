package com.doth.loose.testbean;


import com.doth.stupidrefframe_v1.anno.Entity;

/**
 * @project: classFollowing
 * @package: reflect.trysomething
 * @author: doth
 * @creTime: 2025-03-21  08:54
 * @desc: TODO
 * @v: 1.0
 */
@Entity
public class User {
    private Integer id;
    private String username;

    public User() {
    }

    public User(Integer id, String username) {
        this.id = id;
        this.username = username;
    }


    public String toString() {
        return "com.example.User{id = " + id + ", username = " + username + "}";
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
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置
     * @param username
     */
    public void setUsername(String username) {
        this.username = username;
    }
}
