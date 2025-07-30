package com.doth.selector.executor.query;

import com.doth.selector.executor.supports.QueryList;

public interface QueryExecutorTag<T> {
    default QueryList<T> query(Object... args) {
        return null;
    }
}
