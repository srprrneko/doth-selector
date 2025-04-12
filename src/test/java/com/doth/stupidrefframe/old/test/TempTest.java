package com.doth.stupidrefframe.old.test;

import com.doth.stupidrefframe.selector.v1.loose.testbean.Student;
import com.doth.stupidrefframe.selector.v1.loose.testbean.join.StudentCourseDTO;
import com.doth.stupidrefframe.selector.v1.loose.rubbish_since411.SelectorV2;

import java.util.List;


/**
 * @project: test01
 * @package: com.example.inserttry.newdbutiltry01.test
 * @author: doth
 * @creTime: 2025-03-25  10:51
 * @desc: TODO
 * @v: 1.0
 */
public class TempTest {
    /*
        想法 : 减少所有方法中的必要参数classBean, 通过传入构造方法 的方式实现, 通过beanClass 内部成员变量 传递

        还有更离谱的, 将参数封装成一个类, 对这个, 根据参数对应重载, 这太麻烦了, 还是使用
        或是说使用map存储对应的实例值, 键对应泛型, 如果不存在则创建, 存在则复用

        如果要封装对象属性时, 可以使用键值键所有的返回列存储起来, 然后在判断是否为自定义类型, 然后在进行封装
        也可以通过注解去判断, 这样更省事, 然后优先注解

        也可以
     */
    public static void main(String[] args) {
        String baseSql = "SELECT s.name AS studentName, s.age AS studentAge, " +
                "c.course_name AS courseName, c.credit, " +
                "sc.selected_at AS selectedDate, sc.score " +
                "FROM students s " +
                "INNER JOIN student_courses sc ON s.student_id = sc.student_id " +
                "INNER JOIN courses c ON sc.course_id = c.course_id";
        // 执行查询
        List<StudentCourseDTO> list = SelectorV2.bud(StudentCourseDTO.class).
                query2Lst(baseSql,
                        builder -> {
                            builder.between("sc.selected_at", "2023-09-01", "2023-12-31")
                                    .gt("c.credit", 2.5)
                                    .page("s.student_id", 1, 5);
                        });
        System.out.println("list = " + list);


        // 构建器查询
        List<Student> users = SelectorV2.bud(Student.class)
                .query2Lst(b -> b.between("age", 18, 30));

        System.out.println("users = " + users);
        // 原生SQL查询
        List<Student> student = SelectorV2.raw(Student.class)
                .query2Lst("SELECT * FROM student WHERE age > ?", 18);

        System.out.println("student = " + student);

        System.out.println("SelectorV2.direct(Student.class).query2Lst() = " + SelectorV2.dct(Student.class).query2Lst());
    }
}
