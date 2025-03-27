package com.doth.stupidrefframe_v1.selector;

import com.doth.stupidrefframe_v1.testbean.Classes;
import com.doth.stupidrefframe_v1.testbean.Student;

import java.util.List;

/**
 * @project: test02
 * @package: com.doth.stupidrefframe_v1.selector
 * @author: doth
 * @creTime: 2025-03-27  16:05
 * @desc: TODO
 * @v: 1.0
 */
public class StudentDAO extends Selector_v1<Student>{
    public static void main(String[] args) {
        StudentDAO dao = new StudentDAO();
        List<Student> list = dao.queryDep();
        System.out.println(list);
    }

    public List<Student> queryDep() {
        String sql = "select * from student";
        return raw().query2Lst(sql);
    }
}
