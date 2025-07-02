package com.doth.selector.newtest;

import com.doth.selector.anno.CreateDaoImpl;
import com.doth.selector.core.Selector;
import com.doth.selector.supports.testbean.join3.BookCard;
import com.doth.selector.supports.testbean.join3.BookNameAndSexDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;

@CreateDaoImpl
@Slf4j
public abstract class BookCardDAO extends Selector<BookCard> {

    public abstract BookCard getByCid(Integer id);

    public static void main(String[] args) {

        BookCardDAO dao = new BookCardDAOImpl();
        val start = System.currentTimeMillis();
        // for (int i = 0; i < 50000; i++) {
        val dto = dao.queryByNameAndSex("%zs%", "M");
        // }
        val end = System.currentTimeMillis();
        // log.info("cost: {}",);
        System.out.println("( end - start) = " + (end - start));
        System.out.println(dto);

    }

    @Test
    public void test2() {
        BookCardDAO dao = new BookCardDAOImpl();

        val byCid = dao.getByCid(1001);
        System.out.println(byCid);

    }

    @Test
    public void test() {
        BookCardDAO dao = new BookCardDAOImpl();
        // List<BookNameAndSexDTO> dto = dao.queryByNameAndSex("'zs'", "'M'");
        //
        // System.out.println(dto);
    }

    public BookNameAndSexDTO queryByNameAndSex(String name, String sex) {
        return bud$().query(builder -> {
                    builder.like(BookCard::getName, name)
                            .eq(BookCard::getSex, sex);
                }).toDto(BookNameAndSexDTO.class)
                .toOne();
    }
}