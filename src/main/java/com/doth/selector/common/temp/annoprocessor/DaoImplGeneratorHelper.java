package com.doth.selector.common.temp.annoprocessor;

import com.doth.selector.anno.Join;
import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提供 DaoImplGenerator 所需的辅助方法
 */
public class DaoImplGeneratorHelper {
    private final ProcessingContext ctx;
    private static final Pattern CONDITION_PATTERN = Pattern.compile("^(.*?)(Like|In|Gt|Lt|Eq|Le|Ge|Ne)?$");

    public DaoImplGeneratorHelper(ProcessingContext ctx) {
        this.ctx = ctx;
    }

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

    public String resolveFullColumnPath(TypeElement ent, String fieldName, AtomicInteger idx) {
        return resolveColumnPathRecursive(ent, fieldName, "t0", idx);
    }

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

    private String matchEntityFieldByPrefix(TypeElement ent, String param) {
        String camel = decapitalize(param);
        for (int len = camel.length(); len > 0; len--) {
            String cand = camel.substring(0, len);
            if (entityHasField(ent, cand)) return cand;
        }
        return null;
    }

    private boolean entityHasField(TypeElement ent, String name) {
        for (Element e : ent.getEnclosedElements()) {
            if (e.getKind() == ElementKind.FIELD && e.getSimpleName().contentEquals(name)) {
                return true;
            }
        }
        return false;
    }

    private VariableElement findField(TypeElement ent, String name) {
        for (Element e : ent.getEnclosedElements()) {
            if (e.getKind() == ElementKind.FIELD && e.getSimpleName().contentEquals(name)) {
                return (VariableElement) e;
            }
        }
        return null;
    }

    public String formatQueryParameter(VariableElement p, String operator) {
        String v = p.getSimpleName().toString();
        if ("in".equals(operator) && p.asType().getKind() == TypeKind.ARRAY) {
            return "Arrays.asList(" + v + ")";
        }
        return v;
    }

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