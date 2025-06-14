package com.doth.selector.common.temp.annoprocessor;

import com.squareup.javapoet.*;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 生成 XxxImpl 类，职责集中于方法生成，辅助逻辑由 DaoImplGeneratorHelper 提供支持
 */
public class DaoImplGenerator {
    private final ProcessingContext ctx;
    private final DaoImplGeneratorHelper helper;
    private static final List<String> QUERY_PREFIXES = Arrays.asList("queryBy", "getBy", "listBy");

    public DaoImplGenerator(ProcessingContext ctx) {
        this.ctx = ctx;
        this.helper = new DaoImplGeneratorHelper(ctx);
    }

    public void generate(TypeElement abstractClass) {
        String pkg = ctx.getElementUtils().getPackageOf(abstractClass)
                .getQualifiedName().toString();
        String implName = abstractClass.getSimpleName() + "Impl";

        TypeSpec.Builder clsB = TypeSpec.classBuilder(implName)
                .addModifiers(Modifier.PUBLIC)
                .superclass(TypeName.get(abstractClass.asType()));

        clsB.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("super()")
                .build());

        TypeElement entityType = helper.extractGenericEntityType(abstractClass);

        for (Element e : abstractClass.getEnclosedElements()) {
            if (e.getKind() == ElementKind.METHOD && e.getModifiers().contains(Modifier.ABSTRACT)) {
                clsB.addMethod(buildMethod((ExecutableElement) e, entityType));
            }
        }

        JavaFile javaFile = JavaFile.builder(pkg, clsB.build()).build();
        try {
            javaFile.writeTo(ctx.getFiler());
        } catch (IOException ex) {
            ctx.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "生成实现类失败: " + ex.getMessage());
        }
    }

    private MethodSpec buildMethod(ExecutableElement method, TypeElement entityType) {
        String name = method.getSimpleName().toString();
        MethodSpec.Builder mb = MethodSpec.methodBuilder(name)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.get(method.getReturnType()));

        // 添加所有参数
        method.getParameters().forEach(p ->
                mb.addParameter(TypeName.get(p.asType()), p.getSimpleName().toString())
        );

        CodeBlock.Builder code = CodeBlock.builder();
        String matchedPrefix = null;
        for (String prefix : QUERY_PREFIXES) {
            if (name.startsWith(prefix)) {
                matchedPrefix = prefix;
                break;
            }
        }

        if (matchedPrefix != null) {
            String suffix = name.substring(matchedPrefix.length());
            List<DaoImplGeneratorHelper.ConditionStructure> conds = helper.parseMethodConditions(suffix);
            if (conds.size() != method.getParameters().size()) {
                ctx.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        String.format("参数数量不匹配 预期: %d 实际: %d", conds.size(), method.getParameters().size()), method);
                code.addStatement("return null");
            } else {
                boolean singleResult = "getBy".equals(matchedPrefix);
                String queryMethod = singleResult ? "query2One" : "query2Lst";
                code.add("return bud$$().$L(builder -> builder", queryMethod);
                AtomicInteger idx = new AtomicInteger(0);
                for (int i = 0; i < conds.size(); i++) {
                    DaoImplGeneratorHelper.ConditionStructure c = conds.get(i);
                    String key = helper.resolveFullColumnPath(entityType, c.fieldName, idx);
                    String val = helper.formatQueryParameter(method.getParameters().get(i), c.operator);
                    code.add(String.format(".%s(\"%s\", %s)", c.operator, key, val));
                }
                code.add(");\n");
            }
        } else {
            code.addStatement("return null");
        }

        mb.addCode(code.build());
        return mb.build();
    }
}
