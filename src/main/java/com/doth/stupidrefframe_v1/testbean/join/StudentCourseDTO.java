package com.doth.stupidrefframe_v1.testbean.join;

import com.doth.stupidrefframe_v1.anno.Entity;

import java.math.BigDecimal;
import java.util.Date;

@Entity
public class StudentCourseDTO {
    private String studentName;
    private Integer studentAge;
    private String courseName;
    private BigDecimal credit;
    private Date selectedDate;
    private BigDecimal score;


    public StudentCourseDTO() {
    }

    public StudentCourseDTO(String studentName, Integer studentAge, String courseName, BigDecimal credit, Date selectedDate, BigDecimal score) {
        this.studentName = studentName;
        this.studentAge = studentAge;
        this.courseName = courseName;
        this.credit = credit;
        this.selectedDate = selectedDate;
        this.score = score;
    }

    /**
     * 获取
     * @return studentName
     */
    public String getStudentName() {
        return studentName;
    }

    /**
     * 设置
     * @param studentName
     */
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    /**
     * 获取
     * @return studentAge
     */
    public Integer getStudentAge() {
        return studentAge;
    }

    /**
     * 设置
     * @param studentAge
     */
    public void setStudentAge(Integer studentAge) {
        this.studentAge = studentAge;
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
    public BigDecimal getCredit() {
        return credit;
    }

    /**
     * 设置
     * @param credit
     */
    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    /**
     * 获取
     * @return selectedDate
     */
    public Date getSelectedDate() {
        return selectedDate;
    }

    /**
     * 设置
     * @param selectedDate
     */
    public void setSelectedDate(Date selectedDate) {
        this.selectedDate = selectedDate;
    }

    /**
     * 获取
     * @return score
     */
    public BigDecimal getScore() {
        return score;
    }

    /**
     * 设置
     * @param score
     */
    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String toString() {
        return "StudentCourseDTO{studentName = " + studentName + ", studentAge = " + studentAge + ", courseName = " + courseName + ", credit = " + credit + ", selectedDate = " + selectedDate + ", score = " + score + "}";
    }
}