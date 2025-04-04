package com.doth.stupidrefframe_v1.selector.v1.util;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.Properties;

public class DruidUtil {
    private static DruidDataSource dataSource;

    static {
        initDataSourceWithConfig();
    }

    // 配置文件初始化方式
    private static void initDataSourceWithConfig() {
        try (InputStream input = DruidUtil.class.getClassLoader()
                .getResourceAsStream("druid.properties")) {

            Properties props = new Properties();
            props.load(input);

            // 自动识别Druid标准配置项
            dataSource = (DruidDataSource) DruidDataSourceFactory.createDataSource(props);

            // 手动设置特殊参数
            dataSource.setUseUnfairLock(true);  // 高性能模式
            dataSource.setAsyncInit(true);      // 异步初始化

        } catch (Exception e) {
            throw new RuntimeException("初始化Druid连接池失败", e);
        }
    }

    // 监控方法
    public static void printPoolStatus() {
        System.out.println("活跃连接数: " + dataSource.getActiveCount());
        System.out.println("空闲连接数: " + dataSource.getPoolingCount());
        System.out.println("总创建数: " + dataSource.getCreateCount());
    }

    public static ResultSet executeQuery(String sql, Object[] params) {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement(sql);
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    pstmt.setObject(i + 1, params[i]);
                }
            }
            ResultSet rs = pstmt.executeQuery();
            return wrapResultSet(rs, pstmt, conn);
        } catch (SQLException e) {
            closeResourcesQuietly(pstmt, conn);
            throw new RuntimeException("SQL执行失败: " + e.getMessage(), e);
        }
    }

    private static ResultSet wrapResultSet(ResultSet rs, Statement stmt, Connection conn) {
        return (ResultSet) Proxy.newProxyInstance(
                DruidUtil.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                new ResultSetInvocationHandler(rs, stmt, conn)
        );
    }

    private static class ResultSetInvocationHandler implements InvocationHandler {
        private final ResultSet rs;
        private final Statement stmt;
        private final Connection conn;

        ResultSetInvocationHandler(ResultSet rs, Statement stmt, Connection conn) {
            this.rs = rs;
            this.stmt = stmt;
            this.conn = conn;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("close".equals(method.getName())) {
                closeResources();
                return null;
            }
            try {
                return method.invoke(rs, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

        private void closeResources() throws SQLException {
            try {
                rs.close();
            } finally {
                try {
                    stmt.close();
                } finally {
                    conn.close(); // 实际是归还到连接池
                }
            }
        }
    }

    private static void closeResourcesQuietly(Statement stmt, Connection conn) {
        try {
            if (stmt != null) stmt.close();
        } catch (SQLException e) {
            // todo : log warning
        }
        try {
            if (conn != null) conn.close();
        } catch (SQLException e) {
            // todo : log warning
        }
    }

    // 其他数据库操作方法（根据需要补充）
    public static int executeUpdate(String sql, Object[] params) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    pstmt.setObject(i + 1, params[i]);
                }
            }
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws SQLException {
        System.out.println("DruidUtil.dataSource.getConnection() = " + DruidUtil.dataSource.getConnection());
    }
}