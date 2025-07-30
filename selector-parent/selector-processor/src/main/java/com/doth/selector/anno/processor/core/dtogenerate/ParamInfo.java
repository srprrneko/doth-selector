package com.doth.selector.anno.processor.core.dtogenerate;

import com.doth.selector.anno.JoinLevel;
import com.doth.selector.anno.Next;

import javax.lang.model.element.VariableElement;

/**
 * 从构造提取的参数的信息, 最终转换为dto类的字段信息
 */
class ParamInfo {


    ParamInfo(VariableElement param) {
        this.param = param;
        this.rawArgName = param.getSimpleName().toString();
    }

    /**
     * 参数本身
     */
    VariableElement param;

    String rawArgName;

    /**
     * 是否是属于连接层级 (额外处理)
     */
    boolean isJoin;

    /**
     * 字段名自动生成的 前缀
     */
    String prefix;

    /**
     * 原始字段名
     */
    String originName;

    /**
     * 最终生成的字段名
     */
    String finalFName;

    String showName;

    /**
     * 用于标记该字段是否作为 join 链的起点
     */
    JoinLevel jl;

    /**
     * 用于标记该字段是否作为 join 链的下一级起点
     */
    Next nx;

    void init4JNormalMod(VariableElement p) {
        this.isJoin = false;
        this.finalFName = rawArgName;
    }

    void init4JoinMod(VariableElement param) {
        this.jl = param.getAnnotation(JoinLevel.class);
        this.nx = param.getAnnotation(Next.class);
        this.isJoin = true;

        this.prefix = rawArgName.substring(0, rawArgName.indexOf('_'));
        this.originName = rawArgName.substring(rawArgName.indexOf('_') + 1);

    }

}