package com.doth.selector.supports.testbean;

import com.doth.selector.anno.QueryBean;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@QueryBean
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Student {
    private Integer id;
    private String name;
    private Integer age;
    private Integer classId;


}
