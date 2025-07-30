package com.doth.selector.anno.processor.core;

import com.doth.selector.anno.Join;
import com.doth.selector.anno.processor.BaseAnnotationProcessor;
import com.google.auto.service.AutoService;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.Set;

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.doth.selector.anno.QueryBean")
public class EntityProcessor extends BaseAnnotationProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
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
        for (Element enclosed : classElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosed;

                // 基本类型检查
                if (field.asType().getKind().isPrimitive()) {
                    context.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            String.format("实体类 %s 的字段 %s 必须使用包装类型！",
                                    classElement.getSimpleName(), field.getSimpleName()),
                            field
                    );
                }

                // 自定义类型但缺少 @Join 检查
                else if (isCustomClass(field.asType()) && !hasJoinAnnotation(field)) {
                    context.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            String.format("实体类 %s 的字段 %s 是自定义类型但缺少@Join注解！",
                                    classElement.getSimpleName(), field.getSimpleName()),
                            field
                    );
                }
            }
        }
    }

    private boolean isCustomClass(TypeMirror typeMirror) {
        if (typeMirror.getKind().isPrimitive() || typeMirror.getKind() == TypeKind.ARRAY) {
            return false;
        }

        if (typeMirror instanceof DeclaredType) {
            Element element = ((DeclaredType) typeMirror).asElement();
            if (element instanceof TypeElement) {
                String className = ((TypeElement) element).getQualifiedName().toString();
                return !className.startsWith("java.");
            }
        }
        return false;
    }

    private boolean hasJoinAnnotation(VariableElement field) {
        // 直接注解在字段上
        if (field.getAnnotation(Join.class) != null) {
            return true;
        }

        // 检查字段类型上的注解
        TypeMirror fieldType = field.asType();
        if (fieldType instanceof DeclaredType) {
            Element element = ((DeclaredType) fieldType).asElement();
            if (element instanceof TypeElement) {
                return ((TypeElement) element).getAnnotation(Join.class) != null;
            }
        }
        return false;
    }
}
