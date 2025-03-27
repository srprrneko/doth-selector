package com.doth.stupidrefframe_v1.anno.processor;

import com.doth.stupidrefframe_v1.anno.Overload;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@AutoService(Processor.class) // 自动生成 META-INF 配置
@SupportedAnnotationTypes("com.doth.stupidrefframe_v1.anno.Overload") // 指定处理的注解类型
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class OverloadProcessor extends AbstractProcessor {

    private Types typeUtils;
    private Elements elementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Overload.class)) {
            if (element.getKind() != ElementKind.METHOD) {
                error(element, "@Overload 只能用于方法");
                continue;
            }

            ExecutableElement method = (ExecutableElement) element;
            TypeElement classElement = (TypeElement) element.getEnclosingElement();

            // 检查是否存在重载（包括继承链中的方法）
            if (!hasOverloadInHierarchy(method, classElement)) {
                error(element, "方法 '%s' 没有重载版本（包括父类/接口）", method.getSimpleName());
            }
        }
        return true;
    }

    // 递归检查类及其父类/接口中的重载方法
    private boolean hasOverloadInHierarchy(ExecutableElement method, TypeElement classElement) {
        String methodName = method.getSimpleName().toString();
        List<TypeMirror> visited = new ArrayList<>();

        // 递归遍历继承链
        return checkClassHierarchy(classElement, methodName, method, visited);
    }

    private boolean checkClassHierarchy(TypeElement type, String methodName,
                                        ExecutableElement originalMethod, List<TypeMirror> visited) {
        // 避免重复检查同一类型
        if (visited.contains(type.asType())) return false;
        visited.add(type.asType());

        // 检查当前类中的方法
        for (ExecutableElement m : ElementFilter.methodsIn(type.getEnclosedElements())) {
            if (isOverload(m, originalMethod, methodName)) {
                return true;
            }
        }

        // 递归检查父类和接口
        for (TypeMirror superType : typeUtils.directSupertypes(type.asType())) {
            TypeElement superClass = (TypeElement) typeUtils.asElement(superType);
            if (superClass != null && checkClassHierarchy(superClass, methodName, originalMethod, visited)) {
                return true;
            }
        }

        return false;
    }

    // 判断两个方法是否为重载（同名且参数不同）
    private boolean isOverload(ExecutableElement method1, ExecutableElement method2, String targetName) {
        if (!method1.getSimpleName().toString().equals(targetName)) return false;
        if (method1.equals(method2)) return false; // 排除自身

        // 比较参数类型（擦除泛型）
        return !typeUtils.isSameType(
                typeUtils.erasure(method1.asType()),
                typeUtils.erasure(method2.asType())
        );
    }

    // 错误报告工具方法
    private void error(Element e, String msg, Object... args) {
        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                String.format(msg, args),
                e
        );
    }
}