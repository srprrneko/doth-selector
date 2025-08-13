package com.doth.selector.example.testbean.join3;

import com.doth.selector.anno.CheckE;
import com.doth.selector.anno.MorphCr;
import com.doth.selector.anno.Pk;
import com.doth.selector.anno.MainLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@CheckE
public class BookCard {
    @Pk
    private Integer cid;            // 借书卡编号
    private String name;            // 持卡人姓名
    private String sex;             // 持卡人性别（"M"、"F"）
    private BigDecimal deposit;       // 押金，建议用BigDecimal类型

    @MorphCr(id = "bookNameAndSexDTO")
    public BookCard(@MainLevel String name, String sex) {
    }

}
