package com.doth.stupidrefframe.anno.processor;

import com.doth.stupidrefframe.anno.CreateDaoImpl;
import com.doth.stupidrefframe.anno.Join;
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
import java.io.Writer;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.doth.stupidrefframe.anno.CreateDaoImpl")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class CreateDaoImplProcessorV1 extends AbstractProcessor {

    private Filer filer;
    private Messager messager;
    private Types typeUtils;
    private Elements elementUtils;

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
                messager.printMessage(Diagnostic.Kind.ERROR, "继承查询门面类请使用抽象类", element);
                continue;
            }
            generateEmptyImplementation((TypeElement) element);
        }
        return true;
    }

    private void generateEmptyImplementation(TypeElement abstractClass) {
        String className = abstractClass.getSimpleName() + "Impl";

        String packageName = elementUtils.getPackageOf(abstractClass).getQualifiedName().toString();
        TypeElement entityTypeElement = extractEntityType(abstractClass);

        try (Writer writer = filer.createSourceFile(packageName + "." + className).openWriter()) {
            writer.write("package " + packageName + ";\n\n");
            writer.write("import java.util.LinkedHashMap;\n\n");
            writer.write("public class " + className + " extends " + abstractClass.getSimpleName() + " {\n");


            // 生成构造函数保证父类泛型初始化
            writer.write("    public " + className + "() {\n");
            writer.write("        super();\n");
            writer.write("    }\n\n");

            for (Element enclosedElement : abstractClass.getEnclosedElements()) {
                if (enclosedElement.getKind() == ElementKind.METHOD && enclosedElement.getModifiers().contains(Modifier.ABSTRACT)) {
                    ExecutableElement method = (ExecutableElement) enclosedElement;
                    generateMethodImpl(writer, method, entityTypeElement);
                }
            }

            writer.write("}\n");
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "生成实现类失败: " + e.getMessage());
        }
    }



    private TypeElement extractEntityType(TypeElement abstractClass) {
        TypeMirror superClass = abstractClass.getSuperclass();
        if (superClass instanceof DeclaredType) {
            List<? extends TypeMirror> typeArgs = ((DeclaredType) superClass).getTypeArguments();
            if (!typeArgs.isEmpty()) {
                TypeMirror entityType = typeArgs.get(0);
                return (TypeElement) typeUtils.asElement(entityType);
            }
        }
        messager.printMessage(Diagnostic.Kind.ERROR, "无法获取实体类型", abstractClass);
        return null;
    }



    private void generateMethodImpl(Writer writer, ExecutableElement method, TypeElement entityType) throws Exception {
        String methodName = method.getSimpleName().toString();
        writer.write("    @Override\n");
        writer.write("    public " + method.getReturnType() + " " + methodName + "(");

        List<? extends VariableElement> params = method.getParameters();
        boolean firstParam = true;
        for (VariableElement param : params) {
            if (!firstParam) writer.write(", ");
            writer.write(param.asType() + " " + param.getSimpleName());
            firstParam = false;
        }
        writer.write(") {\n");

        if (methodName.startsWith("queryBy") && params.size() == 1) {
            VariableElement param = params.get(0);
            String paramName = param.getSimpleName().toString();
            String byPart = methodName.substring("queryBy".length());
            String propertyPath = camelToPath(byPart);
            String columnKey = resolveColumnKey(entityType, propertyPath);

            if (columnKey != null) {
                writer.write("        LinkedHashMap<String, Object> cond = new LinkedHashMap<>();\n");
                writer.write("        cond.put(\"" + columnKey + "\", " + paramName + ");\n");
                writer.write("        return dct$().query2Lst(cond);\n");
            } else {
                writer.write("        // 无法解析字段路径: " + propertyPath + "\n");
                writer.write("        return null;\n");
            }
        } else {
            if (method.getReturnType().getKind().isPrimitive()) {
                writer.write("        return " + getDefaultPrimitiveValue(method.getReturnType().toString()) + ";\n");
            } else {
                writer.write("        return null;\n");
            }
        }

        writer.write("    }\n\n");
    }



    private String camelToPath(String camel) {
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i != 0) path.append('.');
                path.append(Character.toLowerCase(c));
            } else {
                path.append(c);
            }
        }
        return path.toString();
    }

    private String resolveColumnKey(TypeElement entityType, String propertyPath) {
        String[] parts = propertyPath.split("\\.");
        String currentAlias = "t0"; // 根实体别名为t0
        TypeElement currentEntity = entityType;

        for (String part : parts) {
            VariableElement field = findField(currentEntity, part);
            if (field == null) return null;

            Join join = field.getAnnotation(Join.class);
            if (join != null) {
                // 遇到@Join注解时重置别名
                currentAlias = join.alias();
                TypeMirror fieldType = field.asType();
                currentEntity = (TypeElement) typeUtils.asElement(fieldType);
            } else {
                // 普通字段直接追加
                currentAlias += "." + part;
            }
        }

        return currentAlias; // 直接返回构建完成的别名路径
    }

    private VariableElement findField(TypeElement type, String fieldName) {
        for (Element enclosed : type.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD && enclosed.getSimpleName().contentEquals(fieldName)) {
                return (VariableElement) enclosed;
            }
        }
        return null;
    }

    private String getDefaultPrimitiveValue(String type) {
        switch (type) {
            case "boolean": return "false";
            case "int":     return "0";
            case "long":    return "0L";
            case "double":  return "0.0";
            default:        return "0";
        }
    }
}