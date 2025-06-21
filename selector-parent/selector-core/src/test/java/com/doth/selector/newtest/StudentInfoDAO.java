package com.doth.selector.newtest;

import com.doth.selector.anno.CreateDaoImpl;
import com.doth.selector.core.Selector;
import com.doth.selector.supports.testbean.join3.StudentInfo;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.List;

/**
 * @author 贤
 * @version 1.0
 * @date 2025/6/20 0:36
 * @description
 */
@CreateDaoImpl
@Slf4j
public abstract class StudentInfoDAO extends Selector<StudentInfo> {

    public abstract List<StudentInfo> queryByStudentId(Integer id);

    @Test
    public void testListFull() {
        StudentInfoDAO dao = new StudentInfoDAOImpl();
        long start = System.currentTimeMillis();
        // for (int i = 0; i < 50000; i++) {
             dao.queryByStudentId(1);
        // }
        long end = System.currentTimeMillis();
        log.info("(end - start) : {}", (end - start));
    }

    public static void main(String[] args) {

    }

}