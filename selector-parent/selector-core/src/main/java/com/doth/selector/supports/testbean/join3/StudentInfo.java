package com.doth.selector.supports.testbean.join3;

import com.doth.selector.anno.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Year;
import java.util.Date;

/**
 * @author YourName
 * @creTime 2025-06-20
 * @desc 学生信息实体
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentInfo {

    @Id
    private Integer studentId;
    private String studentName;
    private String gender;
    private String idCard;
    private Date birthDate;
    private Date admissionTime;
    private Date lastLogin;
    private Date dailyCheckIn;
    private String phone;
    private String email;
    private String homeAddress;
    private Year enrollmentYear;
    

    
    private String className;
    private String studentStatus;
    private Double creditsEarned;
    private Double gpa;
    private String biography;

    @Join(fk = "major_id", refFK = "major_id")
    private MajorInfo majorInfo;

    @DTOConstructor(id = "baseStudentInfo")
    public StudentInfo(
            @MainLevel
            Integer studentId,
            String studentName,
            String gender,
            String idCard,
            String phone,
            Double gpa,

            @JoinLevel(clz = MajorInfo.class, attrName = "majorInfo")
            Integer _majorId,
            String _majorName,
            String _director
    ) {}


}