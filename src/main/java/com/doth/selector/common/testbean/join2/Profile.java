package com.doth.selector.common.testbean.join2;

import com.doth.selector.annotation.Join;

// Profile.java
public class Profile {
    private Integer id;
    private String email;

    @Join(fk = "user_id", refFK = "id") // 反向关联 user 表
    private User user;
}