package com.doth.selector.supports.testbean.join;

import com.doth.selector.anno.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Employee {

    @Id
    private Integer id;

    private String name;

    private Integer age;

    @Join(fk = "d_id", refFK = "id")
    private Department department;



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


        现在解决完了, 考虑新的方向:
            1.考虑写插件 为当前 dto 自动编译生成, 而不需要为整个项目都全部重新生成一遍
            2.将生成的 dto 持久化到目录当中
            3.AutoQueryGenerator 应当支持从表主键的替换, 或是JoinBeanConvertor可以支持从表主键的映射 (现在仅支持主表外键的映射, 理由: 双方实际上一致, 可能导致冲突)
            4.优化 AutoQueryGenerator 的设计问题
            5.优化 DTOConstructor 的职责问题 ****
            6.优化 JoinBeanConvertor 的职责问题

            一点小小的 想法:
                现在的 代码社区都是 gitee, github..
                    它们本质上是做版本控制的, 却还提供了社区模块
                        但这也导致大多数人将其作用与版本控制的工具, 没有太多人去关注别人写的代码, 只有当这个人出名的时候, 才会有人去看
                        引起这一问题的主要还是查看并理解的成本太高, 这也是代码社区的通病
                            如果我能找到解决这个通病的办法, 那么就能突破这一瓶颈, 比如说: "娱乐化"
                            区别于一般的代码社区, 我的平台不包含"版本控制", 只有"最终的版本"
                                我还需要提供类似与方便开发者操作的"剪辑视频控制", 理想在"最终版本下录制视频"实现自由缩放, 光标的跟踪, 特效, 字幕的添加等等
                                这样的话似乎会引起一波新的文化啊...
     */

    /*
        我想到一个方法:
            被声明的 构造包含的字段 在生成的时候赋值为特殊默认值
            这样就可以通过是否等于空, 获取查询列列表

            !!方案取消!!
     */
    @DTOConstructor(id = "empSimple")
    public Employee(Integer id, String name, Integer age) {}

    @DTOConstructor(id = "baseEmpInfo")
    public Employee(
                @MainLevel
                    Integer id,
                    String name,
                @JoinLevel(clz = Department.class, attrName = "department")
                    Integer d_id,
                    String d_name,
                @Next(clz = Company.class, attrName = "company")
                    String company_name
    ) {}

    @DTOConstructor(id = "baseEmpDep")
    public Employee(
                @MainLevel
                    Integer id, String name,
                @JoinLevel(clz = Department.class)
                    String _name
    ) {}

    /*
        现在的需求是赶紧把第一版的写出来然后, 发布到maven 上面, 完成优化的部分, 这真的会很难
     */


    /*
        构造方法构造查询列列表 方案:
            难点:
                1.动态层级
                2.重名问题
                3.冗余注解问题
                    一个注解代表一个层级下的字段, 弊端是顺序难以控制, 好处是层级清晰, 方便阅读
                    现在是 通过构造方法中的参数进行组装映射, 完全不关 DTO 的事, 现在的 DTO 完全就是 让返回的数据做一个命名
            预想:
                public Employee(Integer id, String name, Integer age,
                    @JoinLevel(childTable = Department.class)
                        Integer depId, String depName // -> dep前缀自动匹配 department.name, 如果撇皮失败则全前缀departmentName
                    @JoinLevel(childTable = Company.class)
                        @ColAlias(name = "companyName") String comName
                ) {}




            预想实体中声明的构造参数:
                @DTOConstructor(id = "baseEmpInfo")
                public Employee(
                        @MainLevel // 可以省略
                            Integer id, String name, Integer age,

                            @JoinLevel(clz = Department.class) // -> 对应 t1
                                Integer department_depId, String dep_depName, // -> 对应 this.department.name/id, 或者只输入 dep 自动对应 department; department_代表处于哪一个实体下, 后面才是字段名称
                            @JoinLevel(childTable = Company.class)
                                String com_name
                ) {}

            预想对应生成的DTO类:
                @DependOn(clzPath = "com.doth.selector.join.testbean.common.Employee employee")
                public class Employee$baseEmpInfoDTO >> 原来的DTO类命名
                public class BaseEmpInfo >> 更改后
                {
                    List<String> selectList = new ArrayList<>();

                    private com.doth.selector.join.testbean.common.Employee employee

                    public BaseEmpInfo() {}
                    public BaseEmpInfo(com.doth.selector.join.testbean.common.Employee employee) {
                        this.id = employee.getId();
                        this.name = employee.getName();
                        this.age = employee.getAge();
                        this.depId = employee.getDepartment().getDepId();
                        this.depName = employee.getDepartment().getDepName();
                        this.comName = employee.getDepartment().getCompany().getName();
                    }
                    // 主表信息
                    private Integer id,
                    private String name,
                    private Integer age;

                    // department 的信息
                    private Integer depId;
                    private Integer depName;

                    // company 的信息
                    // private String name; >> 错误! 当生成时发现字段名重复时, 替换为从表的 simpleName + fieldName
                    private String comName; >> 正确! 默认使用从表的 simpleName 的 substring(0, 3) 作为最终字段名, 或者使用 @PfxAlias(name = "company") -> Pfx = Prefix, 最终: companyName, 反正最终字段名一定包含name, 只能自定义前缀, 算是该框架的规则



                    static {
                        DTOFactory.register(com.doth.selector.join.testbean.common.Employee.class, "baseEmpInfo", BaseEmpInfo.class 记得同类名.class);
                        // 最后再将实际字段注册进一个 新的 [查询列列表] 工厂中, 你可以参考 DTOFactory 的做法进行创建
                        selectList.add("t0.id"); // 按构造顺序一一赋值, tN 的生成规则可以参考 ColumnPathResolver
                        selectList.add("t0.name");
                        selectList.add("t0.age");
                        selectList.add("t1.depId");
                        selectList.add("t1.depName");
                        selectList.add("t2.name"); // !!这里不是 t2.comName!! 字段写 comName 是为了防止重名
                        DTOSelectFieldsListFactory.register("baseEmpInfo", selectList);
                        !![这样的话, AutoQueryGenerator 中的 extractDtoFieldNames 方法需要修改, 返回的是查询列列表, 返回前做一层循环依赖判断]!!
                    }
                    // getter, setter, equal, toString... 我来写
                }

    实体信息:
                @Entity
                public class Employee {

                    @Id
                    private Integer id;

                    private String name;

                    private Integer age;

                    @Join(fk = "d_id", refFK = "depId")
                    private Department department;
                    ...
                }
                @Entity
                public class Department {

                    @Id
                    private Integer depId;

                    private String depName; // 框架还内置了检查, 强制要求实体类的字段都必须使用 包装类

                    @Join(fk = "com_id", refFK = "id")
                    private Company company;
                    ...
                }
                @Entity
                public class Company {

                    @Id
                    private Integer id;

                    private String name;
                    ...
                }


                现在的想法, 先优化至最佳, 然后再理解

     */

}
