package com.doth.selector.executor.supports;

import com.doth.selector.common.exception.NonUniqueResultException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class QueryList<T> extends ArrayList<T> {

    public QueryList(Collection<? extends T> c) {
        super(c);
    }

    /**
     * 链式：把多条记录 [取首条并做非唯一防御]
     */
    public T toOne() {
        if (size() > 1) {
            throw new NonUniqueResultException("结果超出！预期 1 条，实际 " + size() + " 条");
        }
        return isEmpty() ? null : get(0);
    }

    /**
     * 如需回到普通可变 List，就复制一份返回
     */
    public List<T> toList() {
        return new ArrayList<>(this);
    }


    /**
     * 任意 List → QueryList 的静态包装
     * @param list list
     */
    public static <T> QueryList<T> from(List<T> list) {
        if (list instanceof QueryList) {
            QueryList<T> q = (QueryList<T>) list;
            return q;
        }
        return new QueryList<>(list);
    }
}
