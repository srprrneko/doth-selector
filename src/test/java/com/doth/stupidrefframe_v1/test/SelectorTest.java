package com.doth.stupidrefframe_v1.test;

import com.doth.stupidrefframe_v1.testbean.Student;
import com.doth.rubbish.EntitySelector;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static com.doth.rubbish.EntitySelector.query2Lst;

/**
 *@project: test01
 *@package: com.example.inserttry.newdbutiltry01.test
 *@author: doth
 *@creTime: 2025-03-24  10:58
 *@desc: TODO
 *@v: 1.0
*/
public class SelectorTest {

    public static void main(String[] args) {
        // 自定义 SQL（支持复杂 JOIN、子查询等）
        String sql = "SELECT id, name, age FROM student WHERE age > ?";
        Object[] params = {25};  // 参数数组

        // 直接执行 SQL
        List<Student> users = EntitySelector.query2Lst(sql, Student.class, params);
        System.out.println(users);

    }
    @Test // exeQuery 方法（带 ConditionBuilder 参数）
    public void test1() {
        // 示例1：年龄大于18的查询（完全复制您的示例）
        List<Student> students1 = query2Lst(Student.class, builder -> {
            builder.gt("age", 18);
        });
        System.out.println("成年学生：" + students1 );
        /* 预期结果（17条）：
        包含id=1,2,3,5,7,8,9,10,11,12,13,15,16,17,18,19,20
        排除id=4(age=18),6(age=17),14(age=18) */
    }


    @Test // exeQuery 方法（带原生条件参数）
    public void test2() {
        // 测试查询 "名字以张开头" 且 "年龄在18、19、20岁" 的学生，按id降序排列，取第一条结果
        LinkedHashMap<String, Object> conditions = new LinkedHashMap<>(); // 保证条件顺序
        conditions.put("name", "张%");   // LIKE 模糊查询（注意包含通配符）
        conditions.put("age", Arrays.asList(18, 19, 20)); // IN 查询

        List<Student> student = query2Lst(
                Student.class,
                conditions,
                "ORDER BY id DESC LIMIT 1" // 附加子句
        );
        System.out.println(student);
    }

    @Test // exeQuery 方法（带 Map 参数）
    public void test3() {
        // 示例3：Map条件查询
        Student student = new Student();
        student.setAge(25);
        student.setName("张%");


        List<Student> students3 = query2Lst(Student.class, student);
        System.out.println("25岁张姓学生：" + students3);
        /* 预期结果（2条）：
        id=5(张小芳),17(小龙女) */
    }
}
