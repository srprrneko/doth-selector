package com.doth.selector.coordinator.supports.sqlgenerator.tool;

import com.doth.selector.anno.Id;
import com.doth.selector.anno.Join;
import com.doth.selector.exception.NonPrimaryKeyException;
import com.doth.selector.testbean.join.Employee;
import com.doth.selector.util.AnnoNamingConvertUtil;
import com.doth.selector.util.CamelSnakeConvertUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class DynamicQueryGenerator {

    /*
        emp{ t0
            dep t1 {
                com t2
            }
            office t3
        }



     */
    private static final String MAIN_ALIAS = "t0"; // 默认主表别名
    private static int joinLevel = 1;  // join 层数, 主表固定为0, 从表从1开始

    // 入口方法, 传递主表实体, 返回完整sql
    public static String generated(Class<?> mainEntity) {
        List<String> selectList = new ArrayList<>(); // 所有查询字段, 例如: "t0.id, t0.name, t1.id, t1.name"
        List<String> joinClauses = new ArrayList<>(); // 存储所有join子句, 例如: "JOIN department t1 ON t0.d_id = t1.id"

        // 递归解析实体关系
        parseEntity(mainEntity, MAIN_ALIAS, selectList, joinClauses, 0);

        // 构建完整SQL
        return new StringBuilder()
                .append("select ").append(String.join(", ", selectList)).append("\n")
                .append("from ").append(getTableName(mainEntity)).append(" ").append(MAIN_ALIAS).append("\n")
                .append(String.join("\n", joinClauses)) // 以换行作为分割, 加入join子句
                .toString();
    }

    // 递归扫描实体类的所有字段，将字段分为正常字段以及外键字段(带@JoinColumn注解的)进行处理
    // 关联两个方法, 普通处理的逻辑和关联字段的处理逻辑,
    private static void parseEntity(Class<?> entity, String currentAlias,
                             List<String> selectList, List<String> joinClauses, int depth) {
        for (Field field : entity.getDeclaredFields()) { // 遍历传入实体信息的所有字段
            if (field.isAnnotationPresent(Join.class)) { // 如果带有@JoinColumn注解的字段
                handleJoinColumn(field, currentAlias, selectList, joinClauses, depth); // 处理关联字段：生成 JOIN 语句，并递归解析关联实体
            } else {
                handleNormalField(field, currentAlias, selectList, depth); // 普通处理
            }
        }
    }
    
    /** 
     * 解析嵌套字段逻辑, 添加
     * @param field 当前字段
     * @param currentAlias 当前字段的别名
     * @param selectList 查询字段列表
     * @param joinClauses JOIN子句列表
     * @param joinFloors join层数
     */
    private static void handleJoinColumn(Field field, String currentAlias,
                                  List<String> selectList, List<String> joinClauses, int joinFloors) {
        Join jc = field.getAnnotation(Join.class);

        // 添加外键列
        String fkColumn = currentAlias + "." + jc.fk(); // 直接表别名 + joinColumn 的fk属性
        selectList.add(fkColumn);

        // 生成JOIN语句
        String nextAlias = "t" + joinLevel++; // t1, t2, ...
        String refTable = getTableName(field.getType()); // 此时接收的字段一定是关联实体, 所以一定是自定义对象, 直接.getType 就可以获取
        String refColumn = getPKField(field.getType()).getName(); // 进入该表里面寻找引用字段

        // 存储join子句
        joinClauses.add(String.format("join %s %s ON %s.%s = %s.%s",
                refTable, nextAlias,
                currentAlias, jc.fk(),
                nextAlias, refColumn)
        );

        // 递归解析关联实体（过滤主键字段）
        parseEntity(field.getType(), nextAlias, selectList, joinClauses, joinFloors + 1);
    }

    private static Field getPKField(Class<?> entityClass) {
        // 尝试快速定位（假设主键是第一个字段）
        Field[] fields = entityClass.getDeclaredFields();
        if (fields.length > 0 && fields[0].isAnnotationPresent(Id.class)) {
            return fields[0];
        }

        // 第二步：遍历所有字段
        for (Field field : fields) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        throw new NonPrimaryKeyException(entityClass.getSimpleName() + " 未找到主键字段, 或请对主键标记注解: " + Id.class.getName());
    }

    /**
     * 处理普通字段（非关联字段）
     *  1.强字段转换为"表别名.列名"格式, 加入 SelectLIst
     *  2.若当前是关联表 (joinFloors > 0), 且字段标记为@Id主键, -> 跳过
     *  示例：Employee.name → t0.name
     *
     * @param field　当前字段
     * @param alias　当前字段的别名
     * @param selectList　存储所有查询字段的列表
     * @param joinFloors　join 层的深度
     */
    private static void handleNormalField(Field field, String alias,
                                   List<String> selectList, int joinFloors) {
        boolean isPrimaryKey = field.isAnnotationPresent(Id.class); // 判断当前字段是否为主键

        // 关联表的主键不加入SELECT（避免冗余）
        if (joinFloors > 0 && isPrimaryKey) return;

        // 规范转换（例如：hireDate → hire_date）
        String columnName = CamelSnakeConvertUtil.camel2SnakeCase(field.getName());

        // 添加规范化后的字段（如 name -> t0.name）
        selectList.add(alias + "." + columnName);
    }

    // 实体转换表名
    private static String getTableName(Class<?> entity) {
        return AnnoNamingConvertUtil.camel2Snake(entity, entity.getSimpleName());
    }




    public static void main(String[] args) {
        DynamicQueryGenerator builder = new DynamicQueryGenerator();
        String s = builder.generated(Employee.class);
        System.out.println("s = " + s);
    }
}