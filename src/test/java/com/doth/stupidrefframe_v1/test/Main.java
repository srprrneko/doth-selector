package com.doth.stupidrefframe_v1.test;


import com.doth.loose.testbean.Student;

import java.util.List;

import static com.doth.loose.rubbish.EntitySelector.query2Lst;

/**
 * @project: test01
 * @package: com.example.inserttry.newdbutiltry01.test
 * @author: doth
 * @creTime: 2025-03-24  01:06
 * @desc: TODO
 * @v: 1.0
 */
public class Main {
    public static void main(String[] args) {
        // 示例1：基础等值查询（保持原样）
        List<Student> students1 = query2Lst(Student.class, builder -> {
            builder.eq("id", 1);
        });
        System.out.println("ID为1的学生：" + students1);
        /* 预期结果：
        [Student{id=1, name='王小明', age=19}] */

        // 示例2：大于条件查询（保持原样）
        List<Student> students2 = query2Lst(Student.class, builder -> {
            builder.gt("age", 18);
        });
        System.out.println("成年学生：" + students2);
        /* 预期结果（18条）：
        包含id=1,2,3,5,7,8,9,10,11,12,13,15,16,17,18,19,20
        排除id=4(age=18),6(age=17),14(age=18) */

        // 示例3：范围+模糊查询（保持原样）
        List<Student> students3 = query2Lst(Student.class, builder -> {
            builder.between("age", 20, 30)
                    .like("name", "%张%");
        });
        System.out.println("20-30岁张姓学生：" + students3);
        /* 预期结果（6条）：
        id=2(张伟),5(张小芳),7(张云),12(张无忌),15(张翠山),19(张飞) */

        // 示例4：IN查询+原生条件（保持原样）
        List<Student> students4 = query2Lst(Student.class, builder -> {
            builder.in("id", 1, 3, 5)
                    .raw("(name IS NOT NULL OR age > 10)");
        });
        System.out.println("复合条件查询结果：" + students4);
        /* 预期结果（3条）：
        id=1,3,5（都满足IN条件且name非空）*/

        // 示例5：基础分页查询（保持原样）
        List<Student> students5 = query2Lst(Student.class, builder -> {
            builder.gt("age", 18)
                    .page("id", 1, 20);
        });
        System.out.println("分页结果：" + students5);
        /* 预期结果（按id排序的18条数据）：
        从id=2开始的所有符合age>18的记录 */

        // 示例6：复杂分页查询（假设create_time字段存在）
        List<Student> students6 = query2Lst(Student.class, builder -> {
            builder.between("age", 20, 30)
                    .like("name", "%张%")
                    .page("id", 100, 10);
        });
        System.out.println("复杂分页结果：" + students6);
        /* 预期结果（空列表）：
        因最大id=20，游标id=100无后续数据 */

        // 示例7：不等值查询
        List<Student> students7 = query2Lst(Student.class, builder -> {
            builder.ne("name", "张三");
        });
        System.out.println("非张三学生：" + students7);
        /* 预期结果（19条）：
        排除id=4(张三)，包含id=11(name=null) */

        // 示例8：组合范围查询（>= 和 <=）
        List<Student> students8 = query2Lst(Student.class, builder -> {
            builder.ge("age", 20)
                    .le("age", 25);
        });
        System.out.println("20-25岁学生：" + students8);
        /* 预期结果（10条）：
        id=2,3,8,9,10,11,12,15,19,20 */

        // 示例9：空值检查
        List<Student> students9 = query2Lst(Student.class, builder -> {
            builder.isNull("name");
        });
        System.out.println("姓名为空的学生：" + students9);
        /* 预期结果（1条）：
        id=11(name=null) */

        // 示例10：混合条件+分页
        List<Student> students10 = query2Lst(Student.class, builder -> {
            builder.eq("age", 20)
                    .like("name", "王%")
                    .page("id", 100, 5);
        });
        System.out.println("20岁王姓分页结果：" + students10);
        /* 预期结果（1条）：
        id=9(王思聪)，因id=19(张飞)不姓王 */

        // 示例11：原生OR条件
        List<Student> students11 = query2Lst(Student.class, builder -> {
            builder.raw("age > 30 OR name LIKE '李%'");
        });
        System.out.println("高级条件查询：" + students11);
        /* 预期结果（5条）：
        id=3(李娜),8(李大鹏),10(李小龙),14(李莫愁),18(诸葛亮-age=31) */

        // 示例12：全特性组合
        List<Student> students12 = query2Lst(Student.class, builder -> {
            builder.between("age", 18, 25)
                    .ne("name", "未知")
                    .isNotNull("id")
                    .page("age", 20, 15);
        });
        System.out.println("全条件分页结果：" + students12);
        /* 预期结果（分页数据）：
        排除id=16(name=未知)，按age排序后取age>20的前15条 */
    }
}
