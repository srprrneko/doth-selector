package com.doth.selector.anno.processor.core;

import com.doth.selector.anno.*;
import com.doth.selector.anno.processor.BaseAnnotationProcessor;
import com.doth.selector.common.dto.*;
import com.doth.selector.common.util.NamingConvertUtil;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.doth.selector.anno.DTOConstructor")
public class DTOConstructorProcessor extends BaseAnnotationProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element elem : roundEnv.getElementsAnnotatedWith(DTOConstructor.class)) {
            if (!(elem instanceof ExecutableElement)) continue;

            ExecutableElement ctor = (ExecutableElement) elem;
            TypeElement entity = (TypeElement) ctor.getEnclosingElement();
            String dtoId = ctor.getAnnotation(DTOConstructor.class).id();

            generateDto(entity, ctor, dtoId);
        }
        return true;
    }

    private void generateDto(TypeElement entityClass, ExecutableElement ctorElem, String dtoId) {
        String pkg = context.getElementUtils()
                .getPackageOf(entityClass)
                .getQualifiedName().toString();

        String dtoName = NamingConvertUtil.upperFstLetter(dtoId, false);
        ClassName entityType = ClassName.get(pkg, entityClass.getSimpleName().toString());

        TypeSpec.Builder cls = TypeSpec.classBuilder(dtoName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(DependOn.class)
                        .addMember("clzPath", "$S", context.getElementUtils().getBinaryName(entityClass).toString())
                        .build())
                .addAnnotation(Data.class)
                .addAnnotation(NoArgsConstructor.class)
                .addAnnotation(AllArgsConstructor.class);

        List<ParamInfo> params = parseParams(ctorElem);
        for (ParamInfo info : params) {
            cls.addField(FieldSpec.builder(
                            TypeName.get(info.param.asType()),
                            info.fieldName,
                            Modifier.PRIVATE
                    ).build());
        }

        cls.addMethod(buildConstructor(entityClass, params));

        JoinChainResult chain = processJoinChains(params, entityClass);

        cls.addStaticBlock(buildStaticInitBlock(entityType, pkg, dtoId, chain.selectFields, chain.joinInfos, entityClass));

        try {
            JavaFile.builder(pkg, cls.build()).build().writeTo(context.getFiler());
        } catch (IOException e) {
            context.getMessager().printMessage(Diagnostic.Kind.ERROR, "生成 DTO 类失败: " + e.getMessage(), entityClass);
        }
    }

    private List<ParamInfo> parseParams(ExecutableElement ctor) {
        List<? extends VariableElement> parameters = ctor.getParameters();
        Set<String> mainNames = new HashSet<>();
        Map<String, Long> baseCount = new HashMap<>();
        for (VariableElement p : parameters) {
            String raw = p.getSimpleName().toString();
            if (!raw.contains("_")) mainNames.add(raw);
            else {
                String base = raw.substring(raw.indexOf('_') + 1);
                baseCount.put(base, baseCount.getOrDefault(base, 0L) + 1);
            }
        }
        List<ParamInfo> result = new ArrayList<>();
        boolean inChain = false;
        String chainAttr = null;
        for (VariableElement p : parameters) {
            String raw = p.getSimpleName().toString();
            ParamInfo info = new ParamInfo();
            info.param = p;
            info.jl = p.getAnnotation(JoinLevel.class);
            info.nx = p.getAnnotation(Next.class);
            if (!raw.contains("_")) {
                info.isJoin = false;
                info.fieldName = raw;
                inChain = false;
                chainAttr = null;
            } else {
                info.isJoin = true;
                String prefix = raw.substring(0, raw.indexOf('_'));
                String base = raw.substring(raw.indexOf('_') + 1);
                info.prefix = prefix;
                info.base = base;
                if (info.jl != null) {
                    inChain = true;
                    chainAttr = getPropNameFromJoinLevel(info.jl);
                } else if (info.nx != null) {
                    inChain = true;
                    chainAttr = getPropNameFromNext(info.nx);
                } else if (!inChain) {
                    info.isJoin = false;
                    info.fieldName = raw;
                }
                if (info.isJoin) {
                    boolean conflict = mainNames.contains(base) || baseCount.getOrDefault(base, 0L) > 1;
                    info.fieldName = conflict ? prefix + capitalize(base) : base;
                }
            }
            result.add(info);
        }
        return result;
    }

    private MethodSpec buildConstructor(TypeElement entity, List<ParamInfo> params) {
        String var = decapitalize(entity.getSimpleName().toString());
        MethodSpec.Builder mb = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(entity), var);
        Map<String, String> prefixPath = new HashMap<>();
        boolean chain = false;
        String currentPath = null;
        for (ParamInfo info : params) {
            String fn = info.fieldName, raw = info.param.getSimpleName().toString();
            if (!info.isJoin) {
                mb.addStatement("this.$L = $L.get$L()", fn, var, capitalize(raw));
            } else {
                if (info.jl != null || !chain) {
                    chain = true;
                    prefixPath.clear();
                    currentPath = var + ".get" + capitalize(getPropNameFromJoinLevel(info.jl)) + "()";
                    prefixPath.put(info.prefix, currentPath);
                } else if (info.nx != null) {
                    String prev = currentPath;
                    currentPath = prev + ".get" + capitalize(getPropNameFromNext(info.nx)) + "()";
                    prefixPath.put(info.prefix, currentPath);
                }
                String path = prefixPath.get(info.prefix);
                if (path != null) mb.addStatement("this.$L = $L.get$L()", fn, path, capitalize(info.base));
                else mb.addComment("未定义关联前缀 '$L' 对应字段 $L", info.prefix, fn);
            }
        }
        return mb.build();
    }

    private JoinChainResult processJoinChains(List<ParamInfo> params, TypeElement entityClass) {
        List<String> selects = new ArrayList<>();
        List<JoinInfo> joins = new ArrayList<>();
        Map<String, String> prefixAlias = new HashMap<>();
        int idx = 1;
        boolean chain = false;
        String currentAttr = null;
        for (ParamInfo info : params) {
            if (!info.isJoin) {
                selects.add("t0." + info.param.getSimpleName());
            } else {
                if (info.jl != null || !chain) {
                    chain = true;
                    prefixAlias.clear();
                    String alias = "t" + idx++;
                    prefixAlias.put(info.prefix, alias);
                    currentAttr = info.jl != null ? getPropNameFromJoinLevel(info.jl) : info.prefix;
                    joins.add(new JoinInfo(currentAttr, alias));
                    selects.add(alias + "." + info.base);
                } else if (info.nx != null) {
                    String alias = "t" + idx++;
                    prefixAlias.put(info.prefix, alias);
                    currentAttr = currentAttr + "." + getPropNameFromNext(info.nx);
                    joins.add(new JoinInfo(currentAttr, alias));
                    selects.add(alias + "." + info.base);
                } else if (prefixAlias.containsKey(info.prefix)) {
                    selects.add(prefixAlias.get(info.prefix) + "." + info.base);
                } else {
                    selects.add("t?." + info.base);
                }
            }
        }
        // filter invalid join paths
        List<JoinInfo> valid = joins.stream()
                .filter(j -> resolveJoinField(entityClass, j.getAttrPath()) != null)
                .collect(Collectors.toList());
        return new JoinChainResult(selects, valid);
    }

    private CodeBlock buildStaticInitBlock(ClassName entityType, String pkg, String dtoId,
                                           List<String> selectFields, List<JoinInfo> joinInfos,
                                           TypeElement entityClass) {
        CodeBlock.Builder cb = CodeBlock.builder()
                .addStatement("$T.register($T.class, $S, $T.class)",
                        DTOFactory.class, entityType, dtoId,
                        ClassName.get(pkg, NamingConvertUtil.upperFstLetter(dtoId, false)))
                .addStatement("$T __select = new $T<>()", ParameterizedTypeName.get(List.class, String.class), ArrayList.class);
        for (String f : selectFields) cb.addStatement("__select.add($S)", f);
        cb.addStatement("$T.register($T.class, $S, __select)",
                DTOSelectFieldsListFactory.class, entityType, dtoId);

        Map<String, String> pathAlias = new LinkedHashMap<>();
        for (JoinInfo ji : joinInfos) pathAlias.put(ji.getAttrPath(), ji.getAlias());

        List<CodeBlock> defs = new ArrayList<>();
        for (JoinInfo ji : joinInfos) {
            VariableElement field = resolveJoinField(entityClass, ji.getAttrPath());
            Join ann = field.getAnnotation(Join.class);
            String table = NamingConvertUtil.camel2SnakeCase(((TypeElement) ((DeclaredType) field.asType()).asElement()).getSimpleName().toString());
            int idxDot = ji.getAttrPath().lastIndexOf('.');
            String parent = idxDot > 0 ? pathAlias.get(ji.getAttrPath().substring(0, idxDot)) : "t0";
            defs.add(CodeBlock.of("new $T($S,$S,$S,$S,$S)", JoinDef.class,
                    table, ann.fk(), ann.refFK(), ji.getAlias(), parent));
        }
        cb.addStatement("$T.register($T.class, $S, new $T($T.of($L)))",
                DTOJoinInfoFactory.class, entityType, dtoId,
                DTOJoinInfo.class, List.class,
                CodeBlock.join(defs, ", "));
        return cb.build();
    }

    private String getPropNameFromJoinLevel(JoinLevel jl) {
        if (!jl.attrName().isEmpty()) return jl.attrName();
        try {
            jl.clz();
        } catch (MirroredTypeException m) {
            TypeElement te = (TypeElement) ((DeclaredType) m.getTypeMirror()).asElement();
            return decapitalize(te.getSimpleName().toString());
        }
        return "";
    }

    private String getPropNameFromNext(Next nx) {
        if (!nx.attrName().isEmpty()) return nx.attrName();
        try {
            nx.clz();
        } catch (MirroredTypeException m) {
            TypeElement te = (TypeElement) ((DeclaredType) m.getTypeMirror()).asElement();
            return decapitalize(te.getSimpleName().toString());
        }
        return "";
    }

    private VariableElement resolveJoinField(TypeElement root, String path) {
        String[] segs = path.split("\\.");
        TypeElement cur = root;
        VariableElement found = null;
        for (int i = 0; i < segs.length; i++) {
            found = null;
            for (Element e : cur.getEnclosedElements()) {
                if (e.getKind() == ElementKind.FIELD && e.getSimpleName().contentEquals(segs[i])) {
                    found = (VariableElement) e;
                    break;
                }
            }
            if (found == null) return null;
            if (i < segs.length - 1) cur = (TypeElement) ((DeclaredType) found.asType()).asElement();
        }
        return found;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String decapitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        if (s.length() > 1 && Character.isUpperCase(s.charAt(0)) && Character.isUpperCase(s.charAt(1))) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private static class ParamInfo {
        VariableElement param;
        boolean isJoin;
        String prefix, base, fieldName;
        JoinLevel jl;
        Next nx;
    }

    @Value
    @AllArgsConstructor
    private static class JoinChainResult {
        List<String> selectFields;
        List<JoinInfo> joinInfos;
    }

    @Value
    @AllArgsConstructor
    private static class JoinInfo {
        String attrPath;
        String alias;
    }
}
