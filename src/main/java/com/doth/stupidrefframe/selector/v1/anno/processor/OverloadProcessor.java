package com.doth.stupidrefframe.selector.v1.anno.processor;

import com.doth.stupidrefframe.selector.v1.anno.Overload;
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



/**
 * 注解处理器：用于验证被@Overload标记的方法是否在类继承链中存在合法重载
 * <p>
 * ▎核心功能
 * 1. 检查@Overload仅用于方法（编译期验证）
 * 2. 递归遍历当前类及其父类/接口的方法，验证是否存在同名但参数类型不同的重载方法
 *
 * @AutoService(Processor.class)          // 自动注册为SPI服务（通过auto-service库生成META-INF配置）[2,5](@ref)
 * @SupportedAnnotationTypes("com.doth...")// 指定处理的注解类型为@Overload
 * @SupportedSourceVersion(RELEASE_11)    // 支持Java 11语法
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("com.doth.stupidrefframe.selector.v1.anno.Overload")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class OverloadProcessor extends AbstractProcessor {
    private Types typeUtils;  // 类型操作工具（用于泛型擦除/继承关系判断）
    private Elements elementUtils; // 元素操作工具（用于获取包/类信息）

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        // 初始化类型和元素工具类（用于后续方法参数和类继承链分析）[4](@ref)
        typeUtils = env.getTypeUtils();
        elementUtils = env.getElementUtils();
    }

    /**
     * ▎注解处理主流程
     * 1. 遍历所有被@Overload标记的元素
     * 2. 验证元素类型必须为方法
     * 3. 检查该方法在继承链中是否存在合法重载
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        for (Element element : env.getElementsAnnotatedWith(Overload.class)) {
            if (element.getKind() != ElementKind.METHOD) { // 非方法则报错[7](@ref)
                error(element, "@Overload 只能用于方法");
                continue;
            }

            // 转换为方法元素并获取所属类
            ExecutableElement method = (ExecutableElement) element;
            TypeElement classElement = (TypeElement) element.getEnclosingElement();

            // 递归检查继承链中的重载方法（包括父类和接口）
            if (!hasOverloadInHierarchy(method, classElement)) {
                error(element, "方法 '%s' 没有重载版本（包括父类/接口）", method.getSimpleName());
            }
        }
        return true; // 已处理所有注解，无需其他处理器介入
    }

    /**
     * ▎递归检查继承链中的重载
     * @param method       被@Overload标记的原方法
     * @param classElement 方法所属的类元素
     * @return true表示存在合法重载，false表示无重载（触发编译错误）
     */
    private boolean hasOverloadInHierarchy(ExecutableElement method, TypeElement classElement) {
        List<TypeMirror> visited = new ArrayList<>(); // 记录已检查类型避免循环
        return checkClassHierarchy(classElement, method.getSimpleName().toString(), method, visited);
    }

    /**
     * ▎层级递归逻辑
     * 1. 检查当前类所有方法
     * 2. 递归检查父类和接口
     */
    private boolean checkClassHierarchy(TypeElement type, String methodName,
                                        ExecutableElement originalMethod, List<TypeMirror> visited) {
        if (visited.contains(type.asType())) return false; // 防止重复检查
        visited.add(type.asType());

        // 检查当前类的所有方法（过滤非目标方法）
        for (ExecutableElement m : ElementFilter.methodsIn(type.getEnclosedElements())) {
            if (isOverload(m, originalMethod, methodName)) return true;
        }

        // 递归检查父类和接口（支持多继承场景）
        for (TypeMirror superType : typeUtils.directSupertypes(type.asType())) {
            TypeElement superClass = (TypeElement) typeUtils.asElement(superType);
            if (superClass != null && checkClassHierarchy(superClass, methodName, originalMethod, visited)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ▎重载判断标准
     * 1. 方法名相同
     * 2. 非同一方法（避免自比较）
     * 3. 参数类型不同（擦除泛型后比较）[4](@ref)
     */
    private boolean isOverload(ExecutableElement m1, ExecutableElement m2, String name) {
        return m1.getSimpleName().toString().equals(name)
                && !m1.equals(m2)
                && !typeUtils.isSameType(
                typeUtils.erasure(m1.asType()),
                typeUtils.erasure(m2.asType()));
    }

    /**
     * ▎错误报告工具
     * 通过ProcessingEnvironment的Messager输出编译错误信息[5](@ref)
     */
    private void error(Element e, String msg, Object... args) {
        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                String.format(msg, args),
                e
        );
    }
}