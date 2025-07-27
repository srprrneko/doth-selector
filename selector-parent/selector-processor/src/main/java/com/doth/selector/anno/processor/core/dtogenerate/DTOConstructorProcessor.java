package com.doth.selector.anno.processor.core.dtogenerate;

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
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>生成DTO的核心类</p>
 * <p></p>
 * <p>使用注意事项</p>
 * <ol>
 *     <li>DTO 构造声明如果没有特殊需要, 尽量私有修饰, 避免无效暴露</li>
 *     <li>局部变量封装到一个 DTOGenaContext, 带一个方法 init 完成准备阶段</li>
 * </ol>
 * <hr/>
 * <p>后续优化思路</p>
 * <ol>
 *     <li>优化局部变量 封装到 DTOGenaContext 生成上下文内, 带一个方法 init 完成准备阶段</li>
 * </ol>
 * <hr/>
 * <p>后续拓展可能</p>
 * <ol>
 *     <li>拓展支持构造中解析 record 类, 旨在保持类中构造的简洁</li>
 *     <li>拓展支持方法构建 DTO, 同为可执行元素</li>
 * </ol>
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("com.doth.selector.anno.DTOConstructor")
public class DTOConstructorProcessor extends BaseAnnotationProcessor {

    // 生成入口, 作用于调用generateDto前的类型过滤和准备
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 遍历编译期间环境, 获取DTOConstructor标记的元素
        for (Element elem : roundEnv.getElementsAnnotatedWith(DTOConstructor.class)) {
            if (!(elem instanceof ExecutableElement)) continue; // 过滤

            // 获取构造元素, 以及所属类, dtoId
            ExecutableElement ctor = (ExecutableElement) elem;
            TypeElement entity = (TypeElement) ctor.getEnclosingElement(); // 解析AST获取构造所属类
            String dtoId = ctor.getAnnotation(DTOConstructor.class).id();
            boolean isAutoPrefix = ctor.getAnnotation(DTOConstructor.class).autoPrefix();

            generateDto(entity, ctor, dtoId, isAutoPrefix);
        }
        return true;
    }

    private void generateDto(TypeElement entityClass, ExecutableElement ctorElem, String dtoId, boolean isAutoPrefix) {
        // 1. 准备阶段
        String pkg = context.getElementUtils() // 准备包名
                .getPackageOf(entityClass)
                .getQualifiedName().toString();
        // 通过包名结合类名 获取类类型 (虽然叫做 ClassName 但是同时可以获取全限定名也可以获取类类型)
        ClassName entityType = ClassName.get(pkg, entityClass.getSimpleName().toString());
        // 将 dtoId 转大小成dto的类名
        String dtoName = NamingConvertUtil.upperFstLetter(dtoId, false);
        // 准备类的 基本结构, 公开, lombok自动生成getter, setter, 以及dependOn注解
        TypeSpec.Builder cls = TypeSpec.classBuilder(dtoName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(DependOn.class)
                        .addMember("clzPath", "$S", context.getElementUtils()
                                .getBinaryName(entityClass).toString()) // 获取二进制全名
                        .build())
                .addAnnotation(Data.class)
                .addAnnotation(NoArgsConstructor.class)
                .addAnnotation(AllArgsConstructor.class);

        // 2.
        List<ParamInfo> params = parseParams(ctorElem, isAutoPrefix, entityType);
        for (ParamInfo info : params) {
            cls.addField(FieldSpec.builder(
                    TypeName.get(info.param.asType()),
                    info.finalFName,
                    Modifier.PRIVATE
            ).build());
        }

        cls.addMethod(buildConstructor(entityClass, params));

        JoinChainResult chain = processJoinChains(params, entityClass);

        cls.addStaticBlock(buildStaticInitBlock(
                entityType, pkg, dtoId,
                chain.selectFields, chain.joinInfos, entityClass
        ));

        try {
            JavaFile.builder(pkg, cls.build())
                    .build()
                    .writeTo(context.getFiler());
        } catch (IOException e) {
            context.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "生成 DTO 类失败: " + e.getMessage(),
                    entityClass
            );
        }
    }

    /**
     * 解析可执行函数 (方法/构造器) 中的参数
     * <p>主要说明</p>
     * <ol>
     *     <li>主表名称不能包含 '_' 符号</li>
     *     <li>'_' 符号表示 递进的 关系, 而非蛇形命名</li>
     *     <li>最终DTO生成的 字段受类字段命名 影响, 如果主表与从表字段<strong>命名相同</strong>, 则使用 递进符 '_' 前的字符作为前缀进行命名; 如果不相同, 则去掉递进符转驼峰作为字段命名</li>
     * </ol>
     * <hr/>
     * <p>工作流程</p>
     * <p>1. </p>
     *
     * @param exeEle       可执行元素, 后续可能拓展方法
     * @param isAutoPrefix
     * @param entityClz
     * @return 参数信息集合
     */
    private List<ParamInfo> parseParams(ExecutableElement exeEle, boolean isAutoPrefix, ClassName entityClz) {
        // 准备阶段
        // 1. 通过可执行元素 获取所有声明的 参数
        List<? extends VariableElement> parameters = exeEle.getParameters();

        Set<String> mainTNames = new HashSet<>(); // 主表单例名称集合 (不带'_'的)
        Map<String, Integer> sameName2Count = new HashMap<>(); // 用于为不同层级命名重复的情况加上层级计数

        for (VariableElement p : parameters) {
            String rawName = p.getSimpleName().toString();

            // 通过是否包含 '_' 判断是否为 主表, 使用mainTNames集合收集主表字段
            if (!rawName.contains("_")) mainTNames.add(rawName);
            // 收集从表
            else {
                String realName = rawName.substring(rawName.indexOf('_') + 1);
                sameName2Count.put(
                        realName,
                        sameName2Count.getOrDefault(realName, 0) + 1
                );
            }
        }

        List<ParamInfo> result = new ArrayList<>();
        boolean inChain = false;
        for (VariableElement p : parameters) {
            ParamInfo info = new ParamInfo(p);

            if (!info.rawArgName.contains("_")) {
                // inChain = false;
                info.init4JNormalMod(p);
            } else {

                info.init4JoinMod(p);


                // 是否是当前层级的 起始字段
                // if (info.jl != null || info.nx != null) {
                //     inChain = true;
                // } else if (!inChain) {
                //     info.isJoin = false;
                //     info.finalFName = info.rawArgName;
                // }
                //
                // if (info.isJoin) {
                    boolean conflict = sameName2Count.getOrDefault(info.originName, 0) > 1;
                    info.finalFName = conflict
                            ? info.prefix + NamingConvertUtil.upperFstLetter(info.originName, false)
                            : info.originName;
                // }
            }
            result.add(info);
        }
        return result;
    }

    private MethodSpec buildConstructor(TypeElement entity, List<ParamInfo> params) {
        String var = NamingConvertUtil.lowerFstLetter(entity.getSimpleName().toString(), false);
        MethodSpec.Builder mb = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(entity), var);

        Map<String, String> prefixPath = new HashMap<>();
        boolean chain = false;
        String currentPath = null;

        for (ParamInfo info : params) {
            String fn = info.finalFName, raw = info.param.getSimpleName().toString();
            if (!info.isJoin) {
                mb.addStatement("this.$L = $L.get$L()",
                        fn, var, NamingConvertUtil.upperFstLetter(raw, false));
            } else {
                if (info.jl != null || !chain) {
                    chain = true;
                    prefixPath.clear();
                    currentPath = var + ".get"
                            + NamingConvertUtil.upperFstLetter(getPropName(info.jl), false) + "()";
                    prefixPath.put(info.prefix, currentPath);
                } else if (info.nx != null) {
                    String prev = currentPath;
                    currentPath = prev + ".get"
                            + NamingConvertUtil.upperFstLetter(getPropName(info.nx), false) + "()";
                    prefixPath.put(info.prefix, currentPath);
                }
                String path = prefixPath.get(info.prefix);
                if (path != null) {
                    mb.addStatement("this.$L = $L.get$L()",
                            fn, path, NamingConvertUtil.upperFstLetter(info.originName, false));
                } else {
                    mb.addComment("未定义关联前缀 '$L' 对应字段 $L",
                            info.prefix, fn);
                }
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
                    currentAttr = info.jl != null
                            ? getPropName(info.jl)
                            : info.prefix;
                    joins.add(new JoinInfo(currentAttr, alias));
                    selects.add(alias + "." + info.originName);
                } else if (info.nx != null) {
                    String alias = "t" + idx++;
                    prefixAlias.put(info.prefix, alias);
                    currentAttr = currentAttr + "." + getPropName(info.nx);
                    joins.add(new JoinInfo(currentAttr, alias));
                    selects.add(alias + "." + info.originName);
                } else if (prefixAlias.containsKey(info.prefix)) {
                    selects.add(prefixAlias.get(info.prefix) + "." + info.originName);
                } else {
                    selects.add("t?." + info.originName);
                }
            }
        }

        // filter invalid join paths
        List<JoinInfo> valid = joins.stream()
                .filter(j -> resolveJoinField(entityClass, j.getAttrPath()) != null)
                .collect(Collectors.toList());

        return new JoinChainResult(selects, valid);
    }

    private CodeBlock buildStaticInitBlock(
            ClassName entityType,
            String pkg,
            String dtoId,
            List<String> selectFields,
            List<JoinInfo> joinInfos,
            TypeElement entityClass
    ) {
        CodeBlock.Builder cb = CodeBlock.builder()
                .addStatement("$T.register($T.class, $S, $T.class)",
                        DTOFactory.class, entityType, dtoId,
                        ClassName.get(pkg,
                                NamingConvertUtil.upperFstLetter(dtoId, false)))
                .addStatement("$T __select = new $T<>()",
                        ParameterizedTypeName.get(List.class, String.class),
                        ArrayList.class);

        for (String f : selectFields) {
            cb.addStatement("__select.add($S)", f);
        }
        cb.addStatement("$T.register($T.class, $S, __select)",
                DTOSelectFieldsListFactory.class, entityType, dtoId);

        // prepare join definitions
        Map<String, String> pathAlias = new LinkedHashMap<>();
        for (JoinInfo ji : joinInfos) {
            pathAlias.put(ji.getAttrPath(), ji.getAlias());
        }

        List<CodeBlock> defs = new ArrayList<>();
        for (JoinInfo ji : joinInfos) {
            VariableElement field = resolveJoinField(entityClass, ji.getAttrPath());
            Join ann = field.getAnnotation(Join.class);
            String table = NamingConvertUtil.camel2Snake(
                    ((TypeElement)
                            ((DeclaredType) field.asType())
                                    .asElement()
                    ).getSimpleName().toString()
            );
            int idxDot = ji.getAttrPath().lastIndexOf('.');
            String parent = idxDot > 0
                    ? pathAlias.get(
                    ji.getAttrPath().substring(0, idxDot))
                    : "t0";
            defs.add(CodeBlock.of("new $T($S,$S,$S,$S,$S)",
                    JoinDefInfo.class,
                    table, ann.fk(), ann.refPK(),
                    ji.getAlias(), parent
            ));
        }
        cb.addStatement("$T.register($T.class, $S, new $T($T.of($L)))",
                DTOJoinInfoFactory.class, entityType, dtoId,
                DTOJoinInfo.class, List.class,
                CodeBlock.join(defs, ", "));
        return cb.build();
    }

    /**
     * 统一解析注解属性：优先使用 explicit，否则通过捕获 MirroredTypeException 获取类名
     */
    private String resolvePropName(String explicit, Runnable clzCall) {
        if (!explicit.isEmpty()) {
            return explicit;
        }
        try {
            clzCall.run();
        } catch (MirroredTypeException m) {
            TypeElement te = (TypeElement) ((DeclaredType) m.getTypeMirror()).asElement();
            return NamingConvertUtil.lowerFstLetter(te.getSimpleName().toString(), false);
        }
        return "";
    }

    private String getPropName(JoinLevel jl) {
        return resolvePropName(jl.attrName(), () -> jl.clz());
    }

    private String getPropName(Next nx) {
        return resolvePropName(nx.attrName(), () -> nx.clz());
    }

    private VariableElement resolveJoinField(TypeElement root, String path) {
        String[] segs = path.split("\\.");
        TypeElement cur = root;
        VariableElement found = null;
        for (int i = 0; i < segs.length; i++) {
            found = null;
            for (Element e : cur.getEnclosedElements()) {
                if (e.getKind() == ElementKind.FIELD
                        && e.getSimpleName().contentEquals(segs[i])) {
                    found = (VariableElement) e;
                    break;
                }
            }
            if (found == null) {
                return null;
            }
            if (i < segs.length - 1) {
                cur = (TypeElement) ((DeclaredType) found.asType()).asElement();
            }
        }
        return found;
    }
}
