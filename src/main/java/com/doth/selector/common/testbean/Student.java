package com.doth.selector.common.testbean;

import com.doth.selector.anno.Entity;

/**
 * @project: classFollowing
 * @package: reflect.execrise8refarr
 * @author: doth
 * @creTime: 2025-03-19  09:33
 * @desc: TODO
 * @v: 1.0
 */
@Entity
public class Student {
    private Integer id;
    private String name;
    private Integer age;
    private Integer classId;  // 对应表中的 class_id 字段
    // Student{id, name, 班级{id, name}}

    public Student() {
    }

    public Student(Integer id, String name, Integer age, Integer classId) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.classId = classId;
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
     * @return classId
     */
    public Integer getClassId() {
        return classId;
    }

    /**
     * 设置
     * @param classId
     */
    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public String toString() {
        return "Student{id = " + id + ", name = " + name + ", age = " + age + ", classId = " + classId + "}";
    }
}
