package com.doth.selector.example.testdao.newtest;
import com.doth.selector.anno.AutoImpl;
import com.doth.selector.anno.UseDTO;
import com.doth.selector.example.testbean.join.Company;
import com.doth.selector.core.Selector;

import java.util.List;

@AutoImpl
public abstract class CompanyDao extends Selector<Company> {



    public static void main(String[] args) {
        // CompanyDao dao = new CompanyDaoImpl();
        // List<Company> res = dao.queryAllBasic();
        // System.out.println("dao.queryAllBasic() = " + res);
        // System.out.println("Type = " + res.get(0).getClass());

    }
    @UseDTO(id = "simple")
    public List<Company> queryAllBasic() {
        return bud$().query(bud ->
                bud.eq(Company::getName, "公司A"));
    }
}