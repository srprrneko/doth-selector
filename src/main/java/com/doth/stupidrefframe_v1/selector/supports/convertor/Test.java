package com.doth.stupidrefframe_v1.selector.supports.convertor;

import com.doth.stupidrefframe_v1.selector.supports.BaseDruidUtil;
import com.doth.stupidrefframe_v1.selector.supports.SelectorHelper;
import com.doth.stupidrefframe_v1.selector.supports.SqlGenerator;
import com.doth.stupidrefframe_v1.testbean.Employee;

import java.sql.ResultSet;
import java.util.List;

/**
 * @project: test02
 * @package: com.doth.stupidrefframe_v1.selector.supports.convertor
 * @author: doth
 * @creTime: 2025-03-27  22:08
 * @desc: TODO
 * @v: 1.0
 */
public class Test {
    public static void main(String[] args) throws Throwable {
        String sql = "select e.id, e.name, d.name from employee e join department d on e.d_id = d.id where d.id = ?";
        ResultSet rs = BaseDruidUtil.executeQuery(sql, new Object[]{1});
        // System.out.println("resultSet = " + resultSet);

        // while (rs.next()) {
        //     JoinBeanConvertorPro convertor = new JoinBeanConvertorPro();
        //     Employee emp = convertor.convert(rs, Employee.class);
        //     System.out.println(emp);
        // }


        // System.out.println(emp);


    }
}
