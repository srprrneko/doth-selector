package com.doth.selector.anno.processor.core.dtogenerate;

import com.doth.selector.anno.JoinLevel;
import com.doth.selector.anno.Next;
import com.doth.selector.common.util.NamingConvertUtil;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;

public class JNNameResolver {
    // 后续拓展自动寻找唯一类型, 当前是默认类简单名小写
    static String getOrD4JNLevelAttrName(JoinLevel jl) {
        return resolveJNName(jl.attrName(), jl::clz);
    }

    static String getOrD4JNLevelAttrName(Next nx) {
        return resolveJNName(nx.attrName(), nx::clz);
    }

    /**
     * 统一解析注解属性: 优先使用 JN 的 attrName, 否则通过捕获 MirroredTypeException 获取类名
     *
     * @param attrName 属性名
     * @param clzCall  类提取器 用于触发异常
     * @return attrName / lower-ClzSimpleName
     */
    static String resolveJNName(String attrName, ClzExtractor clzCall) {
        if (!attrName.isEmpty()) {
            return attrName;
        }
        try {
            Class<?> extract = clzCall.extract();
        } catch (MirroredTypeException m) {
            // 通过异常进行捕获typeMirror 的信息
            TypeElement te = (TypeElement) ((DeclaredType) m.getTypeMirror()).asElement();
            return NamingConvertUtil.lowerFstLetter(te.getSimpleName().toString(), false);
        }
        return "";
    }

    @FunctionalInterface
    /*private*/ interface ClzExtractor {
        Class<?> extract();
    }
}