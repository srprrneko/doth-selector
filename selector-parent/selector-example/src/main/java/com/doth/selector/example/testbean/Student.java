package com.doth.selector.example.testbean;

import com.doth.selector.anno.CheckE;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@CheckE
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Student {
    private Integer id;
    private String name;
    private Integer age;
    private Integer classId;


}
