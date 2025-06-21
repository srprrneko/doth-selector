package com.doth.selector.anno.processor;

import com.doth.selector.anno.*;  // 导入自定义注解
import com.doth.selector.common.util.NamingConvertUtil;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.lang.model.SourceVersion;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.*;
import java.io.Writer;
import java.io.IOException;

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.doth.selector.anno.DTOConstructor")
public class DTOConstructorProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;
    private Types typeUtils;
    private Elements elementUtils;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * 初始化注解处理器上下文环境
     */
    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        this.filer = env.getFiler();
        this.messager = env.getMessager();
        this.typeUtils = env.getTypeUtils();
        this.elementUtils = env.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 查找所有被 @DTOConstructor 注解标注的构造方法
        for (Element elem : roundEnv.getElementsAnnotatedWith(DTOConstructor.class)) {
            if (!(elem instanceof ExecutableElement)) {
                continue;
            }
            ExecutableElement constructorElement = (ExecutableElement) elem;
            TypeElement entityClass = (TypeElement) constructorElement.getEnclosingElement();
            DTOConstructor dtoAnnotation = constructorElement.getAnnotation(DTOConstructor.class);
            String dtoId = dtoAnnotation.id();
            generateDtoClass(entityClass, constructorElement, dtoId);
        }
        return true;
    }

    /**
     * 生成给定实体和构造方法的 DTO 类源文件
     */
    private void generateDtoClass(TypeElement entityClass, ExecutableElement constructorElement, String dtoId) {
        // 确定实体类的包名
        Elements elementUtils = processingEnv.getElementUtils();
        String entityQualifiedName = elementUtils.getBinaryName(entityClass).toString();
        String packageName = "";
        int lastDot = entityQualifiedName.lastIndexOf('.');
        if (lastDot > 0) {
            packageName = entityQualifiedName.substring(0, lastDot);
        }

        // 根据 dtoId 确定 DTO 类名（首字母大写，处理下划线）
        String dtoClassName = NamingConvertUtil.toUpperCaseFirstLetter(dtoId, false);

        // 准备构建类内容的 StringBuilder
        StringBuilder classContent = new StringBuilder();
        if (!packageName.isEmpty()) {
            classContent.append("package ").append(packageName).append(";\n\n");
        }

        // 导入所需的类
        classContent.append("import java.util.ArrayList;\n");
        classContent.append("import java.util.List;\n");
        classContent.append("import com.doth.selector.anno.DependOn;\n");
        classContent.append("import com.doth.selector.anno.DTOConstructor;\n");

        // 如果实体类不在同一包中，导入实体类
        String entitySimpleName = entityClass.getSimpleName().toString();
        if (!packageName.isEmpty() && !entityQualifiedName.equals(packageName + "." + entitySimpleName)) {
            classContent.append("import ").append(entityQualifiedName).append(";\n");
        }

        // 导入 DTOFactory 和 DTOSelectFieldsListFactory（假设它们在可访问的包中）
        classContent.append("import com.doth.selector.dto.DTOFactory;\n");
        classContent.append("import com.doth.selector.dto.DTOSelectFieldsListFactory;\n\n");

        // 类定义，带 @DependOn 注解
        classContent.append("@DependOn(clzPath=\"").append(entityQualifiedName).append("\")\n");
        classContent.append("public class ").append(dtoClassName).append(" {\n\n");

        // 收集构造方法参数信息
        List<? extends VariableElement> params = constructorElement.getParameters();

        // 用于存储主表字段名
        Set<String> mainFieldNames = new HashSet<>();
        // 用于存储关联参数信息（前缀、字段基名、自定义别名）
        List<ParamInfo> joinParamInfos = new ArrayList<>();
        for (VariableElement param : params) {
            String paramName = param.getSimpleName().toString();
            if (!paramName.contains("_")) {
                mainFieldNames.add(paramName);
            } else {
                String prefix = paramName.substring(0, paramName.indexOf('_'));
                String baseName = paramName.substring(paramName.indexOf('_') + 1);
                // 检查 @PfxAlias 注解
                PfxAlias aliasAnn = param.getAnnotation(PfxAlias.class);
                String aliasPrefix = (aliasAnn != null ? aliasAnn.name() : null);
                joinParamInfos.add(new ParamInfo(prefix, baseName, aliasPrefix));
            }
        }

        // 统计不带自定义别名前缀的关联字段基名出现频率
        Map<String, Long> baseNameCount = new HashMap<>();
        for (ParamInfo pi : joinParamInfos) {
            if (pi.aliasPrefix == null) {
                baseNameCount.put(pi.baseName, baseNameCount.getOrDefault(pi.baseName, 0L) + 1);
            }
        }

        // 根据参数信息生成最终 DTO 字段规格
        List<FieldSpec> fieldSpecs = new ArrayList<>();
        for (VariableElement param : params) {
            String paramName = param.getSimpleName().toString();
            TypeMirror paramType = param.asType();
            String fieldName;
            if (!paramName.contains("_")) {
                // 主表字段：名称不变
                fieldName = paramName;
            } else {
                String prefix = paramName.substring(0, paramName.indexOf('_'));
                String baseName = paramName.substring(paramName.indexOf('_') + 1);
                PfxAlias aliasAnn = param.getAnnotation(PfxAlias.class);
                if (aliasAnn != null) {
                    // 使用自定义前缀
                    String customPrefix = aliasAnn.name();
                    fieldName = customPrefix + capitalize(baseName);
                } else {
                    boolean conflict = mainFieldNames.contains(baseName) || (baseNameCount.getOrDefault(baseName, 0L) > 1);
                    if (conflict) {
                        fieldName = prefix + capitalize(baseName);
                    } else {
                        fieldName = baseName;
                    }
                }
            }
            // 确定字段类型（如果在同一包或 java.lang 中，可使用简单名，否则使用全限定名）
            String fieldType = getTypeString(paramType, packageName);
            fieldSpecs.add(new FieldSpec(fieldName, fieldType, param));
        }

        // 生成字段声明
        for (FieldSpec fs : fieldSpecs) {
            classContent.append("    private ").append(fs.type).append(" ").append(fs.name).append(";\n");
        }
        classContent.append("\n");

        // 生成无参构造方法
        classContent.append("    public ").append(dtoClassName).append("() {}\n\n");

        // 生成接收实体对象的构造方法
        String entityParamName = decapitalize(entitySimpleName);
        classContent.append("    public ").append(dtoClassName)
                    .append("(").append(entitySimpleName).append(" ").append(entityParamName).append(") {\n");

        int aliasCounter = 1;
        // 存储每个前缀对应的别名及对象访问路径
        Map<String, String> prefixToAlias = new HashMap<>();
        Map<String, String> prefixToObjectPath = new HashMap<>();
        boolean chainActive = false;
        String currentObjectPath = null;
        String lastJoinPrefix = null;

        for (VariableElement param : params) {
            String paramName = param.getSimpleName().toString();
            FieldSpec fs = fieldSpecs.stream()
                    .filter(f -> f.paramElement.equals(param))
                    .findFirst()
                    .orElse(null);
            if (fs == null) continue;
            String dtoFieldName = fs.name;

            if (!paramName.contains("_")) {
                // 主表字段映射：直接通过实体 getter 赋值
                chainActive = false;
                prefixToAlias.clear();
                prefixToObjectPath.clear();
                lastJoinPrefix = null;

                String getterName = "get" + capitalize(paramName);
                classContent.append("        this.").append(dtoFieldName)
                        .append(" = ").append(entityParamName).append(".")
                        .append(getterName).append("();\n");
            } else {
                // 关联字段映射
                String prefix = paramName.substring(0, paramName.indexOf('_'));
                String baseName = paramName.substring(paramName.indexOf('_') + 1);
                JoinLevel joinAnn = param.getAnnotation(JoinLevel.class);
                Next nextAnn = param.getAnnotation(Next.class);

                if (joinAnn != null || !chainActive) {
                    // 新的关联链开始
                    prefixToAlias.clear();
                    prefixToObjectPath.clear();
                    chainActive = true;
                    lastJoinPrefix = prefix;

                    // 分配新的别名
                    String alias = "t" + aliasCounter++;
                    prefixToAlias.put(prefix, alias);

                    // 构建访问路径：entity.getPrefix()
                    String getterName = "get" + capitalize(prefix);
                    currentObjectPath = entityParamName + "." + getterName + "()";
                    prefixToObjectPath.put(prefix, currentObjectPath);

                    // 从关联对象获取字段值
                    String fieldGetter = "get" + capitalize(baseName);
                    classContent.append("        this.").append(dtoFieldName)
                            .append(" = ").append(currentObjectPath).append(".")
                            .append(fieldGetter).append("();\n");
                } else if (nextAnn != null) {
                    // 当前关联链的下一层
                    String prevObjectPath = currentObjectPath;

                    String alias = "t" + aliasCounter++;
                    prefixToAlias.put(prefix, alias);

                    String getterName = "get" + capitalize(prefix);
                    currentObjectPath = prevObjectPath + "." + getterName + "()";
                    lastJoinPrefix = prefix;
                    prefixToObjectPath.put(prefix, currentObjectPath);

                    String fieldGetter = "get" + capitalize(baseName);
                    classContent.append("        this.").append(dtoFieldName)
                            .append(" = ").append(currentObjectPath).append(".")
                            .append(fieldGetter).append("();\n");
                } else {
                    // 已存在的同层级关联字段（不带注解的参数）
                    if (prefixToObjectPath.containsKey(prefix)) {
                        String objectPath = prefixToObjectPath.get(prefix);
                        String fieldGetter = "get" + capitalize(baseName);
                        classContent.append("        this.").append(dtoFieldName)
                                .append(" = ").append(objectPath).append(".")
                                .append(fieldGetter).append("();\n");
                    } else {
                        // 正常情况下不会出现此情况（未定义的关联前缀）
                        classContent.append("        // 警告：未定义的关联前缀 '")
                                .append(prefix).append("' 对应字段 ").append(dtoFieldName).append("\n");
                    }
                }
            }
        }
        classContent.append("    }\n\n");

        // 生成每个字段的 getter 和 setter 方法
        for (FieldSpec fs : fieldSpecs) {
            String fieldName = fs.name;
            String fieldType = fs.type;
            String capName = capitalize(fieldName);

            // Getter
            classContent.append("    public ").append(fieldType).append(" get").append(capName).append("() {\n")
                        .append("        return this.").append(fieldName).append(";\n")
                        .append("    }\n\n");

            // Setter
            classContent.append("    public void set").append(capName).append("(")
                        .append(fieldType).append(" ").append(fieldName).append(") {\n")
                        .append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n")
                        .append("    }\n\n");
        }

        // 静态代码块：将 DTO 类注册到工厂，并注册查询字段列表
        classContent.append("    static {\n");
        // 在 DTOFactory 中注册 DTO 类
        classContent.append("        DTOFactory.register(").append(entitySimpleName)
                .append(".class, \"").append(dtoId).append("\", ").append(dtoClassName).append(".class);\n");

        // 构建查询字段路径列表
        classContent.append("        List<String> __selectFields = new ArrayList<>();\n");
        aliasCounter = 1;
        prefixToAlias.clear();
        boolean chainActiveForPaths = false;
        lastJoinPrefix = null;

        for (VariableElement param : params) {
            String paramName = param.getSimpleName().toString();
            if (!paramName.contains("_")) {
                // 主表字段
                classContent.append("        __selectFields.add(\"t0.").append(paramName).append("\");\n");
                chainActiveForPaths = false;
                prefixToAlias.clear();
                lastJoinPrefix = null;
            } else {
                // 关联字段
                String prefix = paramName.substring(0, paramName.indexOf('_'));
                String baseName = paramName.substring(paramName.indexOf('_') + 1);
                JoinLevel joinAnn = param.getAnnotation(JoinLevel.class);
                Next nextAnn = param.getAnnotation(Next.class);

                if (joinAnn != null || !chainActiveForPaths) {
                    // 开始新的关联链
                    String alias = "t" + aliasCounter++;
                    prefixToAlias.clear();
                    prefixToAlias.put(prefix, alias);
                    chainActiveForPaths = true;
                    lastJoinPrefix = prefix;
                    classContent.append("        __selectFields.add(\"").append(alias).append(".").append(baseName).append("\");\n");
                } else if (nextAnn != null) {
                    // 继续当前关联链下一层
                    String alias = "t" + aliasCounter++;
                    prefixToAlias.put(prefix, alias);
                    lastJoinPrefix = prefix;
                    classContent.append("        __selectFields.add(\"").append(alias).append(".").append(baseName).append("\");\n");
                } else {
                    // 同层级重复字段（不带注解）
                    if (prefixToAlias.containsKey(prefix)) {
                        String alias = prefixToAlias.get(prefix);
                        classContent.append("        __selectFields.add(\"").append(alias).append(".").append(baseName).append("\");\n");
                    } else {
                        // 正常情况下不应出现此情况
                        classContent.append("        __selectFields.add(\"t?.").append(baseName)
                                .append("\"); // 未找到前缀 '").append(prefix).append("'\n");
                    }
                }
            }
        }

        // 在 DTOSelectFieldsListFactory 中注册查询字段列表
        classContent.append("        DTOSelectFieldsListFactory.register(")
                .append(entitySimpleName).append(".class, \"").append(dtoId)
                .append("\", __selectFields);\n");
        classContent.append("    }\n");

        // 结束类定义
        classContent.append("}\n");

        // 将生成的类写入 .java 文件
        try {
            JavaFileObject fileObject = processingEnv.getFiler()
                    .createSourceFile(packageName.isEmpty() ? dtoClassName : packageName + "." + dtoClassName, entityClass);
            try (Writer writer = fileObject.openWriter()) {
                writer.write(classContent.toString());
            }
        } catch (IOException e) {
            // 如果写文件失败，打印错误信息
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "无法写入 DTO 类: " + e.getMessage(), entityClass);
        }
    }

    /**
     * 将字符串首字母大写，例如 "name" -> "Name"
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * 将可能包含下划线的字符串转换为驼峰形式。
     * 如果 capitalizeFirst 为 true，则首字母大写。
     */
    private String toCamelCase(String input, boolean capitalizeFirst) {
        if (input == null) return null;
        StringBuilder sb = new StringBuilder();
        boolean upperNext = capitalizeFirst;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '_' || c == ' ' || c == '-') {
                upperNext = true;
            } else if (upperNext) {
                sb.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 获取字段声明时的类型名称，处理导入逻辑：
     * - 原始类型直接返回类型名
     * - 如果是 java.lang 下的类型或当前包下的类型，使用简单类名
     * - 否则使用全限定名
     */
    private String getTypeString(TypeMirror typeMirror, String currentPackage) {
        if (typeMirror.getKind().isPrimitive()) {
            return typeMirror.toString();
        }
        if (typeMirror instanceof DeclaredType) {
            TypeElement typeElem = (TypeElement) ((DeclaredType) typeMirror).asElement();
            String qualName = processingEnv.getElementUtils().getBinaryName(typeElem).toString();
            String simpleName = typeElem.getSimpleName().toString();
            // java.lang 下的类型使用简单名
            if (qualName.startsWith("java.lang.")) {
                return simpleName;
            }
            // 当前包下的类型使用简单名
            if (!currentPackage.isEmpty() && qualName.startsWith(currentPackage + ".")) {
                return simpleName;
            }
            // 否则使用全限定名
            return qualName;
        }
        // 其他类型（数组等）直接返回 toString()
        return typeMirror.toString();
    }

    /**
     * 存储关联参数信息（前缀、字段基名以及自定义别名前缀）
     */
    private static class ParamInfo {
        String prefix;
        String baseName;
        String aliasPrefix;

        ParamInfo(String prefix, String baseName, String aliasPrefix) {
            this.prefix = prefix;
            this.baseName = baseName;
            this.aliasPrefix = aliasPrefix;
        }
    }

    /**
     * 存储生成字段的规格信息（字段名、类型以及原始参数元素）
     */
    private static class FieldSpec {
        String name;
        String type;
        VariableElement paramElement;

        FieldSpec(String name, String type, VariableElement element) {
            this.name = name;
            this.type = type;
            this.paramElement = element;
        }
    }

    /**
     * 将字符串首字母小写（用于实体实例变量命名），
     * 如果前两个字母均为大写（如 URL），则不做修改
     */
    private String decapitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        if (str.length() > 1 && Character.isUpperCase(str.charAt(0))
                && Character.isUpperCase(str.charAt(1))) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }
}
