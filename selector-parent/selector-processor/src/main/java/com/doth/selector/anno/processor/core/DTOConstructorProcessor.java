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

        // 准备 DTO 类构建器，添加 @DependOn、@Data、@NoArgsConstructor、@AllArgsConstructor
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

        // 收集构造方法参数及字段元信息
        List<? extends VariableElement> params = ctorElem.getParameters();
        List<FieldMeta> fields = new ArrayList<>();
        Set<String> mainNames = new HashSet<>();
        Map<String, Long> baseCount = new HashMap<>();
        boolean inJoinChain = false;
        String chainAttrName = "";

        // 1) 先统计主表字段、冲突计数
        for (VariableElement p : params) {
            String n = p.getSimpleName().toString();
            if (!n.contains("_")) {
                mainNames.add(n);
            } else {
                String base = n.substring(n.indexOf('_') + 1);
                baseCount.put(base, baseCount.getOrDefault(base, 0L) + 1);
            }
        }

        // 2) 按原逻辑生成 DTO 字段声明
        for (VariableElement p : params) {
            String n = p.getSimpleName().toString();
            String fieldName;
            if (!n.contains("_")) {
                // 主表字段
                fieldName = n;
                inJoinChain = false;
                chainAttrName = "";
            } else {
                // join 链字段
                String prefix = n.substring(0, n.indexOf('_'));
                String base = n.substring(n.indexOf('_') + 1);
                JoinLevel jl = p.getAnnotation(JoinLevel.class);
                Next nx = p.getAnnotation(Next.class);

                if (jl != null) {
                    inJoinChain = true;
                    chainAttrName = getPropNameFromJoinLevel(jl);
                } else if (nx != null) {
                    inJoinChain = true;
                    chainAttrName = getPropNameFromNext(nx);
                } else if (!inJoinChain) {
                    // 非 join 链中的下划线字段，当普通字段处理
                    fieldName = n;
                    TypeName t = TypeName.get(p.asType());
                    classB.addField(FieldSpec.builder(t, fieldName, Modifier.PRIVATE).build());
                    fields.add(new FieldMeta(p, fieldName));
                    continue;
                }

                // 冲突检测
                boolean conflict = mainNames.contains(base)
                        || baseCount.getOrDefault(base, 0L) > 1;
                fieldName = conflict
                        ? prefix + capitalize(base)
                        : base;
            }

            TypeName t = TypeName.get(p.asType());
            classB.addField(FieldSpec.builder(t, fieldName, Modifier.PRIVATE).build());
            fields.add(new FieldMeta(p, fieldName));
        }

        // 构造方法：public DTO(Entity entity) { ... }
        String varEntity = decapitalize(entityClass.getSimpleName().toString());
        MethodSpec.Builder ctorB = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(entityType, varEntity);

        // 构造体中赋值（保持原有逻辑）
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
                // join 链中字段
                String prefix = pn.substring(0, pn.indexOf('_'));
                String base = pn.substring(pn.indexOf('_') + 1);
                JoinLevel jl = p.getAnnotation(JoinLevel.class);
                Next nx = p.getAnnotation(Next.class);

                String prop;
                if (jl != null) {
                    prop = getPropNameFromJoinLevel(jl);
                } else if (nx != null) {
                    prop = getPropNameFromNext(nx);
                } else {
                    prop = prefix;
                }

                if (jl != null || !chain) {
                    chain = true;
                    prefixPath.clear();
                    currentPath = varEntity + ".get" + capitalize(prop) + "()";
                    prefixPath.put(prefix, currentPath);
                } else if (nx != null) {
                    String prev = currentPath;
                    currentPath = prev + ".get" + capitalize(prop) + "()";
                    prefixPath.put(prefix, currentPath);
                }

                if (prefixPath.containsKey(prefix)) {
                    String getter = "get" + capitalize(base);
                    ctorB.addStatement("this.$L = $L.$L()", fn, prefixPath.get(prefix), getter);
                } else {
                    ctorB.addComment("未定义的关联前缀 '$L' 对应字段 $L", prefix, fn);
                }
            }
        }
        classB.addMethod(ctorB.build());

        // —— 新增：收集所有 join 链的信息，用于生成 JoinDef ——
        class JoinInfo {
            final String attrName, alias;

            JoinInfo(String a, String b) {
                this.attrName = a;
                this.alias = b;
            }
        }
        List<JoinInfo> joinInfos = new ArrayList<>();
        int aliasIdx2 = 1;
        boolean chainFlag = false;
        Map<String, String> prefixAlias2 = new HashMap<>();

        for (VariableElement p : params) {
            String pn = p.getSimpleName().toString();
            if (!pn.contains("_")) continue;

            String prefix = pn.substring(0, pn.indexOf('_'));
            JoinLevel jl = p.getAnnotation(JoinLevel.class);
            Next nx = p.getAnnotation(Next.class);
            String alias, attrName;

            if (jl != null || !chainFlag) {
                alias = "t" + aliasIdx2++;
                prefixAlias2.clear();
                prefixAlias2.put(prefix, alias);
                chainFlag = true;
                // 第一次，path 就是 department 这一级
                currentPath = (jl != null)
                        ? getPropNameFromJoinLevel(jl)
                        : prefix;
                joinInfos.add(new JoinInfo(currentPath, alias));
            } else if (nx != null) {
                alias = "t" + aliasIdx2++;
                prefixAlias2.put(prefix, alias);
                // 关键：path 要在上一个 path 后面加上 .company
                String nextName = getPropNameFromNext(nx);
                currentPath = currentPath + "." + nextName;
                joinInfos.add(new JoinInfo(currentPath, alias));
            }
        }

        // 静态注册块：注册 DTO 类、select 字段，以及 join 信息
        CodeBlock.Builder staticB = CodeBlock.builder()
                // 注册 DTOFactory
                .addStatement("$T.register($T.class, $S, $T.class)",
                        DTOFactory.class, entityType, dtoId,
                        ClassName.get(packageName, dtoClassName))

                // 构造并注册 select fields 列表
                .addStatement("$T __selectFields = new $T<>()",
                        ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class)),
                        ClassName.get(ArrayList.class));
        // （原有 __selectFields.add(...) 生成逻辑，不做任何改动）
        int aliasIdx = 1;
        boolean chain2 = false;
        Map<String, String> prefixAlias = new HashMap<>();
        for (VariableElement p : params) {
            String pn = p.getSimpleName().toString();
            if (!pn.contains("_")) {
                staticB.addStatement("__selectFields.add($S)", "t0." + pn);
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


        List<JoinInfo> validJoinInfos = joinInfos.stream()
                .filter(ji -> resolveJoinField(entityClass, ji.attrName) != null)
                .collect(Collectors.toList());

        // —— 新增：注册 JoinDef 列表 到 DTOJoinInfoFactory ——
        // staticB.add("$T.register($T.class, $S, new $T(\n",
        //         DTOJoinInfoFactory.class, entityType, dtoId,
        //         DTOJoinInfo.class);
        // staticB.add("$T.of(\n", List.class);
        List<CodeBlock> blocks = new ArrayList<>();
        for (JoinInfo ji : validJoinInfos) {
            VariableElement joinField = resolveJoinField(entityClass, ji.attrName);
            Join joinAnn = joinField.getAnnotation(Join.class);
            String fk = joinAnn.fk();
            String refFK = joinAnn.refFK();
            DeclaredType dt = (DeclaredType) joinField.asType();
            TypeElement te = (TypeElement) dt.asElement();
            String tableName = NamingConvertUtil.camel2SnakeCase(te.getSimpleName().toString());

            blocks.add(CodeBlock.of(
                    "new $T($S, $S, $S, $S)",
                    JoinDef.class, tableName, fk, refFK, ji.alias
            ));
        }
        CodeBlock joined = CodeBlock.join(blocks, ",\n");

        // 3. 把它塞进 static 初始化块
        staticB.add(
                "$T.register($T.class, $S, new $T(\n",
                DTOJoinInfoFactory.class, entityType, dtoId, DTOJoinInfo.class
        );
        staticB.add("    $T.of(\n", List.class);
        staticB.add("      $L\n", joined);
        staticB.add("    )\n");
        staticB.add("));\n");
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
     * 解析类似 "department.company.location" 的任意深度链路，
     * 在每一级 TypeElement 里去找字段，最后返回最末级的 VariableElement。
     */
    private VariableElement resolveJoinField(TypeElement rootType, String attrPath) {
        String[] segments = attrPath.split("\\.");
        TypeElement currentType = rootType;
        VariableElement found = null;
        for (int i = 0; i < segments.length; i++) {
            String name = segments[i];
            found = null;
            for (Element e : currentType.getEnclosedElements()) {
                if (e.getKind() == ElementKind.FIELD
                        && e.getSimpleName().toString().equals(name)) {
                    found = (VariableElement) e;
                    break;
                }
            }
            if (found == null) return null;
            // 如果还有下一级，就往下 drill
            if (i < segments.length - 1) {
                DeclaredType dt = (DeclaredType) found.asType();
                currentType = (TypeElement) dt.asElement();
            }
        }
        return found;
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
