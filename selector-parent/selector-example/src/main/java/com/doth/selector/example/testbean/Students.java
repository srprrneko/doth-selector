package com.doth.selector.example.testbean;

import com.doth.selector.anno.CheckE;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;


@CheckE
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Students {
    private Integer studentId;
    private String name;
    private Integer age;
    private Timestamp createdAt;
}
