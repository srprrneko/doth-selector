package com.doth.stupidrefframe_v1.test;

import com.doth.stupidrefframe_v1.selector.Selector;
import com.doth.stupidrefframe_v1.selector.Selector_v1;
import com.doth.loose.testbean.Classes;
import com.doth.loose.testbean.Student;
import com.doth.loose.rubbish.StatDTO;

import java.util.List;
import java.util.Map;

/**
 * @project: test01
 * @package: com.example.inserttry.newdbutiltry01.test
 * @author: doth
 * @creTime: 2025-03-24  23:21
 * @desc: TODO
 * @v: 1.0
 */
public class RawSqlTest {

    public static void method1() {

    }

    public static void main(String[] args) {

        long start = System.currentTimeMillis();


        /* 预期结果：
           [Student{id=1, name='王小明', age=19}] */



        // 直接执行带 IN 子句的 SQL
        String sql2 = "SELECT * FROM student WHERE id IN (?, ?, ?)";
        Object[] params2 = {1, 2, 3};
        List<Student> student2 = Selector.raw(Student.class).query2Lst(sql2, params2);
        System.out.println("ID为1/2/3的学生：" + student2);
        /* 预期结果：
           [
             Student{id=1, name='王小明', age=19},
             Student{id=2, name='李小红', age=20},
             Student{id=3, name='张大力', age=21}

           ] */



        // 执行模糊查询（名字包含“张”）
        String sql3 = "SELECT * FROM student WHERE name LIKE ?";
        Object[] params3 = {"%张%"};
        List<Student> student3 = Selector.raw(Student.class).query2Lst(sql3, params3);
        System.out.println("姓张的学生：" + student3);

        /* 预期结果：
           [Student{id=3, name='张大力', age=21}] */



        // 执行无参数查询（获取年龄最大的3个学生）
        String sql4 = "SELECT * FROM student ORDER BY age DESC LIMIT 3";
        List<Student> student4 = Selector.raw(Student.class).query2Lst(sql4);
        System.out.println("年龄最大的3个学生：" + student4);

        /* 预期结果：
           [
             Student{id=5, name='赵高', age=23},
             Student{id=4, name='刘小刚', age=22},
             Student{id=3, name='张大力', age=21}
           ] */


        // 分页查询（每页2条，第2页）
        int pageSize = 2;
        int pageNum = 2;
        String sql5 = "SELECT * FROM student LIMIT ? OFFSET ?";
        Object[] params5 = {pageSize, (pageNum - 1) * pageSize};
        List<Student> student5 = Selector.raw(Student.class).query2Lst(sql5, params5);
        System.out.println("第2页学生：" + student5);

        /* 预期结果：
           [
             Student{id=3, name='张大力', age=21},
             Student{id=4, name='刘小刚', age=22}
           ] */



        // 正确查询示例
        String sql = "SELECT COUNT(*) AS total, MAX(age) AS maxAge FROM student";
        List<StatDTO> stats = Selector.raw(StatDTO.class).query2Lst(sql);
        System.out.println("统计结果：" + stats);

        /* 预期结果：
           [StatDTO{total=5, maxAge=23}] */



        // 多表 JOIN 查询（学生 + 班级名称）
        String sql7 = "SELECT s.id, s.name, c.class_name " +
                "FROM student s " +  // 确保表名正确
                "LEFT JOIN classes c ON s.class_id = c.id " +
                "WHERE s.age > ?";
        Object[] params7 = {20};

        // 添加类型转换和警告抑制
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) (List<?>)
                Selector.raw(Map.class).query2Lst(sql7, params7);

        System.out.println("年龄>20的学生及班级：" + results);

        /* 预期结果：
           [
             {id=3, name='张大力', class_name='高三(1)班'},
             {id=4, name='刘小刚', class_name='高三(2)班'},
             {id=5, name='赵高', class_name='高三(3)班'}
           ] */



        // 查询不存在的记录
        String sql8 = "SELECT * FROM student WHERE id = ?";
        Object[] params8 = {999};
        List<Student> student8 = Selector.raw(Student.class).query2Lst(sql8, params8);
        System.out.println("不存在的学生：" + student8);

        /* 预期结果：
           [] */


        // 参数数量不足（预期抛出异常）
        String sql9 = "SELECT * FROM student WHERE id = ? AND age = ?";
        Object[] params9 = {1}; // 缺少第二个参数
        try {
            List<Student> student9 = Selector.raw(Student.class).query2Lst(sql9, params9);
        } catch (RuntimeException e) {
            System.out.println("捕获异常：" + e.getMessage());
        }

        /* 预期结果：
           捕获异常：SQL 执行失败: SELECT * FROM student WHERE id = ? AND age = ? */

        String sql10 = "select * from classes";
        List<Classes> classes = Selector.raw(Classes.class).query2Lst(sql10);
        System.out.println("classes = " + classes);

        long end = System.currentTimeMillis();
        System.out.println("(end-start) = " + (end - start));
    }
}
// 定义 DTO 接收统计结果
class StudentHandler extends Selector_v1<Student> {
    public static void method1() {
        // 直接执行带参数的 SQL
        String sql1 = "SELECT * FROM student WHERE id = ?";
        // List<Student> student1 = Selector_v1.raw().query2Lst(sql1, 1);
        // System.out.println("ID为1的学生：" + student1);
    }
}