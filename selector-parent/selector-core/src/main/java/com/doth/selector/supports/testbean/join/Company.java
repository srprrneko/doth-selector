package com.doth.selector.supports.testbean.join;

import com.doth.selector.anno.CheckE;
import com.doth.selector.anno.Join;
import com.doth.selector.anno.MorphCr;
import com.doth.selector.anno.Pk;
import com.doth.selector.supports.testbean.join3.DepartmentInfo;
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
@CheckE
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Company {
    @Pk
    private Integer id;
    private String name;

    @Join(fk = "cd_id")
    private DepartmentInfo departmentInfo;

    @MorphCr(id = "simple")
    public Company(String name) {
        this.name = name ;
    }




}
