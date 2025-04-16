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
// import java.io.Writer;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.Set;
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
// public class CreateDaoImplProcessor extends AbstractProcessor {
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
//             generateEmptyImplementation((TypeElement) element);
//         }
//         // 表示已处理这些注解
//         return true;
//     }
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
//     private void generateEmptyImplementation(TypeElement abstractClass) {
//         // 新类名规则：原抽象类名+Impl
//         String className = abstractClass.getSimpleName() + "Impl";
//
//         // 获取父类所在包名
//         //                                                   完全限定名（包含包路径的全名）
//         String packageName = elementUtils.getPackageOf(abstractClass).getQualifiedName().toString();
//
//         // 解析父类泛型获取实体类型, 例: UserDao<User> -> User）
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
//             // 生成无参构造函数
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
//                     implMethod(writer, method, entityTypeElement);
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
//      * 解析父类泛型参数
//      *
//      * 实现逻辑：
//      * 1. 获取父类类型（如BaseDao<User>）
//      * 2. 提取泛型参数列表
//      * 3. 返回第一个泛型参数对应的元素（实体类）
//      */
//     private TypeElement getGeneric(TypeElement abstractClz) {
//         CreateDaoImpl anno = abstractClz.getAnnotation(CreateDaoImpl.class);
//         TypeMirror superClass = abstractClz.getSuperclass();
//
//         if (superClass instanceof DeclaredType) { // 是否是泛型
//
//             List<? extends TypeMirror> typeArgs = ((DeclaredType) superClass).getTypeArguments(); // 获取所有泛型参数
//
//             // 将类型镜像转换为类元素 -> User.class对应的元素
//             if (!typeArgs.isEmpty()) {
//                 TypeMirror entityType = typeArgs.get(0);
//                 return (TypeElement) typeUtils.asElement(entityType);
//             }
//             messager.printMessage(Diagnostic.Kind.ERROR, "被 " +anno.getClass().getSimpleName() + " 标记的父类未对 Selector 指定泛型",
//                     abstractClz);
//             return null;
//         }
//         messager.printMessage(Diagnostic.Kind.ERROR, "无法获取实体类型", abstractClz);
//         return null;
//     }
//
//     /**
//      * 生成具体方法实现 - 核心逻辑分解
//      *
//      * 处理策略：
//      * 1. 识别queryBy开头的方法进行特殊处理
//      * 2. 普通抽象方法生成默认返回值
//      * 3. 参数数量与条件段数匹配校验
//      */
//     private void implMethod(Writer writer, ExecutableElement method, TypeElement entityType) throws Exception {
//         String methodName = method.getSimpleName().toString();
//         // 方法签名生成
//         writer.write("    @Override\n");
//         writer.write("    public " + method.getReturnType() + " " + methodName + "(");
//
//         // 方法参数填充
//         // 处理参数 -> 获取方法参数列表
//         List<? extends VariableElement> params = method.getParameters();
//         // 是否要拼接','
//         boolean isFirst = true;
//
//         for (VariableElement param : params) {
//             if (!isFirst) writer.write(", ");
//             writer.write(param.asType() + " " + param.getSimpleName()); // 类型+参数名
//             isFirst = false;
//         }
//         writer.write(") {\n");
//
//         // 查询方法处理
//         if (methodName.startsWith("queryBy")) {
//             String byPart = methodName.substring("queryBy".length()); // queryByName -> name
//             List<String> condStr = this.split2Conds(byPart);
//
//             if (params.size() != condStr.size()) { // 方法获得的参数 是否等同于 方法名声明的参数
//                 messager.printMessage(Diagnostic.Kind.ERROR,
//                         "参数数量与条件数量不匹配 期望: " + condStr.size() + " 实际: " + params.size(),
//                         method);
//                 writer.write("        return null;\n");
//                 writer.write("    }\n\n");
//                 return;
//             }
//
//             // 准备条件
//             writer.write("        LinkedHashMap<String, Object> cond = new LinkedHashMap<>();\n");
//
//             // 循环处理put部分
//             for (int i = 0; i < condStr.size(); i++) {
//                 String conditionPart = condStr.get(i);
//                 // 驼峰转字段路径（如 UserName -> user.name）
//                 String propertyPath = camelToPath(conditionPart);
//                 // 解析字段对应的数据库列（处理关联注解）
//                 String columnKey = resolveColumnKey(entityType, propertyPath);
//                 String paramName = params.get(i).getSimpleName().toString();
//
//                 if (columnKey == null) {
//                     writer.write("        // 无效字段路径: " + propertyPath + "\n");
//                     continue;
//                 }
//
//                 // 添加条件到映射表（示例：cond.put("t0.name", name);）
//                 writer.write("        cond.put(\"" + columnKey + "\", " + paramName + ");\n");
//             }
//
//             // 调用父类查询方法
//             writer.write("        return dct$().query2Lst(cond);\n");
//         } else {
//             // 非查询方法默认实现
//             if (method.getReturnType().getKind().isPrimitive()) {
//                 writer.write("        return " + getDefaultPrimitiveValue(method.getReturnType().toString()) + ";\n");
//             } else {
//                 writer.write("        return null;\n");
//             }
//         }
//         writer.write("    }\n\n");
//     }
//
//     /**
//      * 分割字符串条件
//      * @param condParts 代表条件的字符串
//      * @return 转载着条件的 list
//      */
//     private List<String> split2Conds(String condParts) {
//         List<String> parts = new ArrayList<>();
//         String[] splits = condParts.split("With|Vz|And"); // 提供三种条件与单词
//
//         for (String part : splits)
//             if (!part.isEmpty())
//                 parts.add(part);
//
//         return parts;
//     }
//
//    /**
//     * 驼峰命名转路径表达式
//     *
//     * @param camel 驼峰式字符串（如"UserName"）
//     * @return 路径表达式（如"user.name"）
//     *
//     * 转换规则：
//     * 1. 遍历每个字符
//     * 2. 遇到大写字母：
//     *    - 非时添加点分隔符
//     *       - 再转为小写字母
//     * 3. 示例：
//     *    "UserName" → "user.name"
//     */
//     private String camelToPath(String camel) {
//         StringBuilder path = new StringBuilder();
//         for (int i = 0; i < camel.length(); i++) {
//             char c = camel.charAt(i);
//             // 处理大写字母（标识新单词开始）
//             if (Character.isUpperCase(c)) {
//                 // 非首字母时添加分隔符
//                 if (i != 0) path.append('.');
//                 path.append(Character.toLowerCase(c));
//             } else {
//                 // 小写字母直接追加
//                 path.append(c);
//             }
//         }
//         return path.toString();
//     }
//
//     /**
//      * 解析属性路径为数据库列键
//      *
//      * @param entityType    实体类型元素（如User.class）
//      * @param propertyPath  属性路径（如"department.name"）
//      * @return 数据库列键表达式（如"t1.name"）
//      *
//      * 处理逻辑：
//      * 1. 按点号分割路径（示例："department.name" → ["department", "name"]）
//      * 2. 遍历每个路径段：
//      *    - 查找当前实体中的字段
//      *    - 遇到@Join注解字段时：
//      *       a. 递增别名计数器（t0→t1）
//      *       b. 切换当前实体到关联类型
//      *    - 普通字段直接拼接别名路径
//      * 3. 示例：
//      *    输入：User实体, "department.name"
//      *    步骤：
//      *    - 解析department字段（带@Join）
//      *       → 别名变为t1，当前实体变为Department
//      *    - 解析name字段 → t1.name
//      */
//     private String resolveColumnKey(TypeElement entityType, String propertyPath) {
//         // 分割路径为层级结构
//         String[] parts = propertyPath.split("\\.");
//         String currentAlias = "t0"; // 主表别名
//         TypeElement currentEntity = entityType;
//         int index = 0; // 别名索引
//
//         for (String part : parts) {
//             // 查找当前实体的字段元素
//             VariableElement field = findField(currentEntity, part);
//             if (field == null) {
//                 // 报告字段不存在
//                 messager.printMessage(Diagnostic.Kind.ERROR, "无法找到字段: " + part + " 在实体 " + currentEntity.getSimpleName());
//                 return null;
//             }
//
//             // 处理关联注解
//             if (field.getAnnotation(Join.class) != null) {
//                 // 遇到关联字段，递增别名索引
//                 index++;
//                 currentAlias = "t" + index;
//                 // 获取关联实体类型（如User.department→Department）
//                 TypeMirror fieldType = field.asType();
//                 currentEntity = (TypeElement) typeUtils.asElement(fieldType);
//             } else {
//                 // 普通字段直接拼接
//                 currentAlias += "." + part;
//             }
//         }
//
//         return currentAlias;
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
