package com.doth.selector.example.testdao;

import com.doth.selector.anno.AutoImpl;
import com.doth.selector.core.Selector;
import com.doth.selector.example.testbean.Students;

import java.util.List;


@AutoImpl
public abstract class StudentsDAO extends Selector<Students> {

    public abstract List<Students> listByStudentId(Integer id);

    // @Test
    // public void test() {
    //     StudentsDAO dao = new StudentsDAOImpl();
    //     List<Students> students = dao.listByStudentId(1);
    //     System.out.println("students = " + students);
    //
    // }
}