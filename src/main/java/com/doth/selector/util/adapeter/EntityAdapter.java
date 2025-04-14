package com.doth.selector.util.adapeter;

import com.doth.selector.testbean.join.Company;
import com.doth.selector.testbean.join.Department;
import com.doth.selector.testbean.join.Employee;
import com.doth.selector.anno.Join;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


// 新类：专注字段提取和参数处理
public class EntityAdapter {
    // ------------------ 通过实体对应着数据库列名的特点, 让实体属性自动为键, 免去map频繁指定键的繁琐操作 ------------------
    public static <T> LinkedHashMap<String, Object> extractNonNullFields(T entity) {
        // 定义一个map用于准备返回最终准备好的键值, 键为字段名, 值为字段值
        LinkedHashMap<String, Object> condMap = new LinkedHashMap<>();
        if (entity == null) return condMap;

        for (Field field : entity.getClass().getDeclaredFields()) { // 遍历所有字段
            try {
                field.setAccessible(true);
                Object value = field.get(entity); // 获取字段值
                if (value != null) {
                    condMap.put(field.getName(), value);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("字段提取失败: " + field.getName(), e);
            }
        }
        return condMap;
    }

    // ------------------ 将map键的值转换为object数组, 用于在执行前填充参数  ------------------
    public static Object[] buildParams(LinkedHashMap<String, Object> condBean) {
        List<Object> params = new ArrayList<>(); // 创建list保证顺序用于存储参数
        if (condBean != null) {
            for (Map.Entry<String, Object> entry : condBean.entrySet()) {
                Object value = entry.getValue(); // 通过字段取值

                // 展开集合为多个参数
                if (value instanceof Collection) { // 如果是集合
                    params.addAll((Collection<?>) value); // 将集合中的元素整块添加到参数列表中
                } else {
                    params.add(value); // 简单添加
                }
            }
        }
        return params.toArray(); // 直接返回数组
    }


    // ------------------ 同extractNonNullFields, 解决嵌套实体下别名对应不上的问题 ------------------
    public static <T> LinkedHashMap<String, Object> extractNestedFields(T entity) {
        return extractRecursive(entity, "t0", new AtomicInteger(1)); // new AtomicInteger(1) 自动处理i++; 忽略主表, 直接从一开始
    }

    private static LinkedHashMap<String, Object> extractRecursive(Object entity, String currentAlias, AtomicInteger counter) {
        LinkedHashMap<String, Object> condMap = new LinkedHashMap<>(); // 最终用于返回的map, 键为字段名, 值为字段值
        if (entity == null) return condMap;

        for (Field field : entity.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object value = field.get(entity);
                if (value == null) continue; // 跳过为空的字段 (因为是包装类所以不设值都为null)

                // 判断是否是关联对象字段
                if (field.isAnnotationPresent(Join.class)) {
                    // counter.getAndIncrement() 先返回值, 再++
                    String nestedAlias = "t" + counter.getAndIncrement(); // 让下一次递归使用新的别名
                    // 当前嵌套对象的值解析完成,
                    LinkedHashMap<String, Object> nestedMap = extractRecursive(value, nestedAlias, counter); // 获取当前嵌套对象后, 递归解析
                    condMap.putAll(nestedMap); // 将最终装填好的map全部添加进主map中
                } else { // 无论如何添加完当前实体的全部字段
                    condMap.put(currentAlias + "." + field.getName(), value); // 别名+字段名处理
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("字段提取失败: " + field.getName(), e);
            }
        }
        return condMap;
    }

    @Test
    public void testNormal() {
        LinkedHashMap<String, Object> condMap = extractNonNullFields(new Employee(1, "John", null));
        condMap.forEach((k,v) -> System.out.println("Key: " + k + ", Value: " + v));
    }

    @Test
    public void testRecursive() {
        Employee employee = new Employee();
        employee.setId(1);
        employee.setName("John");

        Department department = new Department();
        department.setId(2);
        department.setName("HR");
        Company company = new Company();
        company.setId(3);
        company.setName("ABC");


        department.setCompany(company);
        employee.setDepartment(department);

        LinkedHashMap<String, Object> map = extractNestedFields(employee);
        map.forEach((key, value) -> System.out.println("Key: " + key + ", Value: " + value));
    }

}