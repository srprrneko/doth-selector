package com.doth.selector.supports.testbean;

import com.doth.selector.anno.QueryBean;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;


@QueryBean
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Students {
    private Integer studentId;
    private String name;
    private Integer age;
    private Timestamp createdAt;
}
