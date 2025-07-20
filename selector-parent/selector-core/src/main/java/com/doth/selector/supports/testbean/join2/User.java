package com.doth.selector.supports.testbean.join2;

import com.doth.selector.anno.Join;

// User.java
public class User {
    private Integer id;
    private String name;

    @Join(fk = "profile_id", refPK = "id") // 关联 profile 表
    private Profile profile;
}
