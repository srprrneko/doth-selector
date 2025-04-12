package com.doth.stupidrefframe.old.newtest;

import com.doth.stupidrefframe.selector.v1.loose.testbean.Student;
import com.doth.stupidrefframe.selector.v1.loose.rubbish_since411.SelectorV2;

import java.util.List;

/**
 * @project: test02
 * @package: com.doth.stupidrefframe.selector
 * @author: doth
 * @creTime: 2025-03-27  16:05
 * @desc: TODO
 * @v: 1.0
 */
public class StudentDAO extends SelectorV2<Student> {
    public static void main(String[] args) {
        // 模拟在service层聚合
        StudentDAO dao = new StudentDAO();
        // List<Student> list = dao.queryDep();
        // System.out.println("自定义查询全部" +list);
        // System.out.println();

        List<Student> students = dao.queryStudent();
        System.out.println("固定方式查询全部" + students);
        System.out.println();


        // List<Student> students1 = dao.queryStudentById(1);
        // System.out.println("构建者方式查询指定id的学生" + students1);
        // System.out.println();
    }

    public List<Student> queryDep() {
        String sql = "select * from student where id = ?";
        return raw().query2Lst(sql, 1);
    }

    public List<Student> queryStudent() {
        Student student = new Student();
        student.setName("%王");

        // Map
        return dct().query2Lst(student);
    }

    public List<Student> queryStudentById(Integer id) {
        return bud().query2Lst(builder ->
                builder
                        .eq("id", id)
                        .between("age", 10, 20)
                        // .page()
        );
    }
}
