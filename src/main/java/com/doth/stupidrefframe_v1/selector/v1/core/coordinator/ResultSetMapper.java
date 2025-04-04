package com.doth.stupidrefframe_v1.selector.v1.core.coordinator;

import com.doth.stupidrefframe_v1.exception.NoColumnExistException;
import com.doth.stupidrefframe_v1.exception.NonUniqueResultException;
import com.doth.stupidrefframe_v1.selector.v1.supports.convertor.BeanConvertor;
import com.doth.stupidrefframe_v1.selector.v1.supports.convertor.BeanConvertorFactory;
import com.doth.stupidrefframe_v1.selector.v1.supports.convertor.ConvertorType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// 新类：专注结果集到对象的映射
class ResultSetMapper {
    private final BeanConvertor convertor;

    public ResultSetMapper(ConvertorType convertorType) {
        this.convertor = BeanConvertorFactory.getConvertor(convertorType);
    }

    // ------------------ 结果集映射 ------------------
    public <T> List<T> map(ResultSet rs, Class<T> beanClass) {
        List<T> result = new ArrayList<>();
        try {
            while (rs.next()) {
                result.add(convertor.convert(rs, beanClass));
            }
            return result;
        } catch (NoColumnExistException e) {
            throw new RuntimeException("列映射失败: " + e.getMessage(), e);
        } catch (SQLException e) {
            throw new RuntimeException("数据库错误: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("查询失败: " + e.getMessage(), e);
        } catch (Throwable e) {
            throw new RuntimeException("未知异常: " + e.getMessage(), e);
        }
    }

    public static <T> T getSingleResult(List<T> list) {
        if (list.size() > 1) {
            throw new NonUniqueResultException("查询返回了 " + list.size() + " 条结果");
        }
        return list.isEmpty() ? null : list.get(0);
    }
}