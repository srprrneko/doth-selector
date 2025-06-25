package com.doth.selector.supports.testbean;
import com.doth.selector.anno.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class StatDTO {
    private Long total;
    private Integer maxAge;


}