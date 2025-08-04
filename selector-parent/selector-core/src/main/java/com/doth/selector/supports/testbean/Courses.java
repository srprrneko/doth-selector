package com.doth.selector.supports.testbean;


import com.doth.selector.anno.CheckE;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@CheckE
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Courses {
    private Integer courseId;
    private String courseName;
    private Double credit;

}