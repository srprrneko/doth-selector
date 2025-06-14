package com.doth.selector.common.poolsupports;


import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.Properties;

public class DruidPool implements ConnectionPool {

    private static final DruidDataSource dataSource;

    static {
        dataSource = initDataSourceWithConfig();
    }

    // 初始化连接池
    private static DruidDataSource initDataSourceWithConfig() {
        try (InputStream input = DruidPool.class.getClassLoader().getResourceAsStream("druid.properties")) {
            Properties props = new Properties();
            props.load(input);
            DruidDataSource ds = (DruidDataSource) DruidDataSourceFactory.createDataSource(props);
            ds.setUseUnfairLock(true);  // 高性能模式
            ds.setAsyncInit(true);      // 异步初始化
            return ds;
        } catch (Exception e) {
            throw new RuntimeException("初始化Druid连接池失败", e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public ResultSet executeQuery(String sql, Object[] params) throws SQLException {
        Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);

        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
        }

        ResultSet rs = pstmt.executeQuery();
        return wrapResultSet(rs, pstmt, conn);
    }

    @Override
    public int executeUpdate(String sql, Object[] params) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    pstmt.setObject(i + 1, params[i]);
                }
            }
            return pstmt.executeUpdate();
        }
    }

    @Override
    public void printPoolStatus() {
        System.out.println("Druid 活跃连接数: " + dataSource.getActiveCount());
        System.out.println("Druid 空闲连接数: " + dataSource.getPoolingCount());
        System.out.println("Druid 总创建数: " + dataSource.getCreateCount());
    }

    private ResultSet wrapResultSet(ResultSet rs, Statement stmt, Connection conn) {
        return (ResultSet) Proxy.newProxyInstance(
                DruidPool.class.getClassLoader(),
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
                    conn.close(); // 实际归还连接
                }
            }
        }
    }

    public static void main(String[] args) throws SQLException {
        System.out.println("DruidPool.getConnection() = " + new DruidPool().getConnection());
    }
}
