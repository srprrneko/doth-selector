package com.doth.selector.anno.supports;

import com.doth.selector.anno.CreateDaoImpl;
import com.doth.selector.anno.processor.ProcessingContext;
import com.doth.selector.common.util.NamingConvertUtil;
import com.squareup.javapoet.*;
import org.springframework.stereotype.Repository;

import javax.lang.model.element.*;
import javax.tools.Diagnostic;
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
        String pkg = ctx.getElementUtils()
                .getPackageOf(abstractClass)
                .getQualifiedName()
                .toString();

        // 写类结构
        // 1.类名 默认生成的 实现类类名: daoClzName + impl
        String implName = abstractClass.getSimpleName() + "Impl";

        // 2.继承关系
        CreateDaoImpl anno = abstractClass.getAnnotation(CreateDaoImpl.class);

        TypeSpec.Builder clsB = TypeSpec.classBuilder(implName)
                .addModifiers(Modifier.PUBLIC)
                .superclass(TypeName.get(abstractClass.asType()));

        // 2.1是否集成进springIoc容器
        boolean useSpring = anno != null && anno.springSupport();
        if (useSpring) {
            clsB.addAnnotation(Repository.class);
        }


        // 3.添加无参构造
        clsB.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("super()")
                .build());

        // 获取实体泛型
        // 用于为方法生成做准备
        TypeElement entityType = helper.extractGenericEntityType(abstractClass);

        // 遍历dao中的所有抽象方法
        for (Element e : abstractClass.getEnclosedElements()) {
            if (e.getKind() == ElementKind.METHOD
                    && e.getModifiers().contains(Modifier.ABSTRACT)) {

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

    // 构建方法
    private MethodSpec buildMethod(ExecutableElement method, TypeElement entityType) {
        // 构建方法签名
        // 1.方法名
        String name = method.getSimpleName().toString();
        // 2.照搬方法修饰符, 方法名, 返回值
        MethodSpec.Builder mb = MethodSpec.methodBuilder(name)
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.get(method.getReturnType()));

        // 2.1 添加参数
        method.getParameters().forEach(p ->
                mb.addParameter(TypeName.get(p.asType()), p.getSimpleName().toString())
        );


        CodeBlock.Builder code = CodeBlock.builder();
        String matchedPrefix = null;

        // 识别查询约定前缀
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
                boolean single = "getBy".equals(matchedPrefix); // todo
                String queryMethod = "query";
                code.add("return bud$$().$L(builder -> builder", queryMethod);
                AtomicInteger idx = new AtomicInteger(0);
                for (int i = 0; i < conds.size(); i++) {
                    DaoImplGeneratorHelper.ConditionStructure c = conds.get(i);
                    String key = helper.resolveFullColumnPath(entityType, c.fieldName, idx);
                    key = NamingConvertUtil.camel2SnakeCase(key);
                    String val = helper.formatQueryParameter(method.getParameters().get(i), c.operator);
                    code.add(String.format(".%s(\"%s\", %s)", c.operator, key, val));
                }
                // code.add(");\n");
                // 结束构造，根据 single 决定是否加 .toOne()
                if (single) {
                    code.add(").toOne();\n");
                } else {
                    code.add(");\n");
                }
            }
        } else {
            code.addStatement("return null");
        }

        mb.addCode(code.build());
        return mb.build();
    }
}
