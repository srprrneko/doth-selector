package com.doth.selector.coordinator.supports.sqlgenerator.tool.temp;

import com.doth.selector.anno.Id;
import com.doth.selector.anno.Join;
import com.doth.selector.anno.OneToOne;
import com.doth.selector.common.exception.NonPrimaryKeyException;
// import com.doth.selector.common.testbean.join.BaseEmpDep;
// import com.doth.selector.common.testbean.join.BaseEmpInfo;
import com.doth.selector.common.testbean.join.Employee;
import com.doth.selector.common.testbean.join3.User;
import com.doth.selector.common.util.AnnoNamingConvertUtil;
import com.doth.selector.common.util.NamingConvertUtil;
import com.doth.selector.executor.supports.builder.ConditionBuilder;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 协调类：仅负责调度策略初始化与解析流程，不包含业务逻辑
 */
public class AutoQueryGenerator {
    private static final String MAIN_ALIAS = "t0";
    public static final int MAX_JOIN_LENGTH = 10;

    private final QueryContext context = new QueryContext();
    private final QueryInitializationStrategy strategy;


    private AutoQueryGenerator(Class<?> entityClass, ConditionBuilder<?> conditionBuilder) {
        this.strategy = QueryStrategyFactory.getStrategy(entityClass);
        this.strategy.initialize(context, entityClass, conditionBuilder);
    }

    public static String generated(Class<?> entityClass) {
        return generated(entityClass, null);
    }

    public static String generated(Class<?> entityClass, ConditionBuilder<?> conditionBuilder) {
        return new AutoQueryGenerator(entityClass, conditionBuilder).generate();
    }

    private String generate() {
        parseEntity(context.getOriginalEntity(), MAIN_ALIAS, new HashSet<>());
        // 初始化 SQL 构建器
        StringBuilder sql = new StringBuilder();

        // 拼接 SELECT 列
        sql.append("select ");
        List<String> selectList = context.getSelectList();
        for (int i = 0; i < selectList.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(selectList.get(i));
        }

        // 拼接 FROM 主表
        sql.append("\nfrom ")
                .append(getTableName(context.getOriginalEntity()))
                .append(" ")
                .append(MAIN_ALIAS);

        // 拼接 JOIN 子句
        for (String joinClause : context.getJoinClauses()) {
            sql.append("\n").append(joinClause);
        }

        return sql.toString();
    }

    private void parseEntity(Class<?> entity, String alias, Set<Field> ancestorJoins) {
        if (context.getJoinLevel() > MAX_JOIN_LENGTH) {
            throw new RuntimeException("关联层级过深: " + entity.getSimpleName());
        }
        if (!context.getProcessedEntities().add(entity)) {
            boolean canBreak = ancestorJoins.stream().anyMatch(f -> f.isAnnotationPresent(OneToOne.class));
            if (!canBreak) {
                throw new RuntimeException("检测到未标注 @OneToOne 的循环引用: " + entity.getSimpleName());
            }
            return;
        }
        for (Field field : entity.getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Join.class)) {
                handleJoin(field, alias, ancestorJoins);
            } else if (!context.isDtoMode()) {
                context.getSelectList().add(alias + "." + NamingConvertUtil.camel2SnakeCase(field.getName()));
            }
        }
        context.getProcessedEntities().remove(entity);
    }

    private void handleJoin(Field field, String alias, Set<Field> ancestorJoins) {
        String nextAlias = "t" + context.getJoinLevel();
        if (!context.isDtoMode()
                || context.getDtoPrefixes().contains(nextAlias)
                || context.getConditionPrefixes().contains(nextAlias)) {

            context.setJoinLevel(context.getJoinLevel() + 1);
            Set<Field> newAncestors = new HashSet<>(ancestorJoins);
            newAncestors.add(field);

            Join join = field.getAnnotation(Join.class);

            if (!context.isDtoMode()) {
                assert join != null;
                // context.getSelectList().add(alias + "." + NamingConvertUtil.camel2SnakeCase(join.fk()));
            }

            Class<?> target = field.getType();
            Field pk = Arrays.stream(target.getDeclaredFields())
                    .filter(f -> f.isAnnotationPresent(Id.class))
                    .findFirst()
                    .orElseThrow(() -> new NonPrimaryKeyException(target.getSimpleName() + " 缺少 @Id 注解"));
            context.getJoinClauses()
                    .add(String.format("join %s %s ON %s.%s = %s.%s",
                        getTableName(target), nextAlias,
                        alias, join.fk(),
                        nextAlias, pk.getName())
                    );
            parseEntity(target, nextAlias, newAncestors);
        }
    }

    private String getTableName(Class<?> entity) {
        return AnnoNamingConvertUtil.camel2Snake(entity, entity.getSimpleName());
    }

    public static void main(String[] args) {
        // long start = System.currentTimeMillis();
        // String generated = generated(BaseEmpDep.class);
        // long end = System.currentTimeMillis();
        // System.out.println("generated = " + generated);
        // System.out.println("generated = \n" + (end - start));
    }
}
