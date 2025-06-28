package com.doth.selector.coordinator.core;

import com.doth.selector.common.poolsupports.PoolFactory;
import com.doth.selector.coordinator.ResultSetMapper;
import com.doth.selector.convertor.ConvertorType;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * @author 贤
 * @version 1.0
 * @date 2025/4/5 19:54
 * @description 总协调管理执行
 */
@Slf4j
public abstract class ExecuteCoordinator implements ExecuteCoordinatorService {

    // 组合组件
    protected ResultSetMapper rsMapper;
    // private final EntityAdapter condProcessor;


    public ExecuteCoordinator() {
        this.rsMapper = new ResultSetMapper(ConvertorType.LIGHT);
    }

    // 执行并映射, 同事使用
    protected final <T> List<T> executeQuery(Class<T> beanClass, String sql, Object[] params) {
        log.info(
                "\n\u001B[34m┌─────── [SQL] ───────\u001B[0m\n{}\n"
                        + "\u001B[90m├────────────────────\u001B[0m\n"
                        + "\u001B[33m│ condition param: {}\u001B[0m\n"
                        + "\u001B[90m└────────────────────\u001B[0m",
                sql,
                params == null ? "[]" : Arrays.toString(params)
        );
        try (ResultSet rs = PoolFactory.getPool().executeQuery(sql, params)) {
            // long start = System.currentTimeMillis();

            List<T> map = rsMapper.map(rs, beanClass);
            // long end = System.currentTimeMillis();
            // System.out.println("嵌套结构映射耗时 = " + (end - start));
            return map;
        } catch (SQLException e) {
            log.error("对 '{}' 转换时出现异常! 参数: {}", beanClass, params);
            throw new RuntimeException("数据库错误", e);
        }
    }
}
