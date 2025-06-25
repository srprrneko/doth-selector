package com.doth.selector;

import com.doth.selector.anno.CreateDaoImpl;
import com.doth.selector.core.Selector;
import com.doth.selector.supports.testbean.Students;
import org.junit.Test;

import java.util.List;


@CreateDaoImpl
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