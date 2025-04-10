// package com.doth.stupidrefframe.anno.processor;
//
// import com.doth.stupidrefframe.anno.CreateDaoImpl;
// import com.google.auto.service.AutoService;
//
// import javax.annotation.processing.*;
// import javax.lang.model.SourceVersion;
// import javax.lang.model.element.*;
// import javax.tools.Diagnostic;
// import java.io.Writer;
// import java.util.Set;
//
// @AutoService(Processor.class) // 自动注册注解处理器
// @SupportedAnnotationTypes("com.doth.stupidrefframe.anno.CreateDaoImpl") // 处理的自定义注解
// @SupportedSourceVersion(SourceVersion.RELEASE_11)
// public class CreateDaoImplProcessor extends AbstractProcessor {
//
//     private Filer filer; // 用于生成源代码文件
//     private Messager messager; // 用于获取编译信息
//
//     @Override
//     public synchronized void init(ProcessingEnvironment env) {
//         super.init(env);
//         filer = env.getFiler();       // 获取文件生成器
//         messager = env.getMessager(); // 获取编译日志工具
//     }
//
//     @Override
//     public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
//         for (Element element : roundEnv.getElementsAnnotatedWith(CreateDaoImpl.class)) {
//             // 1. 检查是否为抽象类
//             if (element.getKind() != ElementKind.CLASS || !((TypeElement) element).getModifiers().contains(Modifier.ABSTRACT)) {
//                 messager.printMessage(Diagnostic.Kind.ERROR, "继承查询门面类请使用抽象类", element);
//                 continue;
//             }
//
//             // 2. 生成实现类
//             generateEmptyImplementation((TypeElement) element);
//         }
//         return true;
//     }
//
//     private void generateEmptyImplementation(TypeElement abstractClass) {
//         String className = abstractClass.getSimpleName() + "Impl"; // 生成类名为原类名 + Impl
//         String packageName = processingEnv.getElementUtils().getPackageOf(abstractClass).getQualifiedName().toString();
//
//         try (Writer writer = filer.createSourceFile(packageName + "." + className).openWriter()) {
//             // 3. 生成类代码
//             writer.write("package " + packageName + ";\n\n");
//             writer.write("public class " + className + " extends " + abstractClass.getSimpleName() + " {\n");
//
//             // 4. 遍历抽象方法并生成空实现
//             for (Element enclosedElement : abstractClass.getEnclosedElements()) {
//                 if (enclosedElement.getKind() == ElementKind.METHOD
//                         && enclosedElement.getModifiers().contains(Modifier.ABSTRACT)) {
//                     ExecutableElement method = (ExecutableElement) enclosedElement;
//                     generateMethodImpl(writer, method);
//                 }
//             }
//
//             writer.write("}\n");
//         } catch (Exception e) {
//             messager.printMessage(Diagnostic.Kind.ERROR, "生成实现类失败: " + e.getMessage());
//         }
//     }
//
//     private void generateMethodImpl(Writer writer, ExecutableElement method) throws Exception {
//         // 生成方法签名
//         writer.write("    @Override\n");
//         writer.write("    public " + method.getReturnType() + " " + method.getSimpleName() + "(");
//
//         // 生成参数列表
//         boolean firstParam = true;
//         for (VariableElement param : method.getParameters()) {
//             if (!firstParam) writer.write(", ");
//             writer.write(param.asType() + " " + param.getSimpleName());
//             firstParam = false;
//         }
//         writer.write(") {\n");
//
//         // 生成空实现（根据返回类型处理）
//         if (method.getReturnType().getKind().isPrimitive()) {
//             String returnValue = getDefaultPrimitiveValue(method.getReturnType().toString());
//             writer.write("        return " + returnValue + ";\n");
//         } else {
//             writer.write("        return null;\n");
//         }
//         writer.write("    }\n\n");
//     }
//
//     private String getDefaultPrimitiveValue(String type) {
//         switch (type) {
//             case "boolean": return "false";
//             case "int":     return "0";
//             case "long":    return "0L";
//             case "double":  return "0.0";
//             default:        return "0"; // char/short/byte 等默认返回 0
//         }
//     }
// }