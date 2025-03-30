package com.doth.loose.testbean;

public class Classes {
    private Integer id;
    private String className;  // 对应表中的 class_name 字段

    public Classes() {
    }

    public Classes(Integer id, String className) {
        this.id = id;
        this.className = className;
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
     * @return className
     */
    public String getClassName() {
        return className;
    }

    /**
     * 设置
     * @param className
     */
    public void setClassName(String className) {
        this.className = className;
    }

    public String toString() {
        return "Class{id = " + id + ", className = " + className + "}";
    }

    // 构造方法、Getter/Setter 省略

}