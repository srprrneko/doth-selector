package com.doth.stupidrefframe_v1.test;


import com.doth.loose.testbean.User;
import com.doth.loose.rubbish.EntityInserter;

// 使用示例
public class Main_Insert {
    public static void main(String[] args) {
        User user = new User();
        user.setId(13);
        user.setUsername("admin");
        // FieldTypeProcessor.processClasses(User.class);

        new EntityInserter<>()
            .setEntity(user)
            .ignoreNull(true)
            .execute();
    }
}


