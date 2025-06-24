package com.doth.selector.anno.processor.core;

import com.doth.selector.anno.DTOConstructor;
import com.doth.selector.anno.DependOn;
import com.doth.selector.anno.Join;
import com.doth.selector.anno.JoinLevel;
import com.doth.selector.anno.Next;
import com.doth.selector.anno.processor.BaseAnnotationProcessor;
import com.doth.selector.common.dto.DTOFactory;
import com.doth.selector.common.dto.DTOJoinInfo;
import com.doth.selector.common.dto.DTOJoinInfoFactory;
import com.doth.selector.common.dto.DTOSelectFieldsListFactory;
import com.doth.selector.common.dto.JoinDef;
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
import javax.lang.model.type.TypeMirror;
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
        String packageName = context.getElementUtils().getPackageOf(entityClass).getQualifiedName().toString();
        String dtoClassName = NamingConvertUtil.toUpperCaseFirstLetter(dtoId, false);
        ClassName entityType = ClassName.get(packageName, entityClass.getSimpleName().toString());


        // 1.创建dto 先建类, 所以字段在下面
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(dtoClassName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(DependOn.class)
                        .addMember("clzPath", "$S", context.getElementUtils().getBinaryName(entityClass).toString())
                        .build())
                .addAnnotation(Data.class)
                .addAnnotation(NoArgsConstructor.class)
                .addAnnotation(AllArgsConstructor.class);
        // 2.创建字段
        List<ParamInfo> params = parseParams(ctorElem); // 解析参数

        // 从内部类 ParamInfo 里面添加完整的字段信息
        for (ParamInfo info : params) {
            classBuilder.addField(FieldSpec.builder(
                    TypeName.get(info.param.asType()),
                    info.fieldName,
                    Modifier.PRIVATE).build()
            );
        }

        // 3.创建构造 (记忆点: 构造方法也是方法)
        MethodSpec constructor = buildConstructor(entityClass, params);
        classBuilder.addMethod(constructor);

        // 4.提取join子句, 查询列列表, 为构建静态代码块逻辑做准备
        List<JoinInfo> joinInfos = collectJoinInfos(entityClass, params);
        List<String> selectFields = collectSelectFields(params);

        // 5.静态代码块
        CodeBlock staticBlock = buildStaticInitBlock(entityType, packageName, dtoId, selectFields, joinInfos, entityClass);
        classBuilder.addStaticBlock(staticBlock);

        // 收尾
        JavaFile javaFile = JavaFile.builder(packageName, classBuilder.build()).build();
        try {
            javaFile.writeTo(context.getFiler());
        } catch (IOException e) {
            context.getMessager().printMessage(Diagnostic.Kind.ERROR, "生成 DTO 类失败: " + e.getMessage(), entityClass);
        }
    }

    // 通过被标注的构造参数解析成 参数信息元
    private List<ParamInfo> parseParams(ExecutableElement ctor) {
        List<? extends VariableElement> parameters = ctor.getParameters();
        Set<String> mainNames = new HashSet<>();
        Map<String, Long> baseCount = new HashMap<>();

        // first pass: count main vs base
        for (VariableElement p : parameters) {
            String name = p.getSimpleName().toString();
            if (!name.contains("_")) {
                mainNames.add(name);
            } else {
                String base = name.substring(name.indexOf('_') + 1);
                baseCount.put(base, baseCount.getOrDefault(base, 0L) + 1);
            }
        }

        List<ParamInfo> result = new ArrayList<>();
        boolean inJoinChain = false;
        String chainAttr = null;

        for (VariableElement p : parameters) {
            String raw = p.getSimpleName().toString();
            ParamInfo info = new ParamInfo();
            info.param = p;
            info.jl = p.getAnnotation(JoinLevel.class);
            info.nx = p.getAnnotation(Next.class);

            if (!raw.contains("_")) {
                // main field
                info.isJoin = false;
                info.fieldName = raw;
                inJoinChain = false;
                chainAttr = null;
            } else {
                info.isJoin = true;
                String prefix = raw.substring(0, raw.indexOf('_'));
                String base = raw.substring(raw.indexOf('_') + 1);
                info.prefix = prefix;
                info.base = base;

                if (info.jl != null) {
                    inJoinChain = true;
                    chainAttr = getPropNameFromJoinLevel(info.jl);
                } else if (info.nx != null) {
                    inJoinChain = true;
                    chainAttr = getPropNameFromNext(info.nx);
                } else if (!inJoinChain) {
                    // treat as normal
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

    private MethodSpec buildConstructor(TypeElement entityClass, List<ParamInfo> params) {
        String varEntity = decapitalize(entityClass.getSimpleName().toString());
        MethodSpec.Builder mb = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(entityClass), varEntity);

        Map<String, String> prefixPath = new HashMap<>();
        boolean chain = false;
        String currentPath = null;

        for (ParamInfo info : params) {
            String fn = info.fieldName;
            String raw = info.param.getSimpleName().toString();
            if (!info.isJoin) {
                String getter = "get" + capitalize(raw);
                mb.addStatement("this.$L = $L.$L()", fn, varEntity, getter);
            } else {
                // join chain
                String prefix = info.prefix;
                String base = info.base;

                if (info.jl != null || !chain) {
                    chain = true;
                    prefixPath.clear();
                    currentPath = varEntity + ".get" + capitalize(getPropNameFromJoinLevel(info.jl)) + "()";
                    prefixPath.put(prefix, currentPath);
                } else if (info.nx != null) {
                    String prev = currentPath;
                    currentPath = prev + ".get" + capitalize(getPropNameFromNext(info.nx)) + "()";
                    prefixPath.put(prefix, currentPath);
                }

                String path = prefixPath.get(prefix);
                if (path != null) {
                    mb.addStatement("this.$L = $L.get" + capitalize(base) + "()", fn, path);
                } else {
                    mb.addComment("未定义关联前缀 '$L' 对应字段 $L", prefix, fn);
                }
            }
        }
        return mb.build();
    }

    private List<String> collectSelectFields(List<ParamInfo> params) {
        List<String> fields = new ArrayList<>();
        Map<String, String> prefixAlias = new HashMap<>();
        int aliasIdx = 1;
        boolean chain = false;
        for (ParamInfo info : params) {
            String raw = info.param.getSimpleName().toString();
            if (!info.isJoin) {
                fields.add("t0." + raw);
            } else {
                if (info.jl != null || !chain) {
                    chain = true;
                    String al = "t" + aliasIdx++;
                    prefixAlias.clear(); prefixAlias.put(info.prefix, al);
                    fields.add(al + "." + info.base);
                } else if (info.nx != null) {
                    String al = "t" + aliasIdx++;
                    prefixAlias.put(info.prefix, al);
                    fields.add(al + "." + info.base);
                } else if (prefixAlias.containsKey(info.prefix)) {
                    fields.add(prefixAlias.get(info.prefix) + "." + info.base);
                } else {
                    fields.add("t?." + info.base);
                }
            }
        }
        return fields;
    }

    private List<JoinInfo> collectJoinInfos(TypeElement entityClass, List<ParamInfo> params) {
        List<JoinInfo> infos = new ArrayList<>();
        int aliasIdx = 1;
        boolean chain = false;
        Map<String, String> prefixAlias = new HashMap<>();
        String currentAttr = null;

        for (ParamInfo info : params) {
            if (!info.isJoin) continue;
            if (info.jl != null || !chain) {
                chain = true;
                String alias = "t" + aliasIdx++;
                prefixAlias.clear(); prefixAlias.put(info.prefix, alias);
                currentAttr = info.jl != null ? getPropNameFromJoinLevel(info.jl) : info.prefix;
                infos.add(new JoinInfo(currentAttr, alias));
            } else if (info.nx != null) {
                String alias = "t" + aliasIdx++;
                prefixAlias.put(info.prefix, alias);
                String nextAttr = getPropNameFromNext(info.nx);
                currentAttr = currentAttr + "." + nextAttr;
                infos.add(new JoinInfo(currentAttr, alias));
            }
        }
        // filter invalid paths
        return infos.stream()
                .filter(ji -> resolveJoinField(entityClass, ji.attrPath) != null)
                .collect(Collectors.toList());
    }

    private CodeBlock buildStaticInitBlock(ClassName entityType, String pkg, String dtoId,
                                           List<String> selectFields, List<JoinInfo> joinInfos,
                                           TypeElement entityClass) {
        CodeBlock.Builder cb = CodeBlock.builder()
                .addStatement("$T.register($T.class, $S, $T.class)",
                        DTOFactory.class, entityType, dtoId,
                        ClassName.get(pkg, NamingConvertUtil.toUpperCaseFirstLetter(dtoId, false)))
                .addStatement("$T __select = new $T<>()",
                        ParameterizedTypeName.get(List.class, String.class), ArrayList.class);

        for (String expr : selectFields) {
            cb.addStatement("__select.add($S)", expr);
        }
        cb.addStatement("$T.register($T.class, $S, __select)",
                DTOSelectFieldsListFactory.class, entityType, dtoId);

        Map<String, String> pathAlias = new LinkedHashMap<>();
        for (JoinInfo ji : joinInfos) {
            pathAlias.put(ji.attrPath, ji.alias);
        }

        List<CodeBlock> defs = new ArrayList<>();
        for (JoinInfo ji : joinInfos) {
            VariableElement field = resolveJoinField(entityClass, ji.attrPath);
            Join joinAnn = field.getAnnotation(Join.class);
            String fk = joinAnn.fk();
            String refFK = joinAnn.refFK();
            DeclaredType dt = (DeclaredType) field.asType();
            TypeElement te = (TypeElement) dt.asElement();
            String table = NamingConvertUtil.camel2SnakeCase(te.getSimpleName().toString());
            String parentAlias;
            int idx = ji.attrPath.lastIndexOf('.');
            if (idx > 0) parentAlias = pathAlias.get(ji.attrPath.substring(0, idx));
            else parentAlias = "t0";
            defs.add(CodeBlock.of("new $T($S, $S, $S, $S, $S)",
                    JoinDef.class,
                    table, fk, refFK, ji.alias, parentAlias)
            );
        }

        cb.addStatement("$T.register($T.class, $S, new $T( $T.of( $L ) ))",
                DTOJoinInfoFactory.class, entityType, dtoId,
                DTOJoinInfo.class, List.class,
                CodeBlock.join(defs, ", "));

        return cb.build();
    }

    private String getPropNameFromJoinLevel(JoinLevel jl) {
        if (!jl.attrName().isEmpty()) return jl.attrName();
        TypeMirror tm;
        try { jl.clz(); return ""; }
        catch (MirroredTypeException mte) { tm = mte.getTypeMirror(); }
        DeclaredType dt = (DeclaredType) tm;
        TypeElement te = (TypeElement) dt.asElement();
        return decapitalize(te.getSimpleName().toString());
    }

    private String getPropNameFromNext(Next nx) {
        if (!nx.attrName().isEmpty()) return nx.attrName();
        TypeMirror tm;
        try { nx.clz(); return ""; }
        catch (MirroredTypeException mte) { tm = mte.getTypeMirror(); }
        DeclaredType dt = (DeclaredType) tm;
        TypeElement te = (TypeElement) dt.asElement();
        return decapitalize(te.getSimpleName().toString());
    }

    private VariableElement resolveJoinField(TypeElement root, String path) {
        String[] segs = path.split("\\.");
        TypeElement current = root;
        VariableElement found = null;
        for (int i = 0; i < segs.length; i++) {
            found = null;
            for (Element e : current.getEnclosedElements()) {
                if (e.getKind() == ElementKind.FIELD && e.getSimpleName().contentEquals(segs[i])) {
                    found = (VariableElement) e; break;
                }
            }
            if (found == null) return null;
            if (i < segs.length - 1) current = (TypeElement) ((DeclaredType) found.asType()).asElement();
        }
        return found;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String decapitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        if (s.length() > 1 && Character.isUpperCase(s.charAt(0)) && Character.isUpperCase(s.charAt(1)))
            return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    // 参数信息
    private static class ParamInfo {
        VariableElement param;
        boolean isJoin;
        String prefix;
        String base;
        String fieldName;
        JoinLevel jl;
        Next nx;
    }

    // join子句的信息
    @Value
    @AllArgsConstructor
    private static class JoinInfo {
        String attrPath;
        String alias;
    }
}
