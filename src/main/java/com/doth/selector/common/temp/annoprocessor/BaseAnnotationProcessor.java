// src/main/java/com/doth/selector/processor/core/BaseAnnotationProcessor.java
package com.doth.selector.common.temp.annoprocessor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;

/**
 * 所有注解处理器都继承此基类，
 * 自动初始化 ProcessingContext 并统一返回最新的 SourceVersion。
 */
public abstract class BaseAnnotationProcessor extends AbstractProcessor {

    /** 上下文，包含 filer/messager/typeUtils/elementUtils */
    protected ProcessingContext context;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.context = new ProcessingContext(
            processingEnv.getFiler(),
            processingEnv.getMessager(),
            processingEnv.getTypeUtils(),
            processingEnv.getElementUtils()
        );
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * 子类只需实现 process() 方法，直接使用 context 即可。
     */
    @Override
    public abstract boolean process(
        java.util.Set<? extends javax.lang.model.element.TypeElement> annotations,
        javax.annotation.processing.RoundEnvironment roundEnv
    );
}
