package com.doth.selector.annotation.processor.codegena;

import com.doth.selector.annotation.Join;

import javax.lang.model.element.*;
import javax.lang.model.util.Types;
import java.util.concurrent.atomic.AtomicInteger;

public class ColumnPathResolver {

    public static String resolve(TypeElement entity, String param, String alias,
                                 AtomicInteger index, Types typeUtils) {
        String matchedField = findLongestMatch(entity, param);
        if (matchedField == null) return null;

        VariableElement field = findField(entity, matchedField);
        if (field == null) return null;

        if (field.getAnnotation(Join.class) != null) {
            TypeElement nested = (TypeElement) typeUtils.asElement(field.asType());
            String newAlias = "t" + index.incrementAndGet();
            return resolve(nested, param.substring(matchedField.length()), newAlias, index, typeUtils);
        }

        return alias + "." + matchedField;
    }

    private static String findLongestMatch(TypeElement type, String param) {
        String camel = toCamel(param);
        for (int len = camel.length(); len > 0; len--) {
            String candidate = camel.substring(0, len);
            if (hasField(type, candidate)) return candidate;
        }
        return null;
    }

    private static boolean hasField(TypeElement type, String name) {
        for (Element e : type.getEnclosedElements()) {
            if (e.getKind() == ElementKind.FIELD && e.getSimpleName().toString().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static VariableElement findField(TypeElement type, String name) {
        for (Element e : type.getEnclosedElements()) {
            if (e.getKind() == ElementKind.FIELD && e.getSimpleName().contentEquals(name)) {
                return (VariableElement) e;
            }
        }
        return null;
    }

    private static String toCamel(String s) {
        return s == null || s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
