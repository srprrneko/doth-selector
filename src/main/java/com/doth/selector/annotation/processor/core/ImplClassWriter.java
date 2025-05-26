package com.doth.selector.annotation.processor.core;

import com.doth.selector.annotation.processor.codegena.MethodBodyGenerator;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.Writer;
import java.util.List;

public class ImplClassWriter {

    private final TypeElement abstractClass;
    private final Filer filer;
    private final Messager messager;
    private final Types typeUtils;
    private final Elements elementUtils;

    public ImplClassWriter(TypeElement abstractClass, Filer filer, Messager messager,
                           Types typeUtils, Elements elementUtils) {
        this.abstractClass = abstractClass;
        this.filer = filer;
        this.messager = messager;
        this.typeUtils = typeUtils;
        this.elementUtils = elementUtils;
    }

    // 执行代码生成入口
    public void generate() {
        String className = abstractClass.getSimpleName() + "Impl";
        String packageName = elementUtils.getPackageOf(abstractClass).getQualifiedName().toString();
        TypeElement entityTypeElement = getGenericType(abstractClass);

        try (Writer writer = filer.createSourceFile(packageName + "." + className).openWriter()) {
            writer.write("package " + packageName + ";\n\n");
            writer.write("import java.util.*;\n");
            writer.write("import java.util.function.Consumer;\n\n");
            writer.write("public class " + className + " extends " + abstractClass.getSimpleName() + " {\n");
            writer.write("    public " + className + "() { super(); }\n\n");

            for (Element enclosed : abstractClass.getEnclosedElements()) {
                if (enclosed.getKind() == ElementKind.METHOD &&
                    enclosed.getModifiers().contains(Modifier.ABSTRACT)) {
                    MethodBodyGenerator.generateImpl((ExecutableElement) enclosed, writer, messager, entityTypeElement, typeUtils);
                }
            }
            writer.write("}\n");
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "生成实现类失败: " + e.getMessage());
        }
    }

    // 获取泛型中的实体类型
    private TypeElement getGenericType(TypeElement clazz) {
        TypeMirror superClass = clazz.getSuperclass();
        if (superClass instanceof DeclaredType) {
            List<? extends TypeMirror> typeArgs = ((DeclaredType) superClass).getTypeArguments();
            if (!typeArgs.isEmpty()) {
                return (TypeElement) typeUtils.asElement(typeArgs.get(0));
            }
        }
        messager.printMessage(Diagnostic.Kind.ERROR, "无法获取实体类型", clazz);
        return null;
    }
}
