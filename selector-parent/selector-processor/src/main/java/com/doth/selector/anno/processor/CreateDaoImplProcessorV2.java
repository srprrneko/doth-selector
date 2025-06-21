package com.doth.selector.anno.processor;

import com.doth.selector.anno.CreateDaoImpl;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.util.Set;

/**
 * Processor 只负责扫描与校验，将生成逻辑委托给 DaoImplGenerator。
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("com.doth.selector.anno.CreateDaoImpl")
public class CreateDaoImplProcessorV2 extends BaseAnnotationProcessor {

    private DaoImplGenerator generator;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        // 复用同一份 context
        this.generator = new DaoImplGenerator(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(CreateDaoImpl.class)) {
            if (!(element instanceof TypeElement)
             || element.getKind() != ElementKind.CLASS
             || !element.getModifiers().contains(Modifier.ABSTRACT)) {
                context.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "继承 Selector 请使用抽象类!!", element);
                continue;
            }
            generator.generate((TypeElement) element);
        }
        return true;
    }
}
