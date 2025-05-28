package com.doth.selector.anno.processor.codegena;

import com.doth.selector.anno.processor.model.ConditionStructure;

import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MethodBodyGenerator {

    // 主入口：根据抽象方法生成其实现
    public static void generateImpl(ExecutableElement method, Writer writer, Messager messager,
                                    TypeElement entityType, Types typeUtils) throws Exception {

        String methodName = method.getSimpleName().toString();
        writer.write("    @Override\n");
        writer.write("    public " + method.getReturnType() + " " + methodName + "(");
        writeParameters(writer, method.getParameters());
        writer.write(") {\n");

        if (methodName.startsWith("queryBy")) {
            String condPart = methodName.substring("queryBy".length());
            List<ConditionStructure> conditions = parseConditions(condPart);
            List<? extends VariableElement> params = method.getParameters();

            if (conditions.size() != params.size()) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "参数数量不匹配 预期: " + conditions.size() + " 实际: " + params.size(), method);
                writer.write("        return null;\n    }\n\n");
                return;
            }

            AtomicInteger aliasIndex = new AtomicInteger(0);
            writer.write("        return bud$().query2Lst(builder -> {\n");
            writer.write("            builder");

            for (int i = 0; i < conditions.size(); i++) {
                ConditionStructure cond = conditions.get(i);
                String column = ColumnPathResolver.resolve(entityType, cond.fieldName, "t0", aliasIndex, typeUtils);
                String value = processParam(params.get(i), cond.operator);
                writer.write("\n                ." + cond.operator + "(\"" + column + "\", " + value + ")");
            }

            writer.write(";\n        });\n");
        } else {
            writer.write("        return null;\n");
        }

        writer.write("    }\n\n");
    }

    private static String processParam(VariableElement param, String operator) {
        String value = param.getSimpleName().toString();
        if ("in".equals(operator) && param.asType().getKind() == TypeKind.ARRAY) {
            return "Arrays.asList(" + value + ")";
        }
        return value;
    }

    private static void writeParameters(Writer writer, List<? extends VariableElement> params) throws Exception {
        for (int i = 0; i < params.size(); i++) {
            VariableElement param = params.get(i);
            writer.write(param.asType() + " " + param.getSimpleName());
            if (i < params.size() - 1) writer.write(", ");
        }
    }

    private static List<ConditionStructure> parseConditions(String condParts) {
        List<ConditionStructure> conditions = new ArrayList<>();
        String[] splits = condParts.split("With|Vz|And");
        Pattern pattern = Pattern.compile("^(.*?)(Like|In|Gt|Lt|Eq|Le|Ge|Ne)?$");
        for (String part : splits) {
            if (part.isEmpty()) continue;
            Matcher matcher = pattern.matcher(part);
            if (matcher.find()) {
                String field = matcher.group(1);
                String operator = Optional.ofNullable(matcher.group(2)).orElse("Eq").toLowerCase();
                conditions.add(new ConditionStructure(field, operator));
            }
        }
        return conditions;
    }
}
