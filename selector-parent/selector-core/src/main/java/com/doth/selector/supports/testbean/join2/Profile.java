package com.doth.selector.supports.testbean.join2;

import com.doth.selector.anno.Join;

// Profile.java
public class Profile {
    private Integer id;
    private String email;

    @Join(fk = "user_id", refPK = "id") // 反向关联 user 表
    private User user;
}