package com.doth.selector.supports.testbean.join;

import com.doth.selector.anno.QueryBean;
import com.doth.selector.anno.Pk;
import com.doth.selector.anno.Join;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @project: test02
 * @package: com.doth.stupidrefframe.testbean
 * @author: doth
 * @creTime: 2025-03-27  11:35
 * @desc: TODO
 * @v: 1.0
 */
@QueryBean // 加上该注解, 编译期间进行检查
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Department {
    @Pk
    private Integer id;
    private String name; // 框架还内置了检查, 强制要求实体类的字段都必须使用 包装类
    private String dAge;

    @Join(fk = "com_id")
    private Company company;
}
