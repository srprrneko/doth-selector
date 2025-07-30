package com.doth.selector.common.poolsupports;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.Properties;

public class HikariPool implements ConnectionPool {

    private static final HikariDataSource dataSource;

    static {
        Properties props = new Properties();
        try (InputStream input = HikariPool.class.getClassLoader().getResourceAsStream("hikari.properties")) {
            props.load(input);
            HikariConfig config = new HikariConfig(props);
            dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            throw new RuntimeException("初始化Hikari连接池失败", e);
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

    private ResultSet wrapResultSet(ResultSet rs, Statement stmt, Connection conn) {
        return (ResultSet) Proxy.newProxyInstance(
                HikariPool.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("close".equals(method.getName())) {
                            try {
                                rs.close();
                            } finally {
                                try {
                                    stmt.close();
                                } finally {
                                    conn.close(); // 归还连接池
                                }
                            }
                            return null;
                        }
                        try {
                            return method.invoke(rs, args);
                        } catch (InvocationTargetException e) {
                            throw e.getTargetException(); // 保留真实异常
                        }
                    }
                }
        );
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
        System.out.println("Hikari活跃连接数: " + dataSource.getHikariPoolMXBean().getActiveConnections());
        System.out.println("Hikari空闲连接数: " + dataSource.getHikariPoolMXBean().getIdleConnections());
        System.out.println("Hikari总连接数: " + dataSource.getHikariPoolMXBean().getTotalConnections());
    }
}
