package com.doth.selector.coordinator.core;

import com.doth.selector.coordinator.mapper.ResultSetMapper;
import com.doth.selector.coordinator.convertor.ConvertorType;
import com.doth.selector.common.util.DruidUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * @author 贤
 * @version 1.0
 * @date 2025/4/5 19:54
 * @description 总协调管理执行
 */
public abstract class ExecuteCoordinator implements ExecuteCoordinatorService {

    // 组合组件
    protected ResultSetMapper rsMapper;
    // private final EntityAdapter condProcessor;


    public ExecuteCoordinator() {
        this.rsMapper = new ResultSetMapper(ConvertorType.LIGHT);
    }

    // 执行并映射, 同事使用
    protected final <T> List<T> executeQuery(Class<T> beanClass, String sql, Object[] params) {
        System.out.println("最终查询生成的sql: " + sql);
        try (ResultSet rs = DruidUtil.executeQuery(sql, params)) {
            long start = System.currentTimeMillis();

            List<T> map = rsMapper.map(rs, beanClass);
            long end = System.currentTimeMillis();
            System.out.println("嵌套结构映射耗时 = " + (end - start));
            return map;
        } catch (SQLException e) {
            throw new RuntimeException("数据库错误", e);
        }
    }


    // 因使该类职责不清晰移除, 已不再使用
    @Deprecated(since = "1.0", forRemoval = true)
    final public <T> T getSingleResult(List<T> list) {
        return rsMapper.getSingleResult(list);
    }
}
