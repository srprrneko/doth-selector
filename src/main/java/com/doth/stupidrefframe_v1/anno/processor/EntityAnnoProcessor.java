package com.doth.stupidrefframe_v1.anno.processor;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.util.Set;

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.doth.stupidrefframe_v1.anno.Entity") // 指定处理的注解
@SupportedSourceVersion(SourceVersion.RELEASE_11) // 根据你的JDK版本调整
public class EntityAnnoProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            // 遍历所有被 @Entity 注解的类
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element.getKind() == ElementKind.CLASS) {
                    TypeElement classElement = (TypeElement) element;
                    checkFields(classElement);
                }
            }
        }
        return true;
    }

    private void checkFields(TypeElement classElement) {
        // 遍历类的所有字段
        for (Element enclosed : classElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosed;
                // 检查字段是否为基本类型
                if (field.asType().getKind().isPrimitive()) {
                    // 直接报编译错误
                    processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        String.format("实体类 %s 的字段 %s 必须使用包装类型！",
                                classElement.getSimpleName(),
                                field.getSimpleName()),
                                field
                        );
                }
            }
        }
    }
}