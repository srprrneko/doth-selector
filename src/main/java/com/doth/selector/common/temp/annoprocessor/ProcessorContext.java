// com/doth/selector/processor/core/ProcessorContext.java
package com.doth.selector.common.temp.annoprocessor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * 封装 ProcessingEnvironment 相关资源，供 Processor 与 Generator 共享。  
 */
public class ProcessorContext {
    private final Filer filer;
    private final Messager messager;
    private final Types typeUtils;
    private final Elements elementUtils;

    public ProcessorContext(ProcessingEnvironment env) {
        this.filer = env.getFiler();
        this.messager = env.getMessager();
        this.typeUtils = env.getTypeUtils();
        this.elementUtils = env.getElementUtils();
    }

    public Filer getFiler() {
        return filer;
    }

    public Messager getMessager() {
        return messager;
    }

    public Types getTypeUtils() {
        return typeUtils;
    }

    public Elements getElementUtils() {
        return elementUtils;
    }
}
