// package com.doth.selector.annotation.processor;
//
// import com.doth.selector.annotation.CreateDaoImpl;
// import com.doth.selector.annotation.Join;
// import com.google.auto.service.AutoService;
//
// import javax.annotation.processing.*;
// import javax.lang.model.SourceVersion;
// import javax.lang.model.element.*;
// import javax.lang.model.type.DeclaredType;
// import javax.lang.model.type.TypeKind;
// import javax.lang.model.type.TypeMirror;
// import javax.lang.model.util.Elements;
// import javax.lang.model.util.Types;
// import javax.tools.Diagnostic;
// import java.io.IOException;
// import java.io.Writer;
// import java.util.*;
// import java.util.concurrent.atomic.AtomicInteger;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;
//
// @AutoService(Processor.class)
// @SupportedAnnotationTypes("com.doth.selector.annotation.CreateDaoImpl")
// public class CreateDaoImplProcessorV2 extends AbstractProcessor {
//
//     private Filer filer;
//     private Messager messager;
//     private Types typeUtils;
//     private Elements elementUtils;
//
//     @Override
//     public SourceVersion getSupportedSourceVersion() {
//         return SourceVersion.latestSupported();
//     }
//
//     @Override
//     public synchronized void init(ProcessingEnvironment env) {
//         super.init(env);
//         filer = env.getFiler(); // 用于生成类
//         messager = env.getMessager(); // 用于打印编译期消息
//         typeUtils = env.getTypeUtils(); // 类型工具
//         elementUtils = env.getElementUtils(); // 元素工具
//     }
//
//     @Override
//     public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
//         // 扫描所有带 @CreateDaoImpl 的类
//         for (Element element : roundEnv.getElementsAnnotatedWith(CreateDaoImpl.class)) {
//             // 只处理抽象类
//             if (element.getKind() != ElementKind.CLASS || !((TypeElement) element).getModifiers().contains(Modifier.ABSTRACT)) {
//                 messager.printMessage(Diagnostic.Kind.ERROR, "继承 Selector 请使用抽象类!!", element);
//                 continue;
//             }
//             // 写实现类
//             generateDaoImplementation((TypeElement) element);
//         }
//         return true;
//     }
//
//     // 写 xxxImpl 实现类
//     private void generateDaoImplementation(TypeElement abstractClass) {
//         String className = abstractClass.getSimpleName() + "Impl";
//         String packageName = elementUtils.getPackageOf(abstractClass).getQualifiedName().toString();
//         TypeElement entityTypeElement = extractGenericEntityType(abstractClass); // 获取泛型类型
//
//         try (Writer writer = filer.createSourceFile(packageName + "." + className).openWriter()) {
//             // 写包和导入
//             writer.write("package " + packageName + ";\n\n");
//             writer.write("import java.util.*;\n");
//             writer.write("import java.util.function.Consumer;\n\n");
//
//             // 写类头
//             writer.write("public class " + className + " extends " + abstractClass.getSimpleName() + " {\n");
//             writer.write("    public " + className + "() { super(); }\n\n");
//
//             // 写每个抽象方法实现
//             for (Element enclosedElement : abstractClass.getEnclosedElements()) {
//                 if (enclosedElement.getKind() == ElementKind.METHOD && enclosedElement.getModifiers().contains(Modifier.ABSTRACT)) {
//                     generateDaoMethodImplementation(writer, (ExecutableElement) enclosedElement, entityTypeElement);
//                 }
//             }
//
//             writer.write("}\n");
//         } catch (Exception e) {
//             messager.printMessage(Diagnostic.Kind.ERROR, "生成实现类失败: " + e.getMessage());
//         }
//     }
//
//     // 写每个方法实现
//     private void generateDaoMethodImplementation(Writer writer, ExecutableElement method, TypeElement entityType) throws Exception {
//         String methodName = method.getSimpleName().toString();
//         writer.write("    @Override\n");
//         writer.write("    public " + method.getReturnType() + " " + methodName + "(");
//         writeMethodParameters(writer, method.getParameters()); // 写参数
//         writer.write(") {\n");
//
//         if (methodName.startsWith("queryBy")) {
//             // 解析条件字段
//             String condParts = methodName.substring("queryBy".length());
//             List<ConditionStructure> conditions = parseMethodConditions(condParts);
//
//             // 参数数量校验
//             List<? extends VariableElement> params = method.getParameters();
//             if (conditions.size() != params.size()) {
//                 messager.printMessage(Diagnostic.Kind.ERROR, "参数数量不匹配 预期: " + conditions.size() + " 实际: " + params.size(), method);
//                 writer.write("        return null;\n    }\n\n");
//                 return;
//             }
//
//             // 写 builder 构造器
//             AtomicInteger aliasIndex = new AtomicInteger(0);
//             writer.write("        return bud$().query2Lst(builder -> {\n");
//             writer.write("            builder");
//             for (int i = 0; i < conditions.size(); i++) {
//                 ConditionStructure cond = conditions.get(i);
//                 String key = resolveFullColumnPath(entityType, cond.fieldName, aliasIndex); // 解析字段路径
//                 String value = formatQueryParameter(params.get(i), cond.operator); // 格式化右值
//                 writer.write("\n                ." + cond.operator + "(\"" + key + "\", " + value + ")");
//             }
//             writer.write(";\n        });\n");
//         } else {
//             writer.write("        return null;\n");
//         }
//
//         writer.write("    }\n\n");
//     }
//
//     // 获取父类泛型实体类型
//     private TypeElement extractGenericEntityType(TypeElement currentClz) {
//         TypeMirror superClass = currentClz.getSuperclass();
//         if (superClass instanceof DeclaredType) {
//             List<? extends TypeMirror> typeArgs = ((DeclaredType) superClass).getTypeArguments();
//             if (!typeArgs.isEmpty()) {
//                 return (TypeElement) typeUtils.asElement(typeArgs.get(0));
//             }
//         }
//         messager.printMessage(Diagnostic.Kind.ERROR, "无法获取实体类型", currentClz);
//         return null;
//     }
//
//     // 解析 queryBy 字段部分
//     private List<ConditionStructure> parseMethodConditions(String condParts) {
//         List<ConditionStructure> conditions = new ArrayList<>();
//         String[] splits = condParts.split("With|Vz|And");
//         Pattern pattern = Pattern.compile("^(.*?)(Like|In|Gt|Lt|Eq|Le|Ge|Ne)?$");
//
//         for (String part : splits) {
//             if (part.isEmpty()) continue;
//             Matcher matcher = pattern.matcher(part);
//             if (matcher.find()) {
//                 String field = matcher.group(1);
//                 String operator = Optional.ofNullable(matcher.group(2)).orElse("Eq").toLowerCase();
//                 conditions.add(new ConditionStructure(field, operator));
//             }
//         }
//         return conditions;
//     }
//
//     // 解析完整字段路径（支持 join）
//     private String resolveFullColumnPath(TypeElement entityType, String fieldName, AtomicInteger index) {
//         return resolveColumnPathRecursive(entityType, fieldName, "t0", index);
//     }
//
//     // 递归解析字段路径（处理嵌套 join）
//     private String resolveColumnPathRecursive(TypeElement currentEntity, String param, String alias, AtomicInteger index) {
//         String fieldName = matchEntityFieldByPrefix(currentEntity, param); // 匹配字段
//         if (fieldName == null) return null;
//
//         VariableElement field = findField(currentEntity, fieldName);
//         if (field == null) return null;
//
//         // 如果是 Join 字段，递归进入子对象
//         if (field.getAnnotation(Join.class) != null) {
//             TypeElement nestedEntity = (TypeElement) typeUtils.asElement(field.asType());
//             String newAlias = "t" + index.incrementAndGet();
//             String remainingPath = param.substring(fieldName.length());
//             return resolveColumnPathRecursive(nestedEntity, remainingPath, newAlias, index);
//         }
//
//         // 普通字段
//         return alias + "." + fieldName;
//     }
//
//     // 构造右值（如 in 数组 -> List）
//     private String formatQueryParameter(VariableElement param, String operator) {
//         String value = param.getSimpleName().toString();
//         if ("in".equals(operator) && param.asType().getKind() == TypeKind.ARRAY) {
//             return "Arrays.asList(" + value + ")";
//         }
//         return value;
//     }
//
//     // 写方法参数列表
//     private void writeMethodParameters(Writer writer, List<? extends VariableElement> params) throws IOException {
//         boolean first = true;
//         for (VariableElement param : params) {
//             if (!first) writer.write(", ");
//             writer.write(param.asType() + " " + param.getSimpleName());
//             first = false;
//         }
//     }
//
//     // 字段前缀最长匹配
//     private String matchEntityFieldByPrefix(TypeElement entityType, String param) {
//         String camelPath = decapitalize(param);
//         for (int len = camelPath.length(); len > 0; len--) {
//             String candidate = camelPath.substring(0, len);
//             if (entityHasField(entityType, candidate)) return candidate;
//         }
//         return null;
//     }
//
//     // 判断字段是否存在
//     private boolean entityHasField(TypeElement entityType, String fieldName) {
//         for (Element enclosed : entityType.getEnclosedElements()) {
//             if (enclosed.getKind() == ElementKind.FIELD && enclosed.getSimpleName().toString().equals(fieldName)) {
//                 return true;
//             }
//         }
//         return false;
//     }
//
//     // 首字母小写
//     private String decapitalize(String s) {
//         if (s == null || s.isEmpty()) return s;
//         return Character.toLowerCase(s.charAt(0)) + s.substring(1);
//     }
//
//     // 查找字段
//     private VariableElement findField(TypeElement type, String fieldName) {
//         for (Element enclosed : type.getEnclosedElements()) {
//             if (enclosed.getKind() == ElementKind.FIELD && enclosed.getSimpleName().contentEquals(fieldName)) {
//                 return (VariableElement) enclosed;
//             }
//         }
//         return null;
//     }
//
//     // 条件字段结构
//     private static class ConditionStructure {
//         final String fieldName;
//         final String operator;
//         ConditionStructure(String fieldName, String operator) {
//             this.fieldName = fieldName;
//             this.operator = operator;
//         }
//     }
// }
