package com.doth.selector.anno.processor;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import java.util.Set;

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.doth.selector.anno.Entity") // 指定处理的注解
@SupportedSourceVersion(SourceVersion.RELEASE_11) // 根据你的JDK版本调整
public class EntityProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            // 遍历所有被 @Entity 注解的类 > 因为此时的 annotations 只有一个Entity 注解, 所以这里获取的是 Entity 标记的元素
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
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            String.format("实体类 %s 的字段 %s 必须使用包装类型！",
                                    classElement.getSimpleName(),
                                    field.getSimpleName()),
                            field
                    );
                }
                // 检查字段是否为自定义类且没有@Join注解
                else if (isCustomClass(field.asType()) && !hasJoinAnnotation(field)) {
                    processingEnv.getMessager().printMessage(
                            Diagnostic.Kind.ERROR,
                            String.format("实体类 %s 的字段 %s 是自定义类型但缺少@Join注解！",
                                    classElement.getSimpleName(),
                                    field.getSimpleName()),
                            field
                    );
                }
            }
        }
    }

    /**
     * 判断字段类型是否为自定义类
     */
    private boolean isCustomClass(TypeMirror typeMirror) {
        // 基本类型和数组类型直接返回false
        if (typeMirror.getKind().isPrimitive() || typeMirror.getKind() == TypeKind.ARRAY) {
            return false;
        }

        // 检查是否为Java标准库中的类
        if (typeMirror instanceof DeclaredType) {
            Element element = ((DeclaredType) typeMirror).asElement();
            if (element instanceof TypeElement) {
                String className = ((TypeElement) element).getQualifiedName().toString();
                // 简单判断：如果包名以"java."开头，则认为是JDK类
                return !className.startsWith("java.");
            }
        }
        return false;
    }

    /**
     * 判断字段是否有@Join注解
     */
    private boolean hasJoinAnnotation(VariableElement field) {
        // 检查字段上是否有@Join注解
        if (field.getAnnotationMirrors().stream()
                .anyMatch(anno -> anno.getAnnotationType().toString().equals("com.doth.selector.anno.Join"))) {
            return true;
        }

        // 检查字段类型上是否有@Join注解
        TypeMirror fieldType = field.asType();
        if (fieldType instanceof DeclaredType) {
            Element element = ((DeclaredType) fieldType).asElement();
            if (element instanceof TypeElement) {
                return ((TypeElement) element).getAnnotationMirrors().stream()
                        .anyMatch(anno -> anno.getAnnotationType().toString().equals("com.doth.selector.anno.Join"));
            }
        }
        return false;
    }
}