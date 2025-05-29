package com.doth.selector.coordinator.supports.sqlgenerator.tool;

import com.doth.selector.anno.Id;
import com.doth.selector.anno.Join;
import com.doth.selector.anno.OneToOne;
import com.doth.selector.anno.DTOConstructor;
import com.doth.selector.common.exception.NonPrimaryKeyException;
import com.doth.selector.common.testbean.join3.User;
import com.doth.selector.common.util.AnnoNamingConvertUtil;
import com.doth.selector.common.util.CamelSnakeConvertUtil;
import com.doth.selector.dto.DtoStackResolver;
import com.doth.selector.executor.supports.builder.ConditionBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

public class AutoQueryGenerator {

    private static final String MAIN_ALIAS = "t0";
    public static final int MAX_JOIN_LENGTH = 5;

    private final Class<?> mainEntity;
    private final Set<String> dtoFieldSet;
    private final Set<String> conditionPrefixes;
    private final List<String> selectList = new ArrayList<>();
    private final List<String> joinClauses = new ArrayList<>();
    private final Set<Class<?>> processedEntities = new HashSet<>();
    private int joinLevel = 1;

    public static String generated(Class<?> mainEntity) {
        return new AutoQueryGenerator(mainEntity, null).generate();
    }

    public static String generated(Class<?> mainEntity, ConditionBuilder<?> conditionBuilder) {
        return new AutoQueryGenerator(mainEntity, conditionBuilder).generate();
    }

    /**
     * 初始化, 三运表达式 适应旧代码
     *  1.mainEntity 2.dtoFieldSet 3.conditionPrefixes
     * @param mainEntity 主实体
     * @param conditionBuilder cb
     */
    private AutoQueryGenerator(Class<?> mainEntity, ConditionBuilder<?> conditionBuilder) {
        this.mainEntity = mainEntity;
        String dtoId = DtoStackResolver.resolveDTOIdFromStack();
        this.dtoFieldSet = dtoId != null
                ? extractDtoFieldNames(mainEntity, dtoId)
                : null;
        this.conditionPrefixes = conditionBuilder != null
                ? conditionBuilder.extractJoinTablePrefixes()
                : Collections.emptySet();
    }

    /**
     * 搭建sql脚手架 通过 parseEntity 补充搭建变量
     * @return sql
     */
    private String generate() {
        parseEntity(mainEntity, MAIN_ALIAS, new HashSet<>());
        String select = "select " + String.join(", ", selectList);
        String from = "from " + getTableName(mainEntity) + " " + MAIN_ALIAS;
        return select + "\n" + from + "\n" + String.join("\n", joinClauses);
    }

    /**
     * 解析实体, 核心是 (1层级拦截; (2循环检测; (3[字段, join, dto模式] 分流处理
     * @param entity 实体
     * @param alias 别名
     * @param joinFields 关联字段集合
     */
    private void parseEntity(Class<?> entity, String alias, Set<Field> joinFields) {
        if (joinFields.size() > MAX_JOIN_LENGTH) {
            throw new RuntimeException("关联层级过深，建议检查实体设计是否合理: " + entity.getSimpleName());
        }
        if (processedEntities.contains(entity)) {
            boolean breakable = joinFields.stream().anyMatch(f -> f.isAnnotationPresent(OneToOne.class));
            if (!breakable) {
                throw new RuntimeException("检测到未标注 @OneToOne 的循环引用: " + entity.getSimpleName());
            }
            return;
        }
        processedEntities.add(entity);

        // 开始分流
        for (Field field : entity.getDeclaredFields()) {
            field.setAccessible(true); // 很漂亮的一步, 下面全是分支, 结构清晰

            if (field.isAnnotationPresent(Join.class)) {
                String candidateAlias = "t" + joinLevel;
                if (dtoFieldSet != null && !conditionPrefixes.contains(candidateAlias)) {
                    continue;
                }
                Set<Field> newJoinFields = new HashSet<>(joinFields);
                newJoinFields.add(field);
                handleJoin(field, alias, newJoinFields);
            } else if (dtoFieldSet != null) {
                handleField4Dto(field, alias);
            } else {
                handleField(field, alias);
            }
        }
        processedEntities.remove(entity);
    }

    private void handleJoin(Field field, String alias, Set<Field> joinFields) {
        Join join = field.getAnnotation(Join.class);
        // Only include FK in select when not in DTO mode
        if (dtoFieldSet == null) {
            selectList.add(alias + "." + join.fk());
        }

        Class<?> refEntity = field.getType();
        String nextAlias = "t" + joinLevel++;
        Field pk = getPKField(refEntity);

        joinClauses.add(String.format(
                "join %s %s ON %s.%s = %s.%s",
                getTableName(refEntity), nextAlias,
                alias, join.fk(),
                nextAlias, pk.getName())
        );

        parseEntity(refEntity, nextAlias, joinFields);
    }

    private void handleField(Field field, String alias) {
        if (!MAIN_ALIAS.equals(alias) && field.isAnnotationPresent(Id.class)) {
            return;
        }
        String column = CamelSnakeConvertUtil.camel2SnakeCase(field.getName());
        selectList.add(alias + "." + column);
    }

    private void handleField4Dto(Field field, String alias) {
        // Only include main entity fields present in DTO
        if (!MAIN_ALIAS.equals(alias)) {
            return;
        }
        String column = CamelSnakeConvertUtil.camel2SnakeCase(field.getName());
        if (dtoFieldSet.contains(column)) {
            selectList.add(alias + "." + column);
        }
    }

    private Field getPKField(Class<?> entityClass) {
        for (Field f : entityClass.getDeclaredFields()) {
            if (f.isAnnotationPresent(Id.class)) {
                return f;
            }
        }
        throw new NonPrimaryKeyException(entityClass.getSimpleName() + " 未找到主键字段, 请使用@Id注解标记主键");
    }

    private String getTableName(Class<?> entity) {
        return AnnoNamingConvertUtil.camel2Snake(entity, entity.getSimpleName());
    }

    private static Set<String> extractDtoFieldNames(Class<?> entityClass, String dtoId) {
        Set<String> result = new HashSet<>();
        for (Constructor<?> ctor : entityClass.getDeclaredConstructors()) {
            if (ctor.isAnnotationPresent(DTOConstructor.class)
                    && ctor.getAnnotation(DTOConstructor.class).id().equals(dtoId)) {
                for (var param : ctor.getParameters()) {
                    result.add(param.getName());
                }
                break;
            }
        }
        return result;
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        String sql = generated(User.class);
        System.out.println("total = " + (System.currentTimeMillis() - start));
        System.out.println("sql =\n " + sql);
    }
}
