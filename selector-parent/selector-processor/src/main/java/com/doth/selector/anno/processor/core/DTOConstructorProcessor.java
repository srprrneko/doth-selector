package com.doth.selector.anno.processor.core;

import com.doth.selector.anno.DTOConstructor;
import com.doth.selector.anno.DependOn;
import com.doth.selector.anno.JoinLevel;
import com.doth.selector.anno.Next;
import com.doth.selector.anno.processor.BaseAnnotationProcessor;
import com.doth.selector.common.dto.DTOFactory;
import com.doth.selector.common.dto.DTOSelectFieldsListFactory;
import com.doth.selector.common.util.NamingConvertUtil;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;

/**
 * 自动为被 @DTOConstructor 标注的构造方法生成对应的 DTO 类，
 * 并在静态块中注册到 DTOFactory 及 DTOSelectFieldsListFactory。
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("com.doth.selector.anno.DTOConstructor")
public class DTOConstructorProcessor extends BaseAnnotationProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element elem : roundEnv.getElementsAnnotatedWith(DTOConstructor.class)) {
            if (!(elem instanceof ExecutableElement)) continue;
            ExecutableElement ctor = (ExecutableElement) elem;
            TypeElement entity = (TypeElement) ctor.getEnclosingElement();
            DTOConstructor ann = ctor.getAnnotation(DTOConstructor.class);
            generateDto(entity, ctor, ann.id());
        }
        return true;
    }

    private void generateDto(TypeElement entityClass,
                             ExecutableElement ctorElem,
                             String dtoId) {
        // 包名、DTO 类名、实体类型引用
        String packageName = context.getElementUtils()
                .getPackageOf(entityClass)
                .getQualifiedName()
                .toString();
        String dtoClassName = NamingConvertUtil.toUpperCaseFirstLetter(dtoId, false);
        ClassName entityType = ClassName.get(packageName, entityClass.getSimpleName().toString());

        // 准备 DTO 类构建器，添加 @DependOn、@Data、@NoArgsConstructor
        TypeSpec.Builder classB = TypeSpec.classBuilder(dtoClassName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(DependOn.class)
                        .addMember("clzPath", "$S",
                                context.getElementUtils()
                                        .getBinaryName(entityClass)
                                        .toString())
                        .build())
                .addAnnotation(Data.class)
                .addAnnotation(NoArgsConstructor.class)
                .addAnnotation(AllArgsConstructor.class);

        // 收集构造方法参数及主表字段名
        List<? extends VariableElement> params = ctorElem.getParameters();
        Set<String> mainNames = new HashSet<>();
        for (VariableElement p : params) {
            String n = p.getSimpleName().toString();
            if (!n.contains("_")) {
                mainNames.add(n);
            }
        }

        // 统计每个下划线后面 base 出现次数，用于冲突判断
        Map<String, Long> baseCount = new HashMap<>();
        for (VariableElement p : params) {
            String n = p.getSimpleName().toString();
            if (n.contains("_")) {
                String base = n.substring(n.indexOf('_') + 1);
                baseCount.put(base, baseCount.getOrDefault(base, 0L) + 1);
            }
        }

        List<FieldMeta> fields = new ArrayList<>();
        boolean inJoinChain = false;
        String chainAttrName = "";
        for (VariableElement p : params) {
            String n = p.getSimpleName().toString();
            String fieldName;

            if (!n.contains("_")) {
                // 主表字段：直接用参数名
                fieldName = n;
                // 重置 join 链状态
                inJoinChain = false;
                chainAttrName = "";

            } else {
                // 下划线分隔 prefix / base
                String prefix = n.substring(0, n.indexOf('_'));
                String base = n.substring(n.indexOf('_') + 1);

                // 检测 @JoinLevel / @Next 注解，开始或继续 join 链，并记录 attrName
                JoinLevel jl = p.getAnnotation(JoinLevel.class);
                Next nx = p.getAnnotation(Next.class);
                if (jl != null) {
                    inJoinChain = true;
                    chainAttrName = getPropNameFromJoinLevel(jl);
                } else if (nx != null) {
                    inJoinChain = true;
                    chainAttrName = getPropNameFromNext(nx);
                } else if (!inJoinChain) {
                    // 既没有下划线前注解，也不在 join 链中：当作普通字段
                    fieldName = n;
                    TypeName t = TypeName.get(p.asType());
                    classB.addField(FieldSpec.builder(t, fieldName, Modifier.PRIVATE).build());
                    fields.add(new FieldMeta(p, fieldName));
                    continue;
                }

                // 当 prefix 为空时，使用 attrName 的前三个小写字母作为默认前缀
                if (prefix.isEmpty()) {
                    String attr = chainAttrName != null ? chainAttrName : "";
                    prefix = attr.length() >= 3
                            ? attr.substring(0, 3).toLowerCase()
                            : attr.toLowerCase();
                }

                boolean conflict = mainNames.contains(base)
                        || baseCount.getOrDefault(base, 0L) > 1;
                // 冲突则 prefix + Base 驼峰，否则直接 base
                fieldName = conflict
                        ? prefix + capitalize(base)
                        : base;
            }

            // 添加字段
            TypeName t = TypeName.get(p.asType());
            classB.addField(FieldSpec.builder(t, fieldName, Modifier.PRIVATE).build());
            fields.add(new FieldMeta(p, fieldName));
        }

        // 构造方法：public DTO(Entity entity) { ... }
        String varEntity = decapitalize(entityClass.getSimpleName().toString());
        MethodSpec.Builder ctorB = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(entityType, varEntity);

        // 构造方法体：主表字段 & 关联链
        boolean chain = false;
        String currentPath = null;
        Map<String, String> prefixPath = new HashMap<>();

        for (FieldMeta fm : fields) {
            VariableElement p = fm.param;
            String fn = fm.name;
            String pn = p.getSimpleName().toString();

            if (!pn.contains("_")) {
                // 主表字段
                chain = false;
                prefixPath.clear();
                String getter = "get" + capitalize(pn);
                ctorB.addStatement("this.$L = $L.$L()", fn, varEntity, getter);
            } else {
                // 关联字段：根据 @JoinLevel / @Next 的 attrName 或 clz 推断属性名
                String prefix = pn.substring(0, pn.indexOf('_'));
                String base = pn.substring(pn.indexOf('_') + 1);

                JoinLevel jl = p.getAnnotation(JoinLevel.class);
                Next nx = p.getAnnotation(Next.class);

                // 决定用哪个属性名来生成 getXxx()
                String prop;
                if (jl != null) {
                    prop = getPropNameFromJoinLevel(jl);
                } else if (nx != null) {
                    prop = getPropNameFromNext(nx);
                } else {
                    prop = prefix;
                }

                if (jl != null || !chain) {
                    // 新的关联链开始
                    chain = true;
                    prefixPath.clear();
                    currentPath = varEntity + ".get" + capitalize(prop) + "()";
                    prefixPath.put(prefix, currentPath);
                } else if (nx != null) {
                    // 嵌套 next
                    String prev = currentPath;
                    currentPath = prev + ".get" + capitalize(prop) + "()";
                    prefixPath.put(prefix, currentPath);
                }

                // 赋值
                if (prefixPath.containsKey(prefix)) {
                    String getter = "get" + capitalize(base);
                    ctorB.addStatement("this.$L = $L.$L()",
                            fn, prefixPath.get(prefix), getter);
                } else {
                    ctorB.addComment("未定义的关联前缀 '$L' 对应字段 $L", prefix, fn);
                }
            }
        }
        classB.addMethod(ctorB.build());

        // 静态注册块保持不变……
        CodeBlock.Builder staticB = CodeBlock.builder()
                .addStatement("$T.register($T.class, $S, $T.class)",
                        DTOFactory.class, entityType, dtoId,
                        ClassName.get(packageName, dtoClassName))
                .addStatement("$T __selectFields = new $T<>()",
                        ParameterizedTypeName.get(List.class, String.class),
                        ArrayList.class);

        int aliasIdx = 1;
        boolean chain2 = false;
        Map<String, String> prefixAlias = new HashMap<>();

        for (VariableElement p : params) {
            String pn = p.getSimpleName().toString();
            if (!pn.contains("_")) {
                staticB.addStatement("__selectFields.add($S)", "t0." + pn);
                chain2 = false;
                prefixAlias.clear();
            } else {
                String prefix = pn.substring(0, pn.indexOf('_'));
                String base = pn.substring(pn.indexOf('_') + 1);
                JoinLevel jl = p.getAnnotation(JoinLevel.class);
                Next nx = p.getAnnotation(Next.class);

                if (jl != null || !chain2) {
                    String al = "t" + aliasIdx++;
                    prefixAlias.clear();
                    prefixAlias.put(prefix, al);
                    chain2 = true;
                    staticB.addStatement("__selectFields.add($S)", al + "." + base);
                } else if (nx != null) {
                    String al = "t" + aliasIdx++;
                    prefixAlias.put(prefix, al);
                    staticB.addStatement("__selectFields.add($S)", al + "." + base);
                } else if (prefixAlias.containsKey(prefix)) {
                    staticB.addStatement("__selectFields.add($S)",
                            prefixAlias.get(prefix) + "." + base);
                } else {
                    staticB.addStatement("__selectFields.add($S)",
                            "t?." + base);
                }
            }
        }

        staticB.addStatement("$T.register($T.class, $S, __selectFields)",
                DTOSelectFieldsListFactory.class,
                entityType, dtoId);
        classB.addStaticBlock(staticB.build());

        // 写文件
        JavaFile javaFile = JavaFile.builder(packageName, classB.build()).build();
        try {
            javaFile.writeTo(context.getFiler());
        } catch (IOException e) {
            context.getMessager()
                    .printMessage(Diagnostic.Kind.ERROR,
                            "生成 DTO 类失败: " + e.getMessage(),
                            entityClass);
        }
    }


    /**
     * 首字母大写
     */
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * 首字母小写（保留例如 URL 这样的全大写前缀）
     */
    private String decapitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        if (s.length() > 1 && Character.isUpperCase(s.charAt(0))
                && Character.isUpperCase(s.charAt(1))) {
            return s;
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private String getPropNameFromJoinLevel(JoinLevel jl) {
        // 如果用户手动指定了 attrName，就用它
        if (!jl.attrName().isEmpty()) {
            return jl.attrName();
        }
        // 否则用 jl.clz() 的类型名（小写首字母）
        TypeMirror tm;
        try {
            jl.clz();             // 这里肯定会抛 MirroredTypeException
            return "";            // 不会到这行
        } catch (MirroredTypeException mte) {
            tm = mte.getTypeMirror();
        }
        DeclaredType dt = (DeclaredType) tm;
        TypeElement te = (TypeElement) dt.asElement();
        return decapitalize(te.getSimpleName().toString());
    }

    private String getPropNameFromNext(Next nx) {
        if (!nx.attrName().isEmpty()) {
            return nx.attrName();
        }
        TypeMirror tm;
        try {
            nx.clz();
            return "";
        } catch (MirroredTypeException mte) {
            tm = mte.getTypeMirror();
        }
        DeclaredType dt = (DeclaredType) tm;
        TypeElement te = (TypeElement) dt.asElement();
        return decapitalize(te.getSimpleName().toString());
    }


    /**
     * 用于临时存储字段及其对应的参数元素
     */
    private static class FieldMeta {
        final VariableElement param;
        final String name;

        FieldMeta(VariableElement p, String n) {
            this.param = p;
            this.name = n;
        }
    }
}
