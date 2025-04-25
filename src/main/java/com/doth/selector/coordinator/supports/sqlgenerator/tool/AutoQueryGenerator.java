package com.doth.selector.coordinator.supports.sqlgenerator.tool;

import com.doth.selector.anno.Id;
import com.doth.selector.anno.Join;
import com.doth.selector.anno.OneToOne;
import com.doth.selector.exception.NonPrimaryKeyException;
import com.doth.selector.testbean.join3.User;
import com.doth.selector.util.AnnoNamingConvertUtil;
import com.doth.selector.util.CamelSnakeConvertUtil;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class AutoQueryGenerator {
    private static final String MAIN_ALIAS = "t0"; // 默认主表别名
    private static int joinLevel = 1;  // join 层数, 主表固定为0, 从表从1开始


    public static String generated(Class<?> mainEntity) {
        List<String> selectList = new ArrayList<>();
        List<String> joinClauses = new ArrayList<>();

        parseEntity(mainEntity, MAIN_ALIAS, selectList, joinClauses,
                0, mainEntity, null, null);

        return new StringBuilder()
                .append("select ").append(String.join(", ", selectList)).append("\n")
                .append("from ").append(getTableName(mainEntity)).append(" ").append(MAIN_ALIAS).append("\n")
                .append(String.join("\n", joinClauses))
                .toString();
    }

    // 递归扫描实体类的所有字段，将字段分为正常字段以及外键字段(带@JoinColumn注解的)进行处理
    // 新增mainEntity参数以识别是否指向原始主表
    private static void parseEntity(Class<?> entity, String currentAlias,
                                    List<String> selectList, List<String> joinClauses,
                                    int depth, Class<?> mainEntity,
                                    Set<Field> joinField, Set<Class<?>> processedEntities) {
        // 初始化调用栈
        if (processedEntities == null) {
            processedEntities = new HashSet<>();
        }

        // 严格检测循环引用
        if (processedEntities.contains(entity)) {
            if (entity.equals(mainEntity) && depth > 0) { // 回到主表且不是第一次访问
                // 只有标注了@OneToOne才允许
                boolean hasOneToOne = false;
                for (Field f : entity.getDeclaredFields()) {
                    if (f.getType() == mainEntity && f.isAnnotationPresent(OneToOne.class)) {
                        hasOneToOne = true;
                        break;
                    }
                }
                if (!hasOneToOne) {
                    throw new RuntimeException("检测到未标注 @OneTo1的相互持有关系: " +
                            entity.getSimpleName() + " ←→ " + mainEntity.getSimpleName() +
                            "\n请使用@OneTo1标注或使用 DirectQueryExecutor DTO解决循环引用: 详见todo");
                }
                return;
            }
            throw new RuntimeException("检测到循环引用: " + entity.getSimpleName() +
                    " 已经在调用栈中出现");
        }

        processedEntities.add(entity);

        for (Field field : entity.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Join.class)) {
                // 严格检查相互持有关系
                if (field.getType() == mainEntity && depth > 0) {
                    if (!field.isAnnotationPresent(OneToOne.class)) {
                        throw new RuntimeException("检测到未标注@OneToOne的相互持有关系: " +
                                entity.getSimpleName() + "." + field.getName() +
                                " ↔ " + mainEntity.getSimpleName() +
                                "\n请使用@OneToOne标注或使用DTO解决循环引用");
                    }
                    continue;
                }

                Set<Field> newJoinField = joinField == null ? new HashSet<>() : new HashSet<>(joinField);
                if (newJoinField.add(field)) {
                    handleJoinColumn(field, currentAlias, selectList, joinClauses,
                            depth, mainEntity, newJoinField, processedEntities);
                }
            } else {
                handleNormalField(field, currentAlias, selectList, depth);
            }
        }

        processedEntities.remove(entity);
    }

    /**
     * 解析嵌套字段逻辑, 添加
     * @param field 当前字段
     * @param currentAlias 当前字段的别名
     * @param selectList 查询字段列表
     * @param joinClauses JOIN子句列表
     * @param joinFloors join层数
     * @param mainEntity 原始主表类型，用于识别循环引用
     */
    private static void handleJoinColumn(Field field, String currentAlias,
                                         List<String> selectList, List<String> joinClauses,
                                         int joinFloors, Class<?> mainEntity,
                                         Set<Field> joinField, Set<Class<?>> processedEntities) {
        Join jc = field.getAnnotation(Join.class);

        // 添加外键列到SELECT列表
        String fkColumn = currentAlias + "." + jc.fk();
        selectList.add(fkColumn);

        Class<?> refEntity = field.getType();

        // 生成JOIN子句
        String nextAlias = "t" + joinLevel++;
        String refTable = getTableName(refEntity);
        String refColumn = getPKField(refEntity).getName();

        joinClauses.add(
                String.format("join %s %s ON %s.%s = %s.%s",
                        refTable, nextAlias,
                        currentAlias, jc.fk(),
                        nextAlias, refColumn)
        );

        // 递归解析关联实体
        parseEntity(refEntity, nextAlias, selectList, joinClauses,
                joinFloors + 1, mainEntity, joinField, processedEntities);
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
        System.out.println(generated(User.class));
    }
}