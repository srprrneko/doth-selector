package com.doth.selector.common.poolsupports;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface ConnectionPool {
    Connection getConnection() throws SQLException;
    ResultSet executeQuery(String sql, Object[] params) throws SQLException;
    int executeUpdate(String sql, Object[] params) throws SQLException;
    void printPoolStatus();
}
