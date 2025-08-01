package com.doth.selector.supports.testbean.join3;

import com.doth.selector.anno.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.Year;

/**
 * @author YourName
 * @creTime 2025-06-20
 * @desc 学生信息实体
 */
@QueryBean
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentInfo {

    @Pk
    private Integer studentId;
    private String studentName;
    private String gender;
    private String idCard;
    private LocalDateTime birthDate;
    private LocalDateTime admissionTime;
    private LocalDateTime lastLogin;
    private LocalDateTime dailyCheckIn;
    private String phone;
    private String email;
    private String homeAddress;
    private Year enrollmentYear;
    

    
    private String className;
    private String studentStatus;
    private Double creditsEarned;
    private Double gpa;
    private String biography;

    @Join(fk = "major_id", refPK = "major_id")
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