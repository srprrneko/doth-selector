package com.doth.selector.anno.processor;

import com.doth.selector.anno.DTOConstructor;
import com.doth.selector.common.util.NamingConvertUtil;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;
import java.io.Writer;
import java.util.Set;
import java.util.stream.Collectors;

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.doth.selector.anno.DTOConstructor")
public class DTOConstructorProcessor extends AbstractProcessor {

    private Filer filer;
    private Elements elementUtils;
    private Types typeUtils;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(DTOConstructor.class)) {
            if (element.getKind() != ElementKind.CONSTRUCTOR) continue;

            ExecutableElement constructor = (ExecutableElement) element;
            TypeElement enclosingClass = (TypeElement) constructor.getEnclosingElement();

            String dtoId = constructor.getAnnotation(DTOConstructor.class).id();
            boolean autoClzName = constructor.getAnnotation(DTOConstructor.class).isAutoClzName();
            String dtoClassName = enclosingClass.getSimpleName() + "$" + dtoId + "DTO";
            if (!autoClzName) {
                dtoClassName = NamingConvertUtil.toUpperCaseFirstLetter(dtoId, true);
            }
            String pkg = elementUtils.getPackageOf(enclosingClass).getQualifiedName().toString();

            try {
                JavaFileObject file = filer.createSourceFile(pkg + "." + dtoClassName);
                try (Writer writer = file.openWriter()) {
                    writer.write("package " + pkg + ";\n\n");
                    writer.write("import com.doth.selector.dto.DTOFactory;\n\n");
                    writer.write("public class " + dtoClassName + " extends " + enclosingClass.getQualifiedName() + " {\n");

                    String params = constructor.getParameters().stream()
                            .map(v -> v.asType() + " " + v.getSimpleName())
                            .collect(Collectors.joining(", "));

                    String args = constructor.getParameters().stream()
                            .map(v -> v.getSimpleName().toString())
                            .collect(Collectors.joining(", "));

                    writer.write("    public " + dtoClassName + "() { super(); }\n");
                    writer.write("    public " + dtoClassName + "(" + params + ") {\n");
                    writer.write("        super(" + args + ");\n");
                    writer.write("    }\n\n");



                    // 静态注册逻辑
                    writer.write("    static {\n");
                    writer.write("        DTOFactory.register(" + enclosingClass.getQualifiedName() + ".class, \"" + dtoId + "\", " + dtoClassName + ".class);\n");
                    writer.write("    }\n");

                    writer.write("}\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}
