package com.doth.selector.newtest;

import com.doth.selector.anno.CreateDaoImpl;
import com.doth.selector.anno.UseDTO;
import com.doth.selector.supports.testbean.join.Company;
import com.doth.selector.core.Selector;

import java.util.List;

/**
 * @author 贤
 * @version 1.0
 * @date 2025/5/26 0:39
 * @description todo
 */
@CreateDaoImpl
public abstract class CompanyDao extends Selector<Company> {



    public static void main(String[] args) {
        // CompanyDao dao = new CompanyDaoImpl();
        // List<Company> res = dao.queryAllBasic();
        // System.out.println("dao.queryAllBasic() = " + res);
        // System.out.println("Type = " + res.get(0).getClass());

    }
    @UseDTO(id = "simple")
    public List<Company> queryAllBasic() {
        return bud$().query2Lst(bud ->
                bud.eq(Company::getName, "公司A"));
    }
}