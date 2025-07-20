package com.doth.selector.supports.testbean.join3;

import com.doth.selector.anno.DTOConstructor;
import com.doth.selector.anno.QueryBean;
import com.doth.selector.anno.Pk;
import com.doth.selector.anno.MainLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@QueryBean // this anno will check all field make sure isn't primative
public class BookCard {
    @Pk
    private Integer cid;            // 借书卡编号
    private String name;            // 持卡人姓名
    private String sex;             // 持卡人性别（"M"、"F"）
    // private String createDate;      // 办卡日期（建议yyyy-MM-dd）
    private BigDecimal deposit;       // 押金，建议用BigDecimal类型

    // use constructor can create dto directly, append anno: "DTOConstructor"
    @DTOConstructor(id = "bookNameAndSexDTO")
    public BookCard(@MainLevel String name, String sex) {
        // ... you can write down method body logic as your like, but you can write args only, then can auto generate dto easy
    }

}
