package com.doth.stupidrefframe_v1.trygetgenericity;


import com.doth.stupidrefframe_v1.testbean.Student;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 泛型工具类，用于动态获取子类指定的泛型类型并实例化对象。
 * 核心原理：通过子类继承泛型基类，在父类中利用反射获取子类声明的具体泛型类型。
 *
 * 设计背景：
 * - 模仿 JPA 框架中 Repository<T> 的设计，如 `JpaRepository<Entity, ID>` 的实现[6](@ref)
 * - 子类继承时会将泛型参数固化到类结构元数据中，父类通过解析该元数据实现零参数操作
 *
 * @param <T> 泛型参数，表示需要操作的目标类型（如数据库实体类）
 */
public class BeanUtil<T> {
    // 存储泛型类型对应的 Class 对象（如 Student.class）
    private Class<T> entityType;
    // 泛型对象实例（通过反射创建的 T 类型对象）
    private T entity;

    /**
     * 构造函数逻辑分步解析：
     * 1. 获取当前类的直接父类泛型信息
     * 2. 解析出子类继承时指定的具体泛型类型
     * 3. 通过反射实例化泛型对象
     *
     * 技术要点：
     * - 必须通过子类继承（如 StudentBeanUtil）或匿名内部类使用，否则无法获取泛型参数[6](@ref)
     * - 直接实例化 BeanUtil<Student> 会丢失泛型信息（类型擦除机制）[3](@ref)
     */
    public BeanUtil() {
        /*
         * 步骤 1：获取泛型父类类型
         * - getClass() 获取当前运行时类（实际是子类如 StudentBeanUtil）
         * - getGenericSuperclass() 返回带有泛型参数的父类类型（即 BeanUtil<Student>）
         */
        Type superClass = getClass().getGenericSuperclass();

        // 验证是否为参数化类型（即是否包含泛型参数）
        // 仅处理包含泛型的子类
        if (!(superClass instanceof ParameterizedType)) {
            throw new IllegalArgumentException("必须通过子类继承并指定泛型参数，示例：class StudentUtil extends BeanUtil<Student>");
        }

        // 步骤 2：解析泛型参数
        ParameterizedType pt = (ParameterizedType) superClass;
        Type[] typeArgs = pt.getActualTypeArguments(); // 获取泛型参数数组（如 [Student.class]）
        this.entityType = (Class<T>) typeArgs[0]; // 提取第一个泛型参数对应的 Class 对象

        /*
         * 步骤 3：实例化泛型对象
         * 要求：泛型类必须有无参构造函数（与 JPA 实体类规范一致）
         * 原理：通过反射调用 newInstance() 创建对象实例[1,3](@ref)
         */
        try {
            this.entity = entityType.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            // 处理常见异常：无构造方法/构造方法不可访问等
            throw new RuntimeException("实例化失败: " + entityType.getName() +
                    "，请确认该类存在无参构造函数且非抽象类", e);
        }
    }


    public T getEntity() {
        return entity;
    }
}

// 正确用例子类（模仿 JPA 的 Repository 继承方式）
class StudentBeanUtil extends BeanUtil<Student> {} // 通过继承固化泛型类型为 Student

class Main {

    public static void main(String[] args) throws NoSuchMethodException {
        //
        // Method method = Main.class.getMethod("print", String.class);
        // System.out.println("运行时注解是否存在: " + method.isAnnotationPresent(Overload.class));
        // 正确用法：通过子类继承获取 Student 实例
        System.out.println(new StudentBeanUtil().getEntity()); // 输出 Student 对象

        /*
         * 特殊用法：匿名内部类临时指定泛型类型
         * 原理：匿名内部类会生成继承 BeanUtil<Student> 的子类
         * 注意：此方式适用于测试，生产环境建议用显式子类[6](@ref)
         */
        System.out.println(new BeanUtil<Student>(){}.getEntity()); // 同样输出 Student 对象
    }
}