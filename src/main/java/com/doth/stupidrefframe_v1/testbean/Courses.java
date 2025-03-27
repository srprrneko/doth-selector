package com.doth.stupidrefframe_v1.testbean;


import com.doth.stupidrefframe_v1.anno.Entity;

@Entity
public class Courses {
    private Integer courseId;
    private String courseName;
    private Double credit;

    public Courses() {
    }

    public Courses(Integer courseId, String courseName, Double credit) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.credit = credit;
    }

    /**
     * 获取
     * @return courseId
     */
    public Integer getCourseId() {
        return courseId;
    }

    /**
     * 设置
     * @param courseId
     */
    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    /**
     * 获取
     * @return courseName
     */
    public String getCourseName() {
        return courseName;
    }

    /**
     * 设置
     * @param courseName
     */
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    /**
     * 获取
     * @return credit
     */
    public Double getCredit() {
        return credit;
    }

    /**
     * 设置
     * @param credit
     */
    public void setCredit(Double credit) {
        this.credit = credit;
    }

    public String toString() {
        return "Courses{courseId = " + courseId + ", courseName = " + courseName + ", credit = " + credit + "}";
    }


    // Getter/Setter...
}