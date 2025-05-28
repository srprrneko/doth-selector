package com.doth.selector.common.testbean.join;

import com.doth.selector.anno.DTOConstructor;
import com.doth.selector.anno.Entity;
import com.doth.selector.anno.Id;
import com.doth.selector.anno.Join;

/**
 * @project: test02
 * @package: com.doth.stupidrefframe.testbean
 * @author: doth
 * @creTime: 2025-03-27  11:35
 * @desc:
 * n+1
 *  select from emp...
 *      -> d_id
 *      select from dept where d_id = ?
 * @v: 1.0
 */
@Entity
public class Employee {

    @Id
    private Integer id;

    private String name;

    private Integer age;

    @Join(fk = "d_id", refFK = "id")
    private Department department;


    public Employee() {
    }

    /*
        DTO新疑问: 当前的构造方法是这样的话, 我们就难以控制同样参数的构造的层级控制, 这后续可以通过 懒加载实现, 但使用的依然是实体, 似乎我的 DTO 生成方案有些难以解决这个问题

        1.如果用户只要从表的 第一个 属性, 通常我们设计DTO的时候是 直接让它暴露 在外层
            可是我现在的做法是 必须让
        2.开发者创建的 DTO, 加入开发者 对其进行更新 最终一运行就把他们的 更改全部给弄没了, 开发者只能在 构造里 修改DTO (后期: [是否同步更改? Alt+Enter])
            目前的做法, 确实有一些是不太妥当的, 因为最终目标是控制自动sql的生成, 让 sql 的生成参考 DTO, 也就是说, 完全完全不依赖构造方法的方法体,
              方式一: 开发者仅仅需要 写一个构造方法签名就可以了, 优点是可以解决外部直接暴露外键的问题, 通过给参数定义一个 注解..
              方式二: 但是要知道, 现在的手段是通过子类直接去访问 父类的构造方法, 仅仅需要super(...) 也就是说, 开发者可以在父类构造方法体中控制赋值操作
                   1.外键字段

               无论如何, 都解决不了,
                1.最终返回的 字段的就算子类没有声明, 父类包含了, 那么这个字段就会为空, 也就是说,
                这个DTO自动生成的方式, 实际上只是方便用于控制 [懒加载], 所以不应该叫做DTO, 而是叫做[懒加载模型LazyModel=LM]

     */
    public Employee(Integer id, String name, Integer age, Department department) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.department = department;
    }

    @DTOConstructor(id = "empDeptVzId")
    public Employee(Integer id, String name, Integer age, Department department, Integer d_id) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.department = department;
        this.getDepartment().setId(d_id);
    }

    @DTOConstructor(id = "empWithDepId")
    public Employee(Integer id, String name, Integer age, Integer dId) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.getDepartment().setId(dId);
    }

    @DTOConstructor(id = "empSimple")
    public Employee(Integer id, String name, Integer age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    /**
     * 获取
     * @return id
     */
    public Integer getId() {
        return id;
    }

    /**
     * 设置
     * @param id
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * 获取
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * 设置
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取
     * @return age
     */
    public Integer getAge() {
        return age;
    }

    /**
     * 设置
     * @param age
     */
    public void setAge(Integer age) {
        this.age = age;
    }

    /**
     * 获取
     * @return department
     */
    public Department getDepartment() {
        return department;
    }

    /**
     * 设置
     * @param department
     */
    public void setDepartment(Department department) {
        this.department = department;
    }

    public String toString() {
        return "Employee{id = " + id + ", name = " + name + ", age = " + age + ", department = " + department + "}";
    }
}
