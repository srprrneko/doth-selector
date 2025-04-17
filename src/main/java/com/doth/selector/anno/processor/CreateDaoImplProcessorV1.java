// package com.doth.selector.anno.processor;
//
// import com.doth.selector.anno.CreateDaoImpl;
// import com.doth.selector.anno.Join;
// import com.google.auto.service.AutoService;
//
// import javax.annotation.processing.*;
// import javax.lang.model.SourceVersion;
// import javax.lang.model.element.*;
// import javax.lang.model.type.DeclaredType;
// import javax.lang.model.type.TypeMirror;
// import javax.lang.model.util.Elements;
// import javax.lang.model.util.Types;
// import javax.tools.Diagnostic;
// import java.io.IOException;
// import java.io.Writer;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.Set;
// import java.util.concurrent.atomic.AtomicInteger;
//
//
// /**
//  * 动态生成DAO实现类的注解处理器
//  *
//  * 功能说明:
//  * 1. 处理被 @CreateDaoImpl 标记的抽象类
//  * 2. 自动生成包含具体方法实现的子类（类名+Impl）
//  * 3. 自动实现以"queryBy"开头的查询方法, 以 With 或 Vz 进行条件分割, 例如: queryByNameVzDepartmentId()
//  * 4. 通过 @Join 注解处理关联字段的路径解析
//  *
//  * 处理流程:
//  * 1. 扫描所有被 @CreateDaoImpl 标记的类元素
//  * 2. 验证目标类是否符合抽象类要求
//  * 3. 解析父类泛型获取实体类型信息
//  * 4. 生成包含具体方法实现的子类
//  */
// @AutoService(Processor.class)
// @SupportedAnnotationTypes("com.doth.selector.anno.CreateDaoImpl")
// // @SupportedSourceVersion(SourceVersion.RELEASE_11)
// public class CreateDaoImplProcessorV1 extends AbstractProcessor {
//
//
//     // 注解处理工具类实例
//     private Filer filer;        // 文件生成器
//     private Messager messager;  // 消息报告器
//     private Types typeUtils;    // 类型操作工具
//     private Elements elementUtils; // 元素操作工具
//
//
//
//     @Override
//     public SourceVersion getSupportedSourceVersion() {
//         // 动态匹配当前JDK支持的最高版本
//         return SourceVersion.latestSupported();
//     }
//
//
//     // 初始化处理器环境
//     @Override
//     public synchronized void init(ProcessingEnvironment env) {
//         super.init(env);
//         filer = env.getFiler();
//         messager = env.getMessager();
//         typeUtils = env.getTypeUtils();
//         elementUtils = env.getElementUtils();
//     }
//
//    /**
//     * 主处理方法
//     *
//     * 处理流程：
//     * 1. 扫描所有带 @CreateDaoImpl 注解的类
//     * 2. 检查是否为抽象类（必要条件）
//     * 3. 对符合条件的类生成Impl实现类
//     */
//     @Override
//     public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
//
//         for (Element element : roundEnv.getElementsAnnotatedWith(CreateDaoImpl.class)) {
//             // 元素类型必须是类且包含abstract修饰符
//             if (element.getKind() != ElementKind.CLASS || !((TypeElement) element).getModifiers().contains(Modifier.ABSTRACT)) {
//                 // 向编译器报告错误
//                 messager.printMessage(Diagnostic.Kind.ERROR, "继承 Selector 请使用抽象类!!", element);
//                 continue;
//             }
//             // 验证通过后进入生成流程
//             writeImplClz((TypeElement) element);
//         }
//         // 表示已处理这些注解
//         return true;
//     }
//
//
//
//
//     /**
//      * 生成空实现类骨架 - 核心步骤分解
//      *
//      * 实现步骤
//      * 1. 确定新类名 (原类名+Impl)
//      * 2. 提取包路径和实体类型信息
//      * 3. 创建类文件并写入基础结构
//      * 4. 处理所有抽象方法
//      * @param abstractClass 被处理的抽象类元素
//      */
//     private void writeImplClz(TypeElement abstractClass) {
//
//         // 默认名  dao + impl
//         String className = abstractClass.getSimpleName() + "Impl";
//
//         // 获取父类所在包名
//         //                                                   完全限定名（包含包路径的全名）
//         String packageName = elementUtils.getPackageOf(abstractClass).getQualifiedName().toString();
//
//         // 解析父类泛型获取实体类型
//         TypeElement entityTypeElement = this.getGeneric(abstractClass);
//
//         // 创建类文件
//         try (Writer writer = filer.createSourceFile(packageName + "." + className).openWriter()) {
//             // 声明类前 (包, 导入工具类)
//             writer.write("package " + packageName + ";\n\n");
//             writer.write("import java.util.LinkedHashMap;\n\n");
//
//             // 类声明（继承原抽象类）
//             writer.write("public class " + className + " extends " + abstractClass.getSimpleName() + " {\n");
//
//
//             // 生成无参构造, 调用super确保父类正确初始化
//             writer.write("    public " + className + "() {\n");
//             writer.write("        super();\n");
//             writer.write("    }\n\n");
//
//             // 获取类中所有元素
//             for (Element enclosedElement : abstractClass.getEnclosedElements()) {
//                 // 仅处理抽象方法
//                 if (enclosedElement.getKind() == ElementKind.METHOD && enclosedElement.getModifiers().contains(Modifier.ABSTRACT)) {
//                     // 强转成可执行元素(方法)
//                     ExecutableElement method = (ExecutableElement) enclosedElement;
//
//                     writeImplMethod(writer, method, entityTypeElement); // 一个方法一个方法的画
//                 }
//             }
//
//             writer.write("}\n");
//         } catch (Exception e) {
//             messager.printMessage(Diagnostic.Kind.ERROR, "生成实现类失败: " + e.getMessage());
//         }
//     }
//
//     /**
//      * 解析父类翻新
//      * @param abstractClz (约定) 抽象父类
//      * @return 实体类型
//      */
//     private TypeElement getGeneric(TypeElement abstractClz) {
//         CreateDaoImpl anno = abstractClz.getAnnotation(CreateDaoImpl.class);
//
//         TypeMirror superClass = abstractClz.getSuperclass(); // 获取镜像类型
//
//         if (superClass instanceof DeclaredType) { // 是否是类型
//
//             List<? extends TypeMirror> typeArgs = ((DeclaredType) superClass).getTypeArguments(); // 获取所有泛型参数
//
//             // 将类型镜像转换为类元素 -> User.class对应的元素
//             if (!typeArgs.isEmpty()) {
//                 TypeMirror entityType = typeArgs.get(0);
//                 return (TypeElement) typeUtils.asElement(entityType);
//             }
//             messager.printMessage(Diagnostic.Kind.ERROR, "被 @" +anno.getClass().getSimpleName() + " 标记的类未对 Selector 指定泛型",
//                     abstractClz);
//             return null;
//         }
//         messager.printMessage(Diagnostic.Kind.ERROR, "无法获取实体类型", abstractClz);
//         return null;
//     }
//
//
//
//     /**
//      * 分割字符串条件
//      * @param condParts 代表条件的字符串
//      * @return 转载着条件的 list
//      */
//     private List<String> splitWithPara(String condParts) {
//         List<String> parts = new ArrayList<>();
//         String[] splits = condParts.split("With|Vz|And"); // 提供三种条件'与'单词
//
//         for (String part : splits)
//             if (!part.isEmpty())
//                 parts.add(part);
//
//         return parts;
//     }
//
//     private void writePara4Method(Writer writer, List<? extends VariableElement> params) throws IOException {
//         // 是否第一个参数, 用于动态拼接','
//         boolean isFirst = true;
//
//         for (VariableElement param : params) {
//             if (!isFirst) writer.write(", ");
//             writer.write(param.asType() + " " + param.getSimpleName()); // 类型 + 参数名
//             isFirst = false;
//         }
//     }
//
//     // private void writeErr
//
//     /**
//      * 核心:画出实现方法
//      * @param writer 用于输出代码
//      * @param method 需要生成实现的方法
//      * @param entityType 关联实体
//      * @throws Exception 异常
//      */
//     private void writeImplMethod(Writer writer, ExecutableElement method, TypeElement entityType) throws Exception {
//         // 获取方法名, 准备进行拆分解析
//         String methodName = method.getSimpleName().toString();
//
//         // 方法体
//         writer.write("    @Override\n");
//         writer.write("    public " + method.getReturnType() + " " + methodName + "("); // 全限定返回值类型 + 方法名
//
//         // 画参数
//         List<? extends VariableElement> params = method.getParameters(); // 当前方法 的参数列表
//
//         // for (VariableElement param : params) {
//         //     if (!isFirst) writer.write(", ");
//         //     writer.write(param.asType() + " " + param.getSimpleName()); // 类型 + 参数名
//         //     isFirst = false;
//         // }
//
//         writePara4Method(writer, params);
//
//         writer.write(") {\n"); // 闭合参数体, 开始方法体
//
//         // 处理约定查询名..
//         if (methodName.startsWith("queryBy")) {
//             // 截取 条件部分 ..With..
//             String condParts = methodName.substring("queryBy".length()); // 例: NameWithAgeWith...
//
//             List<String> condParams = this.splitWithPara(condParts); // 参数: name, age...
//
//             if (params.size() != condParams.size()) { // 校验参数与 方法条件段 是否匹配
//                 messager.printMessage(Diagnostic.Kind.ERROR,
//                         "参数数量与条件数量不匹配 期望: " + condParams.size() + " 实际: " + params.size(),
//                         method);
//                 // 补上默认值
//                 writer.write("        return \"0\";\n");
//                 writer.write("    }\n\n");
//                 return;
//             }
//
//             // 准备条件集
//             writer.write("        LinkedHashMap<String, Object> cond = new LinkedHashMap<>();\n");
//
//             // 遍历处理每一行 put(k,v)
//             // 1.处理k
//             // 2.处理v
//             for (int i = 0; i < condParams.size(); i++) {
//
//                 String value = params.get(i).getSimpleName().toString(); // 获取v
//
//                 String param = condParams.get(i); // 例: DepartmentName, 用于生成报告, 对解析参数进行传参
//
//                 // 解析参数 -> DepartmentOfficeName -> t2.name
//                 String key = cvnParam2K4Map(entityType, param); // 解析当前参数 tn.property
//
//                 if (key == null) {
//                     writer.write("        // 当前条件参数解析时遇到异常: " + param + "\n");
//                     continue;
//                 }
//
//                 writer.write("        cond.put(\"" + key + "\", " + value + ");\n");
//             }
//
//             writer.write("        return dct$().query2Lst(cond);\n");
//
//         } else { // 空实现
//             if (method.getReturnType().getKind().isPrimitive()) {
//                 writer.write("        return " + getDefaultPrimitiveValue(method.getReturnType().toString()) + ";\n");
//             } else {
//                 writer.write("        return null;\n");
//             }
//         }
//
//         // 收尾
//         writer.write("    }\n\n");
//     }
//
//
//     /**
//      * 解析表别名 入口, 调用方implMethod
//      * @param entityType 实体对象
//      * @param param 条件参数
//      * @return 字符串
//      */
//     private String cvnParam2K4Map(TypeElement entityType, String param) {
//         return resolveColumnKeyRecursive(
//                 entityType,
//                 param,
//                 "t0", // 从主表出发
//                 new AtomicInteger(0) // 使用原子类管理索引
//         );
//     }
//
//     private String resolveColumnKeyRecursive(
//             TypeElement currentEntity, // 当前层级的实体
//             String param, // 当前条件参数: DepartmentName
//             String entyLvAlia, // 当前层级的别名（如 t0）
//             AtomicInteger index
//     ) {
//         // 1. 查找最长匹配字段
//         String fieldName = findLongestMatchingField(currentEntity, param);
//         if (fieldName == null) return null;
//
//         // 2. 获取字段元素
//         VariableElement field = findField(currentEntity, fieldName);
//         if (field == null) return null;
//
//         // 两种处理策略: 核心 -> 一直走if 找下一张表, 直到else
//         // a.关联字段特殊处理
//         if (field.getAnnotation(Join.class) != null) {
//             // 准备下一轮别名, 直到走else
//             String newAlias = "t" + index.incrementAndGet();
//             TypeElement nestedEntity = (TypeElement) typeUtils.asElement(field.asType());
//
//             // 递归处理剩余路径（使用新别名）
//             String nextPath = param.substring(fieldName.length());
//             // 关键点：传递新别名给下一层
//
//             return resolveColumnKeyRecursive(
//                     nestedEntity,
//                     nextPath,
//                     newAlias, // 传递新别名给下一层
//                     index
//             ); // 直接返回子层路径，不拼接当前层别名
//         }
//
//         // b.普通字段直接处理
//         String nextPath = param.substring(fieldName.length());
//         if (!nextPath.isEmpty()) {
//             messager.printMessage(Diagnostic.Kind.ERROR, "字段: '" + fieldName + "' 无法继续解析", field);
//             return null;
//         }
//         return entyLvAlia + "." + fieldName; // t0.property
//     }
//
//     private String findLongestMatchingField(TypeElement entityType, String param) {
//         String camelPath = toCamelCase(param); // 转换成小驼峰, 匹配类字段
//
//         for (int len = camelPath.length(); len > 0; len--) {
//             String candidate = camelPath.substring(0, len);
//
//             // 去实体中查找是否存在当前字段
//             if (hasField(entityType, candidate)) {
//                 // 找到则返回
//                 return candidate;
//             }
//         }
//         return null;
//     }
//
//     private boolean hasField(TypeElement entityType, String fieldName) {
//         for (Element enclosed : entityType.getEnclosedElements()) {
//             if (enclosed.getKind() == ElementKind.FIELD && enclosed.getSimpleName().toString().equals(fieldName)) {
//                 return true;
//             }
//         }
//         return false;
//     }
//
//     private String toCamelCase(String s) {
//         if (s == null || s.isEmpty()) return s;
//         return Character.toLowerCase(s.charAt(0)) + s.substring(1);
//     }
//
//     /** 查找类中的字段元素
//      *
//      * @param type      要查找的类元素（如User.class）
//      * @param fieldName 目标字段名（如"name"）
//      * @return 找到的字段元素，未找到返回null
//      *
//      * 实现逻辑：
//      * 1. 遍历类的所有成员元素
//      * 2. 筛选字段类型元素（ElementKind.FIELD）
//      * 3. 匹配字段名称
//      *
//      * 示例：
//      * User类结构：
//      * public class User {
//      *     private String name; // ← 会被匹配到
//      *     public void getName() {...} // ← 方法不匹配
//      * }
//      */
//     private VariableElement findField(TypeElement type, String fieldName) {
//         for (Element enclosed : type.getEnclosedElements()) {
//             if (enclosed.getKind() == ElementKind.FIELD && enclosed.getSimpleName().contentEquals(fieldName)) {
//                 return (VariableElement) enclosed;
//             }
//         }
//         return null;
//     }
//
//     private String getDefaultPrimitiveValue(String type) {
//         switch (type) {
//             case "boolean": return "false"; // 布尔类型默认值
//             case "int":     return "0";     // 整型默认
//             case "long":    return "0L";    // 长整型需要L后缀
//             case "double":  return "0.0";   // 浮点型带小数点
//             default:        return "0";     // 其他数值类型（short/byte等）
//         }
//     }
// }
