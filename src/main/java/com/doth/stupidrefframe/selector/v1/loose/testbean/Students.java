package com.doth.stupidrefframe.selector.v1.loose.testbean;

import com.doth.stupidrefframe.selector.v1.anno.Entity;

import java.security.Timestamp;

/**
 * @project: test01
 * @package: com.example.inserttry.newdbutiltry01.test.testbean
 * @author: doth
 * @creTime: 2025-03-25  20:23
 * @desc: TODO
 * @v: 1.0
 */
@Entity
public class Students {
    private Integer studentId;
    private String name;
    private Integer age;
    private Timestamp createdAt;


    public Students() {
    }

    public Students(Integer studentId, String name, Integer age, Timestamp createdAt) {
        this.studentId = studentId;
        this.name = name;
        this.age = age;
        this.createdAt = createdAt;
    }

    /**
     * 获取
     * @return studentId
     */
    public Integer getStudentId() {
        return studentId;
    }

    /**
     * 设置
     * @param studentId
     */
    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
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
     * @return age
     */
    public Integer getAge() {
        return age;
    }

    /**
     * 设置
     * @param age
     */
    public void setAge(Integer age) {
        this.age = age;
    }

    /**
     * 获取
     * @return createdAt
     */
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置
     * @param createdAt
     */
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String toString() {
        return "Students{studentId = " + studentId + ", name = " + name + ", age = " + age + ", createdAt = " + createdAt + "}";
    }

    // Getter/Setter...
}
