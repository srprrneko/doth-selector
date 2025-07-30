package com.doth.selector.supports.testbean;
import com.doth.selector.anno.QueryBean;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@QueryBean
public class StatDTO {
    private Long total;
    private Integer maxAge;


}