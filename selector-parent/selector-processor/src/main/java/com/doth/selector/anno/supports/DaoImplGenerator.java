package com.doth.selector.anno.supports;

import com.doth.selector.anno.AutoImpl;
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
 * <p>生成 DAO 实现类的核心类</p>
 * <p></p>
 * <p>职责说明</p>
 * <ol>
 *     <li>根据抽象 DAO 类生成对应的实现类代码</li>
 *     <li>遍历抽象方法并使用辅助类生成查询方法的具体实现</li>
 * </ol>
 * <hr/>
 * <p>使用说明</p>
 * <ol>
 *     <li>在 Processor 中初始化时创建该类实例: <code>new DaoImplGenerator(context)</code></li>
 *     <li>调用 {@link #generate(TypeElement)} 方法生成 DAO 的实现类</li>
 * </ol>
 * <hr/>
 * <p>后续改进</p>
 * <ol>
 *     <li>支持更多方法名约定（如分页查询等前缀）</li>
 *     <li>增强生成语句的灵活性, 比如引入事务注解等</li>
 * </ol>
 */
public class DaoImplGenerator {
    private final ProcessingContext ctx;
    private final DaoImplGeneratorHelper helper;
    private static final List<String> QUERY_PREFIXES = Arrays.asList("queryBy", "getBy", "listBy");

    public DaoImplGenerator(ProcessingContext ctx) {
        this.ctx = ctx;
        this.helper = new DaoImplGeneratorHelper(ctx);
    }

    /**
     * <p>生成 DAO 实现类</p>
     * <p>说明: 根据指定的抽象 DAO 类信息, 生成对应的实现类文件</p>
     * <p>生成流程: </p>
     * <ol>
     *     <li>确定包名和实现类名（在抽象类名后加 "Impl"）</li>
     *     <li>如果有 {@link AutoImpl#springSupport()} 注解, 则为类添加 {@code @Repository} 注解</li>
     *     <li>添加一个无参构造函数并调用 {@code super()}</li>
     *     <li>提取 DAO 泛型中的实体类型, 以用于构建查询条件</li>
     *     <li>遍历 DAO 中所有抽象查询方法, 调用 {@link #buildMethod(ExecutableElement, TypeElement)} 生成实现方法</li>
     *     <li>将生成的类写入文件系统</li>
     * </ol>
     *
     * @param abstractClass 抽象 DAO 类的元素
     */
    public void generate(TypeElement abstractClass) {
        String pkg = ctx.getElementUtils()
                .getPackageOf(abstractClass)
                .getQualifiedName()
                .toString();

        // 写类结构
        // 1.类名 默认生成的 实现类类名: daoClzName + impl
        String implName = abstractClass.getSimpleName() + "Impl";

        // 2.继承关系
        AutoImpl anno = abstractClass.getAnnotation(AutoImpl.class);

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

    /**
     * <p>构建 DAO 方法的实现</p>
     * <p>说明: 根据抽象方法名和参数, 生成具体的查询逻辑调用</p>
     * <p>工作流程: </p>
     * <ol>
     *     <li>复制方法签名: 名称、修饰符、返回类型和参数列表</li>
     *     <li>识别方法名前缀（如 queryBy, getBy, listBy）</li>
     *     <li>解析前缀后的条件部分, 生成相应的查询语句链（使用辅助类 {@link DaoImplGeneratorHelper}）</li>
     *     <li>如果方法前缀是 {@code getBy}, 则调用 {@code toOne()} 方法获取单个结果；否则返回列表</li>
     *     <li>如果无法识别前缀或条件不匹配, 则返回 {@code null} 并打印错误</li>
     * </ol>
     *
     * @param method     抽象方法元素
     * @param entityType 实体类型元素
     * @return 生成的方法实现 {@link MethodSpec.Builder} 对象
     */
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
                    key = NamingConvertUtil.camel2Snake(key);
                    String val = helper.formatQueryParameter(method.getParameters().get(i), c.operator);
                    code.add(String.format(".%s(\"%s\", %s)", c.operator, key, val));
                }
                // 根据 single 判断是否加 .toOne()
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
