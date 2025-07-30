package com.doth.selector.supports.testbean.join3;

import com.doth.selector.anno.QueryBean;
import com.doth.selector.anno.Pk;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author YourName
 * @creTime 2025-06-20
 * @desc 专业信息实体
 */
@QueryBean
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