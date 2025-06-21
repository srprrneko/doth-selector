// src/main/java/com/doth/selector/processor/core/ProcessingContext.java
package com.doth.selector.anno.processor;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * 把 ProcessingEnvironment 中常用的工具一次性封装，供注解处理器与生成器共享。
 */
public class ProcessingContext {
    private final Filer filer;
    private final Messager messager;
    private final Types typeUtils;
    private final Elements elementUtils;

    public ProcessingContext(Filer filer,
                             Messager messager,
                             Types typeUtils,
                             Elements elementUtils) {
        this.filer = filer;
        this.messager = messager;
        this.typeUtils = typeUtils;
        this.elementUtils = elementUtils;
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
