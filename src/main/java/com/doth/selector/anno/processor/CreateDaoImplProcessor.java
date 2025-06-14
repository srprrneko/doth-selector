// package com.doth.selector.anno.processor;
//
// import com.doth.selector.anno.CreateDaoImpl;
// import com.google.auto.service.AutoService;
//
// import javax.annotation.processing.*;
// import javax.lang.model.SourceVersion;
// import javax.lang.model.element.*;
// import javax.lang.model.util.Elements;
// import javax.lang.model.util.Types;
// import javax.tools.Diagnostic;
// import java.util.Set;
//
// import com.doth.selector.anno.processor.core.ImplClassWriter;
//
// @AutoService(Processor.class)
// @SupportedAnnotationTypes("com.doth.selector.anno.CreateDaoImpl")
// public class CreateDaoImplProcessor extends AbstractProcessor {
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
//     // 初始化注解处理器上下文环境
//     @Override
//     public synchronized void init(ProcessingEnvironment env) {
//         super.init(env);
//         this.filer = env.getFiler();
//         this.messager = env.getMessager();
//         this.typeUtils = env.getTypeUtils();
//         this.elementUtils = env.getElementUtils();
//     }
//
//     // 注解处理器主入口，处理 @CreateDaoImpl 注解
//     @Override
//     public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
//         for (Element element : roundEnv.getElementsAnnotatedWith(CreateDaoImpl.class)) {
//             if (element.getKind() != ElementKind.CLASS || !element.getModifiers().contains(Modifier.ABSTRACT)) {
//                 messager.printMessage(Diagnostic.Kind.ERROR, "继承 Selector 请使用抽象类!!", element);
//                 continue;
//             }
//
//             ImplClassWriter writer = new ImplClassWriter(
//                 (TypeElement) element, filer, messager, typeUtils, elementUtils
//             );
//             writer.generate(); // 生成实现类
//         }
//         return true;
//     }
// }
