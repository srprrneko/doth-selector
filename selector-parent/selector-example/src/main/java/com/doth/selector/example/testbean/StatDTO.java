package com.doth.selector.example.testbean;
import com.doth.selector.anno.CheckE;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@CheckE
public class StatDTO {
    private Long total;
    private Integer maxAge;


}