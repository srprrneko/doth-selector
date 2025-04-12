package com.doth.stupidrefframe.old.test;

import com.doth.stupidrefframe.selector.v1.loose.testbean.join.StudentCourseDTO;

import java.util.List;

import static com.doth.stupidrefframe.selector.v1.loose.rubbish.EntitySelector.query2Lst;
/**
 * @project: test01
 * @package: com.example.inserttry.newdbutiltry01.test
 * @author: doth
 * @creTime: 2025-03-25  20:15
 * @desc: TODO
 * @v: 1.0
 */
public class BuilderSqlTest {
    public static void main(String[] args) {
        // 自定义基础SQL（包含正确的JOIN）
        String baseSql = "SELECT s.name AS studentName, s.age AS studentAge, " +
                "c.course_name AS courseName, c.credit, " +
                "sc.selected_at AS selectedDate, sc.score " +
                "FROM students s " +
                "INNER JOIN student_courses sc ON s.student_id = sc.student_id " +
                "INNER JOIN courses c ON sc.course_id = c.course_id";

        // 执行查询
        List<StudentCourseDTO> results = query2Lst(baseSql, StudentCourseDTO.class,
                builder -> {
                    builder.between("sc.selected_at", "2023-09-01", "2023-12-31")
                            .gt("c.credit", 2.5)
                            .page("s.student_id", 1, 5);
                }

        );
        results.forEach(System.out::println);


        // 示例1：正确使用别名映射（三表连接）
        List<StudentCourseDTO> results1 = query2Lst(StudentCourseDTO.class, builder -> {
            builder.between("sc.selected_at", "2023-09-01", "2023-12-31")
                    .gt("c.credit", 2.5)
                    .page("s.student_id", 1000, 5);
        });
        System.out.println("秋季学期高学分课程选课记录：" + results1);
/* 预期结果：
[StudentCourseDTO{studentName='张三', courseName='数据库原理', credit=3.5},
 StudentCourseDTO{studentName='李四', courseName='算法分析', credit=4.0}] */

// 示例2：列名未匹配异常（问题示例）
        try {
            List<StudentCourseDTO> errorResults = query2Lst(StudentCourseDTO.class, builder -> {
                builder.eq("course_name", "数据库原理"); // 未使用别名
            });
        } catch (Exception e) {
            System.out.println("捕获异常: " + e.getMessage());
        }
/* 预期异常：
NoColumnExistException: 列 course_name 不存在于实体类 StudentCourseDTO
（正确做法应使用c.course_name AS courseName） */

// 示例3：正确结果集映射（包含NULL值）
        List<StudentCourseDTO> results2 = query2Lst(StudentCourseDTO.class, builder -> {
            builder.isNull("sc.score")
                    .eq("s.name", "张三");
        });
        System.out.println("张三未出成绩的课程：" + results2);
/* 预期结果：
[StudentCourseDTO{studentName='张三', courseName='机器学习', score=null}] */

// 示例4：分页参数错误（问题示例）
        List<StudentCourseDTO> pageResults = query2Lst(StudentCourseDTO.class, builder -> {
            builder.page("selected_at", "2023-12-25", 3) // 错误游标字段
                    .raw("sc.selected_at DESC");
        });
/* 问题分析：
1. 游标字段selected_at未带表别名（应使用sc.selected_at）
2. orderBy方法不存在于ConditionBuilder
3. 分页逻辑依赖WHERE条件中的游标字段 */

// 正确分页写法：
        List<StudentCourseDTO> correctPageResults = query2Lst(StudentCourseDTO.class, builder -> {
            builder.page("sc.record_id", 10, 5) // 正确使用关联表的主键
                    .gt("c.credit", 2.0);
        });
    }
}
