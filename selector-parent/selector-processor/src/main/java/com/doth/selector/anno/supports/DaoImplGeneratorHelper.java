package com.doth.selector.anno.supports;

import com.doth.selector.anno.Join;
import com.doth.selector.anno.processor.ProcessingContext;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>辅助 DaoImplGenerator 的工具类</p>
 * <p></p>
 * <p>职责说明</p>
 * <ol>
 *     <li>提取 DAO 抽象类继承父类的泛型实体类型</li>
 *     <li>解析查询方法名称中的条件表达式</li>
 *     <li>解析实体字段查询路径及格式化查询参数</li>
 * </ol>
 * <hr/>
 * <p>使用示例</p>
 * <ol>
 *     <li>在生成 DAO 实现类时, 使用 {@link #extractGenericEntityType(TypeElement)} 获取实体类型</li>
 *     <li>处理命名规范的查询方法（如 <code>queryByNameLikeAndAgeGt</code>）, 使用 {@link #parseMethodConditions(String)} 提取字段和操作符</li>
 *     <li>根据条件字段名和别名, 用 {@link #resolveFullColumnPath(TypeElement, String, AtomicInteger)} 构建完整的查询列路径</li>
 * </ol>
 * <hr/>
 * <p>后续改进</p>
 * <ol>
 *     <li>支持更多查询条件模式 (如复杂的逻辑运算符组合)</li>
 *     <li>提升字段解析的鲁棒性, 处理更复杂的实体关联场景</li>
 * </ol>
 */
public class DaoImplGeneratorHelper {
    private final ProcessingContext ctx;
    private static final Pattern CONDITION_PATTERN = Pattern.compile("^(.*?)(Like|In|Gt|Lt|Eq|Le|Ge|Ne)?$"); // 支持的比较条件


    public DaoImplGeneratorHelper(ProcessingContext ctx) {
        this.ctx = ctx;
    }


    /**
     * <p>提取 DAO 父类声明的泛型实体类型</p>
     * <p>说明: 如果 DAO 实现类继承了带泛型参数的父类（如 BaseDao&lt;Entity&gt;）, 本方法将返回泛型的实体类型</p>
     *
     * @param cls DAO 抽象类类型元素
     * @return 实体类型的 {@link TypeElement}, 若无法提取则返回 {@code null} 并打印错误
     */
    public TypeElement extractGenericEntityType(TypeElement cls) {
        TypeMirror sup = cls.getSuperclass();
        if (sup instanceof DeclaredType) {
            DeclaredType dt = (DeclaredType) sup;
            List<? extends TypeMirror> args = dt.getTypeArguments();
            if (!args.isEmpty()) {
                return (TypeElement) ctx.getTypeUtils().asElement(args.get(0));
            }
        }
        ctx.getMessager().printMessage(Diagnostic.Kind.ERROR, "无法获取实体类型", cls);
        return null;
    }

    /**
     * <p>解析方法名中的查询条件</p>
     * <p>说明: 将方法名后缀按照 With、Vz、And 等关键字拆分, 每个片段匹配 <code>字段+操作符</code></p>
     *
     * @param parts 方法名后缀字符串（例如 <code>"NameLikeAndAgeGt"</code>）
     * @return 由字段名和操作符组成的 {@link ConditionStructure} 列表
     */
    public List<ConditionStructure> parseMethodConditions(String parts) {
        List<ConditionStructure> list = new ArrayList<>();
        String[] splits = parts.split("With|Vz|And");
        for (String s : splits) {
            if (s.isEmpty()) continue;
            Matcher m = CONDITION_PATTERN.matcher(s);
            if (m.find()) {
                String field = m.group(1);
                String op = Optional.ofNullable(m.group(2)).orElse("Eq").toLowerCase();
                list.add(new ConditionStructure(field, op));
            }
        }
        return list;
    }

    /**
     * <p>构建实体属性的全路径</p>
     * <p>说明: 根据实体字段前缀匹配和 {@link Join} 注解, 在多级关联场景下递归拼接字段路径</p>
     *
     * @param ent       实体类类型元素
     * @param fieldName 查询字段名（可能包含嵌套前缀, 如 <code>"departmentCompanyName"</code>）
     * @param idx       别名索引计数器, 用于在递归中生成别名（t0, t1, ...）
     * @return 形如 <code>"t0.department.companyName"</code> 的完整列路径, 若无法匹配则返回 {@code null}
     */
    public String resolveFullColumnPath(TypeElement ent, String fieldName, AtomicInteger idx) {
        return resolveColumnPathRecursive(ent, fieldName, "t0", idx);
    }

    // 内部递归方法, 根据当前实体和剩余参数, 构造完整的列路径
    private String resolveColumnPathRecursive(TypeElement curr, String param, String alias, AtomicInteger idx) {
        String f = matchEntityFieldByPrefix(curr, param);
        if (f == null) return null;
        VariableElement fld = findField(curr, f);
        if (fld == null) return null;

        if (fld.getAnnotation(Join.class) != null) {
            TypeElement nested = (TypeElement) ctx.getTypeUtils().asElement(fld.asType());
            String nextAlias = "t" + idx.incrementAndGet();
            String remain = param.substring(f.length());
            return resolveColumnPathRecursive(nested, remain, nextAlias, idx);
        }
        return alias + "." + f;
    }

    // 匹配实体字段: 通过将参数 decapitalize 后截取不同长度前缀, 找出实体中存在的字段名
    private String matchEntityFieldByPrefix(TypeElement ent, String param) {
        String camel = decapitalize(param);
        for (int len = camel.length(); len > 0; len--) {
            String cand = camel.substring(0, len);
            if (entityHasField(ent, cand)) return cand;
        }
        return null;
    }

    // 检查实体类型是否包含指定字段
    private boolean entityHasField(TypeElement ent, String name) {
        for (Element e : ent.getEnclosedElements()) {
            if (e.getKind() == ElementKind.FIELD && e.getSimpleName().contentEquals(name)) {
                return true;
            }
        }
        return false;
    }

    // 查找实体中指定字段元素
    private VariableElement findField(TypeElement ent, String name) {
        for (Element e : ent.getEnclosedElements()) {
            if (e.getKind() == ElementKind.FIELD && e.getSimpleName().contentEquals(name)) {
                return (VariableElement) e;
            }
        }
        return null;
    }

    /**
     * <p>格式化查询参数</p>
     * <p>说明: 针对不同操作符处理参数格式, 如将数组转换为列表以适应 SQL 的 IN 查询</p>
     *
     * @param p        参数元素
     * @param operator 查询操作符（如 "in", "eq" 等）
     * @return 返回格式化后的参数字符串（如果是数组且操作符为 "in" 则包装为 Arrays.asList）
     */
    public String formatQueryParameter(VariableElement p, String operator) {
        String v = p.getSimpleName().toString();
        if ("in".equals(operator) && p.asType().getKind() == TypeKind.ARRAY) {
            return "Arrays.asList(" + v + ")";
        }
        return v;
    }

    // 将首字母小写
    private String decapitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * 条件字段结构
     */
    public static class ConditionStructure {
        public final String fieldName;
        public final String operator;

        public ConditionStructure(String f, String op) {
            this.fieldName = f;
            this.operator = op;
        }
    }
}