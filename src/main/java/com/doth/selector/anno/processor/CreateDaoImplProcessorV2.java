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

    // 初始化环境
    private Filer filer;
    private Messager messager;
    private Types typeUtils;
    private Elements elementUtils;

    // 动态获取jdk最高版本
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    // 初始化环境
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        filer = env.getFiler(); // 用于文件输出
        messager = env.getMessager(); // 编译器信息输出
        typeUtils = env.getTypeUtils(); // 操作类型的工具
        elementUtils = env.getElementUtils(); // 操作元素的工具
    }

    /**
     * 入口
     * @param annotations the annotation types requested to be processed
     * @param roundEnv  environment for information about the current and prior round
     * @return 处理是否成功
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 循环被标记的元素
        for (Element element : roundEnv.getElementsAnnotatedWith(CreateDaoImpl.class)) {
            // 如果不是类, 或者不是抽象类
            if (element.getKind() != ElementKind.CLASS || !((TypeElement) element).getModifiers().contains(Modifier.ABSTRACT)) {
                // 则输出提示
                messager.printMessage(Diagnostic.Kind.ERROR, "继承 Selector 请使用抽象类!!", element);
                continue;
            }
            // 其他则 开始画实现类 (强转成类)
            writeImplClz((TypeElement) element);
        }
        return true;
    }

    private void writeImplClz(TypeElement abstractClass) {
        // 为类起名
        String className = abstractClass.getSimpleName() + "Impl";
        String packageName = elementUtils.getPackageOf(abstractClass).getQualifiedName().toString(); // 因为要继承所以起包名

        // 获取泛型参数 的具体类型
        TypeElement entityTypeElement = getGeneric(abstractClass);

        // 创建文件 传入全限定类名
        try (Writer writer = filer.createSourceFile(packageName + "." + className).openWriter()) {
            // 包名结构初始化
            writer.write("package " + packageName + ";\n\n");
            writer.write("import java.util.*;\n");
            writer.write("import java.util.function.Consumer;\n\n");

            // 类结构初始化
            writer.write("public class " + className + " extends " + abstractClass.getSimpleName() + " {\n");
            writer.write("    public " + className + "() { super(); }\n\n"); // 确保付父类正确初始化

            // 遍历该类中的元素
            for (Element enclosedElement : abstractClass.getEnclosedElements()) {
                // 仅处理抽象方法
                if (enclosedElement.getKind() == ElementKind.METHOD && enclosedElement.getModifiers().contains(Modifier.ABSTRACT)) {
                    // 画方法 (代码生成器, 元素->强转后的, 实体类型)
                    writeImplMethod(writer, (ExecutableElement) enclosedElement, entityTypeElement);
                }
            }
            // 画完之后闭合
            writer.write("}\n");
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "生成实现类失败: " + e.getMessage());
        }
    }

    /**
     * 获取泛型的方法
     * @param currentClz 当前类
     * @return 泛型类型
     */
    private TypeElement getGeneric(TypeElement currentClz) {
        // Java 编译期注解处理环境 中的一个接口，用于表示 Java 类型的抽象概念
        TypeMirror superClass = currentClz.getSuperclass();

        // 判断是否是声明的类型[具名类型"通过class,interface声明的类型"] (过滤基本类型)
        if (superClass instanceof DeclaredType) {
            // 获取泛型参数集合
            List<? extends TypeMirror> typeArgs = ((DeclaredType) superClass).getTypeArguments();
            // 参数不等于空, 则
            if (!typeArgs.isEmpty()) {
                // 取第一个
                return (TypeElement) typeUtils.asElement(typeArgs.get(0));
            }
        }
        messager.printMessage(Diagnostic.Kind.ERROR, "无法获取实体类型", currentClz);
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
            // 创建共享的索引计数器
            AtomicInteger aliasIndex = new AtomicInteger(0);

            writer.write("        return bud$().query2Lst(builder -> {\n");
            writer.write("            builder");
            for (int i = 0; i < conditions.size(); i++) {
                ConditionInfo cond = conditions.get(i);
                String key = resolveColumnKey(entityType, cond.fieldName, aliasIndex);
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

    private String resolveColumnKey(TypeElement entityType, String fieldName, AtomicInteger index) {
        return resolveColumnKeyRecursive(
            entityType, 
            fieldName, 
            "t0", 
            index
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