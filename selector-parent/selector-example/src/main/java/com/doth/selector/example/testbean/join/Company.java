package com.doth.selector.example.testbean.join;

import com.doth.selector.anno.CheckE;
import com.doth.selector.anno.Join;
import com.doth.selector.anno.MorphCr;
import com.doth.selector.anno.Pk;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@CheckE
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Company {
    @Pk
    private Integer id;
    private String name;

    // @Join(fk = "cd_id")
    // private DepartmentInfo departmentInfo;



    @MorphCr(id = "simple")
    public Company(String name) {
        this.name = name ;
    }




}
