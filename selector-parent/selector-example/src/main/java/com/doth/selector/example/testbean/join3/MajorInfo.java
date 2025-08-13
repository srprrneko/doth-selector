package com.doth.selector.example.testbean.join3;

import com.doth.selector.anno.CheckE;
import com.doth.selector.anno.Pk;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@CheckE
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MajorInfo {

    @Pk
    private Integer majorId;
    private String majorName;
    private String department;
    private Date establishmentDate;
    private String director;
    private String contactPhone;
    private String description;
    private Integer creditRequirement;

}