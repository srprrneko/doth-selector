package com.doth.selector.executor.supports;

import com.doth.selector.common.exception.mapping.NonUniqueResultException;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /**
     * 方便将任何 QueryList<T> 转换为 QueryList<R>
     * @param mapper lambda
     */
    public <R> QueryList<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = this.stream()
                              .map(mapper)
                              .collect(Collectors.toList());
        return QueryList.from(mapped);
    }

    /**
     * 将实体列表转换为 DTO 列表, 当前 仅支持方法引用的lambda条件, 如需要 也可以使用selector提供的api >> f(lambda)
     * @param dtoClass 目标 DTO 类型
     */
    @Deprecated // 查询生命周期中 泛型确定过晚, 后续优化
    public <D> QueryList<D> toDto(Class<D> dtoClass) {
        return this.map(entity -> {
            D dto = BeanUtils.instantiateClass(dtoClass);
            BeanUtils.copyProperties(entity, dto);
            return dto;
        });
    }
}
