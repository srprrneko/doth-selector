package com.doth.selector.supports.testbean.join;

import com.doth.selector.anno.DTOConstructor;
import com.doth.selector.anno.Entity;
import com.doth.selector.anno.Pk;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @project: test02
 * @package: com.doth.stupidrefframe.testbean.join
 * @author: doth
 * @creTime: 2025-03-29  16:02
 * @desc: TODO
 * @v: 1.0
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Company {
    @Pk
    private Integer id;
    private String name;


    @DTOConstructor(id = "simple")
    public Company(String name) {
        this.name = name ;
    }

}
