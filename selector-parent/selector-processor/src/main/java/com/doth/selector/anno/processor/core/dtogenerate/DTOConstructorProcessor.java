package com.doth.selector.anno.processor.core.dtogenerate;

import com.doth.selector.anno.*;
import com.doth.selector.anno.Name;
import com.doth.selector.anno.processor.BaseAnnotationProcessor;
import com.doth.selector.common.dto.*;
import com.doth.selector.common.util.NamingConvertUtil;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.doth.selector.anno.processor.core.dtogenerate.JNNameResolver.getOrD4JNLevelAttrName;


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
@SupportedAnnotationTypes("com.doth.selector.anno.MorphCr")
@Slf4j
public class DTOConstructorProcessor extends BaseAnnotationProcessor {

    // 生成入口, 作用于调用generateDto前的类型过滤和准备
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 遍历编译期间环境, 获取DTOConstructor标记的元素
        for (Element elem : roundEnv.getElementsAnnotatedWith(MorphCr.class)) {
            if (!(elem instanceof ExecutableElement)) continue; // 过滤

            // 获取构造元素, 以及所属类, dtoId
            ExecutableElement ctor = (ExecutableElement) elem;
            TypeElement entity = (TypeElement) ctor.getEnclosingElement(); // 解析AST获取构造所属类
            String dtoId = ctor.getAnnotation(MorphCr.class).id();
            boolean isAutoPrefix = ctor.getAnnotation(MorphCr.class).autoPrefix();

            generateDto(entity, ctor, dtoId, isAutoPrefix);
        }
        return true;
    }

    private void generateDto(TypeElement entityClass, ExecutableElement ctorElem, String dtoId, boolean isAutoPrefix) {
        // 1. 准备阶段
        String pkg = context.getElementUtils() // 准备所属类包名, 用于获取所属类
                .getPackageOf(entityClass).getQualifiedName().toString();
        // 通过包名结合类名 获取类类型 (ClassName 可以获取全限定名也可以获取类类型, 非表面意思)
        ClassName entityType = ClassName.get(pkg, entityClass.getSimpleName().toString());
        // 将 dtoId 转大小成dto的类名
        String dtoName = NamingConvertUtil.upperFstLetter(dtoId, false);
        // 准备类的 基本结构, 公开, lombok自动生成getter, setter, 以及dependOn注解
        TypeSpec.Builder cls = TypeSpec.classBuilder(dtoName).addModifiers(Modifier.PUBLIC).addAnnotation(AnnotationSpec.builder(DependOn.class).addMember("clzPath", "$S", context.getElementUtils().getBinaryName(entityClass).toString()) // 获取二进制全名
                .build()).addAnnotation(Data.class).addAnnotation(NoArgsConstructor.class).addAnnotation(AllArgsConstructor.class);

        // 2. 解析构造声明参数 并 写入字段
        List<ParamInfo> params = parseParams(ctorElem, isAutoPrefix, entityType);
        // 遍历params在类中加入字段
        for (ParamInfo info : params) {
            // 根据是否@Name判断替换生成的字段
            String fieldName = info.showName != null ? info.showName : info.finalFName;
            cls.addField(FieldSpec.builder(TypeName.get(info.param.asType()), fieldName, Modifier.PRIVATE).build());
        }

        // 3. 写入构造方法
        cls.addMethod(buildConstructor(entityClass, params));

        // 4. 处理并返回链信息
        ParamChainInfo chain = parseParam4StaticBlock(params, entityClass);

        // 5. 通过链信息构建静态代码块的注册信息
        cls.addStaticBlock(
                buildStaticBlock(entityType, pkg, dtoId, chain)
        );

        try {
            JavaFile.builder(pkg, cls.build())
                    .build()
                    .writeTo(context.getFiler());
        } catch (IOException e) {
            context.getMessager().printMessage(Diagnostic.Kind.ERROR, "生成 DTO 类失败: " + e.getMessage(), entityClass);
        }
    }

    /**
     * 解析可执行函数 (方法/构造器) 中的参数, 从普通参数到 >> paramInfo 的转换
     * <p>主要说明</p>
     * <ol>
     *     <li>主表名称不能包含 '_' 符号</li>
     *     <li>'_' 符号表示 递进符, 用于表示递进到这层寻找字段</li>
     *     <li>最终DTO生成的 字段受类字段命名 影响, 如果主表与从表字段<strong>命名相同</strong>, 则使用 递进符 '_' 前的字符作为前缀进行命名; 如果不相同, 则去掉递进符转驼峰作为字段命名, 你可以使用 selector.anno.@Name 注解来解决这个问题, 它会原样返回value中的值</li>
     * </ol>
     * <hr/>
     * <p>工作流程</p>
     * <p>1. 第一次遍历填充 processed, sameName2Count 两个集合, 一个用于标记处理过的信息, 一个用于给空前缀 自动前缀添加</p>
     * <p>2. 第二次遍历用于将参数解析成 paramInfo 信息, 并做空前缀自动添加前缀 的逻辑, 以及根据 isAutoPrefix 判断是否需要强制添加前缀</p>
     * <hr/>
     *
     * @param exeEle       可执行元素, 后续可能拓展方法
     * @param isAutoPrefix 是否自动别名
     * @param entityClz    预留
     * @return 参数信息集合
     */
    private List<ParamInfo> parseParams(ExecutableElement exeEle, boolean isAutoPrefix, ClassName entityClz) {
        // 准备阶段
        // 1. 通过可执行元素 获取所有声明的 参数
        List<? extends VariableElement> parameters = exeEle.getParameters();

        Set<String> processedName = new HashSet<>(); // 主表单例名称集合 (不带'_'的)
        Map<String, Integer> sameName2Count = new HashMap<>(); // 用于为无前缀参数名 (_name) 自动按照tN..格式起名进行去重

        for (VariableElement p : parameters) {
            String rawName = p.getSimpleName().toString();

            // 通过是否包含 '_' 判断是否为 主表, 使用mainTNames集合收集主表字段
            processedName.add(rawName);
            // 收集重复的参数名
            if (rawName.contains("_")) {
                String realName = rawName.substring(rawName.indexOf('_') + 1);
                sameName2Count.put(
                        realName,
                        sameName2Count.getOrDefault(realName, 0) + 1
                );
            }
        }

        // 准备最终返回的结果集
        List<ParamInfo> result = new ArrayList<>();
        for (VariableElement p : parameters) {
            // 初始化参数信息
            ParamInfo info = new ParamInfo(p);
            // 主表分支
            if (!info.rawArgName.contains("_")) {
                info.init4JNormalMod(p);
            } else { // 从表分支
                info.init4JoinMod(p);
                // 空前缀自动添加前缀防止命名冲突, 例: employee{name, department: {name}}   declare in dto-constructor:  _name >> t1Name
                if (info.prefix.equals("")) { // 也可以isEmpty, 不能 == ""
                    info.prefix = "t" + sameName2Count.get(info.originName).toString();
                }
                Name name = p.getAnnotation(Name.class);
                if (name != null) {
                    info.showName = name.value();
                }

                boolean conflict = processedName.contains(info.originName);
                boolean needPrefix = conflict || !isAutoPrefix;

                info.finalFName = needPrefix
                        ? info.prefix + NamingConvertUtil.upperFstLetter(info.originName, false)
                        : info.originName;
            }
            result.add(info);
        }
        return result;
    }

    /**
     * 基于参数以及实体 信息 构建构造方法
     * <p>注意事项</p>
     * <ol>
     *     <li>层级前缀 会沿用至 当前层级所有字段, 请保持一致, 如有特殊字段命名需求请使用 selector.anno.Name 注解</li>
     * </ol>
     * <p>以防 你在理解的时候总是翻来覆去..便于理解的生成样板</p>
     * <pre><code>
     * public BaseEmpInfo(Employee employee) {
     *     this.id = employee.getId();
     *     this.name = employee.getName();
     *     this.deId = employee.getDepartment().getId();
     *     this.deName = employee.getDepartment().getName();
     *     this.mngCompanyName = employee.getDepartment().getCompany().getName();
     * }
     * </code></pre>
     *
     * @param entity 实体类
     * @param params 参数信息
     * @return 写好的构造方法 methodSpec 对象
     */
    private MethodSpec buildConstructor(TypeElement entity, List<ParamInfo> params) {
        // 1. 准备阶段
        // 准备小写所属类名; 用于构建参数名,
        String masterName =
                NamingConvertUtil.lowerFstLetter(entity.getSimpleName().toString(), false);
        // 准备构造方法签名参数
        MethodSpec.Builder mb = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(entity), masterName);

        // 2. 写入方法体逻辑
        // 准备阶段
        Map<String, String> prefixPath = new HashMap<>();
        boolean chain = false;
        String currentPath = null;

        for (ParamInfo info : params) {
            // 获取声明的字段名
            String fn = info.showName != null ? info.showName : info.finalFName;

            // 处理主表分支
            if (!info.isJoin) {
                //              this.id = employee.getId();
                mb.addStatement("this.$L = $L.get$L()", fn, masterName, NamingConvertUtil.upperFstLetter(info.rawArgName, false));
            } else { // 处理从表分支

                // 特殊处理 层级起点字段 加上 例: getDepartment()
                if (info.jl != null || !chain) { // join 层级, 获取单层路径
                    // 当前层级延续
                    chain = true;
                    // 新层级则清空上一层级
                    prefixPath.clear();

                    assert info.jl != null;
                    currentPath = masterName + ".get" // 通过当前层级起步参数上的 @JoinLevel 获取当前路径, employee.get
                            + NamingConvertUtil.upperFstLetter(getOrD4JNLevelAttrName(info.jl), false) // Department
                            + "()"; // together: employee.getDepartment()

                    prefixPath.put(info.prefix, currentPath);
                } else if (info.nx != null) { // next 层级, 延续上一个路径
                    // 基于上一个路径延伸至当前路径
                    String prev = currentPath;
                    currentPath = prev + ".get" // employee.getDepartment().get
                            + NamingConvertUtil.upperFstLetter(getOrD4JNLevelAttrName(info.nx), false) // Company
                            + "()"; // together: employee.getDepartment().getCompany()

                    prefixPath.put(info.prefix, currentPath);
                }
                // 最后再拼接原字段名
                String path = prefixPath.get(info.prefix);
                mb.addStatement("this.$L = $L.get$L()", fn, path, NamingConvertUtil.upperFstLetter(info.originName, false));
            }
        }
        return mb.build();
    }

    /**
     * 将参数信息解析成 链信息, 服务于静态代码块, 工厂注册逻辑
     *
     * @param params      参数信息
     * @param entityClass 所属类
     * @return 参数链信息
     */
    private ParamChainInfo parseParam4StaticBlock(List<ParamInfo> params, TypeElement entityClass) {
        // 1. 准备阶段
        ChainProcessContext context = new ChainProcessContext();

        // 2. 遍历参数提取链信息
        for (ParamInfo info : params) {
            // 处理主表参数分支
            if (!info.isJoin) {
                // 直接t0.+ 原名
                context.selectColList.add("t0." + info.rawArgName);
            } else { // 处理非主表参数分支

                // 特殊处理 层级起点字段 加上 例: getDepartment()
                if (info.jl != null || !context.chain) {// join 层级, 获取单层路径
                    context.startNewChain(info);
                } else if (info.nx != null) { // next 层级, 延续上一个路径
                    context.extendCurrentChain(info);
                } else if (context.prefix2Alias.containsKey(info.prefix)) { // 处理非起点字段分支
                    context.selectColList.add(context.prefix2Alias.get(info.prefix) + "." + info.originName);
                } else { // 错误分支
                    context.selectColList.add("t?." + info.originName);
                }
            }
        }

        // 3. 过滤出有效的joinInfo
        List<VariableElement> validVarEls = new ArrayList<>();
        List<JoinInfo> validJI = context.joinInfos.stream()
                .filter(j -> {
                            VariableElement validF = resolvePath(entityClass, j.getAttrName());
                            validVarEls.add(validF);

                            return validF != null;
                        }
                ).collect(Collectors.toList());

        return new ParamChainInfo(context.selectColList, validJI, validVarEls);
    }


    /**
     * 主要通过 parseParam4StaticBlock 收集的 ParamChainInfo 信息构建静态代码块; 注册 DTO工厂, 查询列列表工厂, join子句工厂
     *
     * @param entityType 原实体类型, 主要作用于工厂注册的 键
     * @param pkg        包名
     * @param dtoId      dtoId
     * @param chainInfo  从 parseParam4StaticBlock 方法提取的 链信息
     * @return 填充好的 CodeBlock
     */
    private CodeBlock buildStaticBlock(ClassName entityType, String pkg, String dtoId, ParamChainInfo chainInfo) {
        CodeBlock.Builder cb = CodeBlock.builder()
                // 1.注册DTOFactory
                .addStatement("$T.register($T.class, $S, $T.class)",
                        DTOFactory.class,
                        entityType,
                        dtoId,
                        ClassName.get(
                                pkg,
                                NamingConvertUtil.upperFstLetter(dtoId, false)
                        )
                )
                // 2. 初始化list用于DTOSelectFieldsListFactory.register做准备
                .addStatement("$T __select = new $T<>()",
                        ParameterizedTypeName.get(List.class, String.class),
                        ArrayList.class
                );

        // 3. 收集查询列
        for (String f : chainInfo.selectColList) {
            cb.addStatement("__select.add($S)", f);
        }
        // 4. 注册查询列列表工厂
        cb.addStatement("$T.register(" +
                        "$T.class, $S, __select" +
                        ")",
                DTOSelectFieldsListFactory.class,
                entityType, dtoId
        );

        // 5. 注册joinInfo工厂
        // 先准备代码块集合, 留到结尾统一添加, 确保格式美观
        List<CodeBlock> defs = new ArrayList<>();
        for (int i = 0; i < chainInfo.joinInfos.size(); i++) {
            // 共享索引, 准备用于 创建 JoinInfo
            VariableElement field = chainInfo.validVarEls.get(i);
            JoinInfo ji = chainInfo.joinInfos.get(i);

            Join join = field.getAnnotation(Join.class);
            // 直接获取已分配好的别名, 如果没有则 t0
            String bindAlias = ji.getAlias() == null ? "t0" : ji.getAlias();

            // 获取表名
            String tName = resolveTableName(field);
            defs.add(
                    CodeBlock.of("new $T($S,$S,$S,$S,$S)",
                            JoinDefInfo.class,
                            tName, join.fk(), join.refPK(), ji.getAlias(), bindAlias
                    )
            );
        }
        CodeBlock listOfBlock = CodeBlock.builder()
                .add("List.of(\n")
                .indent()
                .add(CodeBlock.join(defs, ",\n"))
                .unindent()
                .add("\n)")
                .build();

        CodeBlock dtoJoinInfoBlock = CodeBlock.builder()
                .add("new $T(\n", DTOJoinInfo.class)
                .indent()
                .add("$L\n", listOfBlock)
                .unindent()
                .add(")")
                .build();

        // 最终 .register 调用
        cb.add(
                CodeBlock.builder()
                        .add("$T.register(\n", DTOJoinInfoFactory.class)
                        .indent()
                        .add("$T.class,\n", entityType)
                        .add("$S,\n", dtoId)
                        .add("$L\n", dtoJoinInfoBlock)
                        .unindent()
                        .add(");\n") // 结尾
                        .build()
        );
        return cb.build();
    }

    /**
     * 根据字段获取对应实体类的表名, 优先读取 @TableName 注解的 value 值
     * <p>后续拓展</p>
     * <ol>
     *     <li>支持 暂时废弃的selector.anno.TableName 注解的支持</li>
     * </ol>
     *
     * @param field 字段元素
     * @return 表名
     */
    private String resolveTableName(VariableElement field) {
        // 获取字段类型对应的类元素
        TypeElement typeElement = (TypeElement)
                ((DeclaredType) field.asType())
                        .asElement();

        // 获取自定义表名
        // TableName tableName = typeElement.getAnnotation(TableName.class);
        // if (tableName != null && !tableName.value().isEmpty()) {
        //     return tableName.value();
        // }

        // 默认使用类名转下划线命名作为表名
        return NamingConvertUtil.camel2Snake(typeElement.getSimpleName().toString());
    }

    /**
     * todo: 命名不清晰
     * 将路径分成左右侧依赖关系, 左侧就是current, 右侧就是 segs.i, 代表从current递进下找i
     *
     * @param root 实体
     * @param path 当前路径
     * @return 合格的字段
     */
    private VariableElement resolvePath(TypeElement root, String path) {
        // 将 attrName 看做从左到右的依赖路径, 拆解成每一段主中从对象的命名
        String[] segs = path.split("\\.");

        // 原实体为根, 通过命名一层往下找从, 来切换根视角
        TypeElement cur = root;

        VariableElement found = null;
        for (int i = 0; i < segs.length; i++) {
            found = null;
            // 查找
            for (Element e : cur.getEnclosedElements()) {
                if (e.getKind() == ElementKind.FIELD
                        && e.getSimpleName().contentEquals(segs[i])) {
                    found = (VariableElement) e;
                    break;
                }
            }

            // 路径中断则返回空
            if (found == null) return null;
            // 因为attrName记录的是当前从的所有路径, 例{department.company}, 所以最后一位就是当前从{company}
            if (i < segs.length - 1) {
                cur = (TypeElement) ((DeclaredType) found.asType()).asElement();
            }
        }
        return found;
    }
}
