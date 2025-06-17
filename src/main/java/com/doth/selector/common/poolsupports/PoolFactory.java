package com.doth.selector.common.poolsupports;

import java.util.Objects;

/**
 * 简单工厂 + 策略选择：默认使用 HikariPool，可通过系统属性切换为 DruidPool
 */
public class PoolFactory {
    private static final ConnectionPool POOL;

    static {
        String poolType = System.getProperty("orm.pool", "hikari").toLowerCase(); // 默认 hikari
        switch (poolType) {
            case "druid":
                POOL = new DruidPool();
                break;
            case "hikari":
            default:
                POOL = new HikariPool();
        }
    }

    public static ConnectionPool getPool() {
        return POOL;
    }

    public static void printPoolStatus() {
        POOL.printPoolStatus();
    }

    // 允许在运行时显式切换（可选）
    public static void switchTo(ConnectionPool otherPool) {
        throw new UnsupportedOperationException("当前为静态初始化，动态切换不可用，如需动态切换请重构为持有池引用的实例上下文");
    }
}
