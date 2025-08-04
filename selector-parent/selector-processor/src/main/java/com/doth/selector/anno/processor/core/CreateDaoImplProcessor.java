package com.doth.selector.anno.processor.core;

import com.doth.selector.anno.AutoImpl;
import com.doth.selector.anno.processor.BaseAnnotationProcessor;
import com.doth.selector.anno.supports.DaoImplGenerator;
import com.google.auto.service.AutoService;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

/**
 * <p>处理 @AutoImpl 注解的 Annotation Processor</p>
 * <p></p>
 * <p>职责说明</p>
 * <ol>
 *     <li>扫描标注了 {@code @AutoImpl} 的抽象类</li>
 *     <li>校验类定义是否为抽象类</li>
 *     <li>将具体的生成逻辑委托给 {@link DaoImplGenerator}</li>
 * </ol>
 * <hr/>
 * <p>使用说明</p>
 * <ol>
 *     <li>在注解处理环境初始化时, 创建该 Processor 的实例</li>
 *     <li>符合条件的类会自动触发 {@link #process(Set, RoundEnvironment)} 方法</li>
 *     <li>对于非抽象类或错误用法, 打印编译错误提示</li>
 * </ol>
 * <hr/>
 * <p>后续改进</p>
 * <ol>
 *     <li>支持更多注解选项, 比如指定 DAO 后缀名或其它配置</li>
 *     <li>增强错误提示, 使其更具可读性和指导性</li>
 * </ol>
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("com.doth.selector.anno.AutoImpl")
public class CreateDaoImplProcessor extends BaseAnnotationProcessor {

    private DaoImplGenerator generator;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        // 复用 context
        this.generator = new DaoImplGenerator(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(AutoImpl.class)) {
            if (!(element instanceof TypeElement)
                    || element.getKind() != ElementKind.CLASS
                    || !element.getModifiers().contains(Modifier.ABSTRACT)) {

                context.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "继承 Selector 请使用抽象类!!", element);
                continue;
            }
            // 委托给 DaoImplGenerator 生成实现
            generator.generate((TypeElement) element);
        }
        return true;
    }
}
