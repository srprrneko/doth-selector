package com.doth.selector.anno.processor;

import com.doth.selector.anno.CreateDaoImpl;
import com.doth.selector.anno.Join;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.doth.selector.anno.CreateDaoImpl")
public class CreateDaoImplProcessorV2 extends AbstractProcessor {

    private Filer filer;
    private Messager messager;
    private Types typeUtils;
    private Elements elementUtils;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        filer = env.getFiler();
        messager = env.getMessager();
        typeUtils = env.getTypeUtils();
        elementUtils = env.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(CreateDaoImpl.class)) {
            if (element.getKind() != ElementKind.CLASS || !((TypeElement) element).getModifiers().contains(Modifier.ABSTRACT)) {
                messager.printMessage(Diagnostic.Kind.ERROR, "继承 Selector 请使用抽象类!!", element);
                continue;
            }
            writeImplClz((TypeElement) element);
        }
        return true;
    }

    private void writeImplClz(TypeElement abstractClass) {
        String className = abstractClass.getSimpleName() + "Impl";
        String packageName = elementUtils.getPackageOf(abstractClass).getQualifiedName().toString();
        TypeElement entityTypeElement = getGeneric(abstractClass);

        try (Writer writer = filer.createSourceFile(packageName + "." + className).openWriter()) {
            writer.write("package " + packageName + ";\n\n");
            writer.write("import java.util.*;\n");
            writer.write("import java.util.function.Consumer;\n\n");

            writer.write("public class " + className + " extends " + abstractClass.getSimpleName() + " {\n");
            writer.write("    public " + className + "() { super(); }\n\n");

            for (Element enclosedElement : abstractClass.getEnclosedElements()) {
                if (enclosedElement.getKind() == ElementKind.METHOD && 
                    enclosedElement.getModifiers().contains(Modifier.ABSTRACT)) {
                    writeImplMethod(writer, (ExecutableElement) enclosedElement, entityTypeElement);
                }
            }
            writer.write("}\n");
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "生成实现类失败: " + e.getMessage());
        }
    }

    private TypeElement getGeneric(TypeElement abstractClz) {
        TypeMirror superClass = abstractClz.getSuperclass();
        if (superClass instanceof DeclaredType) {
            List<? extends TypeMirror> typeArgs = ((DeclaredType) superClass).getTypeArguments();
            if (!typeArgs.isEmpty()) {
                return (TypeElement) typeUtils.asElement(typeArgs.get(0));
            }
        }
        messager.printMessage(Diagnostic.Kind.ERROR, "无法获取实体类型", abstractClz);
        return null;
    }

    private void writeImplMethod(Writer writer, ExecutableElement method, TypeElement entityType) throws Exception {
        String methodName = method.getSimpleName().toString();
        writer.write("    @Override\n");
        writer.write("    public " + method.getReturnType() + " " + methodName + "(");
        writeParameters(writer, method.getParameters());
        writer.write(") {\n");

        if (methodName.startsWith("queryBy")) {
            String condParts = methodName.substring("queryBy".length());
            List<ConditionInfo> conditions = parseConditionParts(condParts);
            List<? extends VariableElement> params = method.getParameters();

            if (conditions.size() != params.size()) {
                messager.printMessage(Diagnostic.Kind.ERROR, 
                    "参数数量不匹配 预期: " + conditions.size() + " 实际: " + params.size(), method);
                writer.write("        return null;\n    }\n\n");
                return;
            }

            writer.write("        return bud$().query2Lst(builder -> {\n");
            writer.write("            builder");
            for (int i = 0; i < conditions.size(); i++) {
                ConditionInfo cond = conditions.get(i);
                String key = resolveColumnKey(entityType, cond.fieldName);
                String value = processParameterValue(params.get(i), cond.operator);
                
                writer.write("\n                ." + cond.operator + "(\"" + key + "\", " + value + ")");
            }
            writer.write(";\n        });\n");
        } else {
            writer.write("        return null;\n");
        }
        writer.write("    }\n\n");
    }

    private List<ConditionInfo> parseConditionParts(String condParts) {
        List<ConditionInfo> conditions = new ArrayList<>();
        String[] splits = condParts.split("With|Vz|And");
        Pattern pattern = Pattern.compile("^(.*?)(Like|In|Gt|Lt|Eq)?$");
        
        for (String part : splits) {
            if (part.isEmpty()) continue;
            Matcher matcher = pattern.matcher(part);
            if (matcher.find()) {
                String field = matcher.group(1);
                String operator = Optional.ofNullable(matcher.group(2))
                                         .orElse("Eq").toLowerCase();
                conditions.add(new ConditionInfo(field, operator));
            }
        }
        return conditions;
    }

    private String resolveColumnKey(TypeElement entityType, String fieldName) {
        return resolveColumnKeyRecursive(
            entityType, 
            fieldName, 
            "t0", 
            new AtomicInteger(0)
        );
    }

    private String resolveColumnKeyRecursive(TypeElement currentEntity, String param, 
                                           String alias, AtomicInteger index) {
        String fieldName = findLongestMatchingField(currentEntity, param);
        if (fieldName == null) return null;

        VariableElement field = findField(currentEntity, fieldName);
        if (field == null) return null;

        if (field.getAnnotation(Join.class) != null) {
            TypeElement nestedEntity = (TypeElement) typeUtils.asElement(field.asType());
            String newAlias = "t" + index.incrementAndGet();
            String remainingPath = param.substring(fieldName.length());
            return resolveColumnKeyRecursive(nestedEntity, remainingPath, newAlias, index);
        }

        return alias + "." + fieldName;
    }

    private String processParameterValue(VariableElement param, String operator) {
        String value = param.getSimpleName().toString();
        if ("in".equals(operator) && param.asType().getKind() == TypeKind.ARRAY) {
            return "Arrays.asList(" + value + ")";
        }
        return value;
    }

    private void writeParameters(Writer writer, List<? extends VariableElement> params) throws IOException {
        boolean first = true;
        for (VariableElement param : params) {
            if (!first) writer.write(", ");
            writer.write(param.asType() + " " + param.getSimpleName());
            first = false;
        }
    }

    private static class ConditionInfo {
        final String fieldName;
        final String operator;

        ConditionInfo(String fieldName, String operator) {
            this.fieldName = fieldName;
            this.operator = operator;
        }
    }

    // 以下辅助方法与原始实现保持一致
    private String findLongestMatchingField(TypeElement entityType, String param) {
        String camelPath = toCamelCase(param); // 转换成小驼峰, 匹配类字段

        for (int len = camelPath.length(); len > 0; len--) {
            String candidate = camelPath.substring(0, len);

            // 去实体中查找是否存在当前字段
            if (hasField(entityType, candidate)) {
                // 找到则返回
                return candidate;
            }
        }
        return null;
    }

    private boolean hasField(TypeElement entityType, String fieldName) {
        for (Element enclosed : entityType.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD && enclosed.getSimpleName().toString().equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    private String toCamelCase(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private VariableElement findField(TypeElement type, String fieldName) {
        for (Element enclosed : type.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD && enclosed.getSimpleName().contentEquals(fieldName)) {
                return (VariableElement) enclosed;
            }
        }
        return null;
    }
}