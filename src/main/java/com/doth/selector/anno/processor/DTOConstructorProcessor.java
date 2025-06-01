package com.doth.selector.anno.processor;

import com.doth.selector.anno.*;  // Import the custom annotations
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

    // 初始化注解处理器上下文环境
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
        // Find all constructors annotated with @DTOConstructor
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
     * Generates the DTO class source file for the given entity and constructor.
     */
    private void generateDtoClass(TypeElement entityClass, ExecutableElement constructorElement, String dtoId) {
        // Determine package name of entity
        Elements elementUtils = processingEnv.getElementUtils();
        String entityQualifiedName = elementUtils.getBinaryName(entityClass).toString();
        String packageName = "";
        int lastDot = entityQualifiedName.lastIndexOf('.');
        if (lastDot > 0) {
            packageName = entityQualifiedName.substring(0, lastDot);
        }
        // Determine DTO class name (capitalize dtoId, handle underscores if any)
        String dtoClassName = toCamelCase(dtoId, true);  // true for capitalize first letter
        // Prepare content builder for class
        StringBuilder classContent = new StringBuilder();
        if (!packageName.isEmpty()) {
            classContent.append("package ").append(packageName).append(";\n\n");
        }
        // Import statements for annotations and any needed classes
        classContent.append("import java.util.ArrayList;\n");
        classContent.append("import java.util.List;\n");
        // Import the annotations (if not in same package)
        classContent.append("import com.doth.selector.anno.DependOn;\n");
        classContent.append("import com.doth.selector.anno.DTOConstructor;\n");  // in case needed for clarity, though not used inside class
        // Import entity class if not same package
        String entitySimpleName = entityClass.getSimpleName().toString();
        if (!packageName.isEmpty() && !entityQualifiedName.equals(packageName + "." + entitySimpleName)) {
            // If the entity class is in a different package (which would be unusual if names differ),
            // but typically entityQualifiedName = packageName + "." + entitySimpleName.
            // We'll ensure import is correct:
            classContent.append("import ").append(entityQualifiedName).append(";\n");
        }
        // Import DTOFactory and DTOSelectFieldsListFactory (assuming they are in accessible packages)
        classContent.append("import com.doth.selector.dto.DTOFactory;\n");
        classContent.append("import com.doth.selector.dto.DTOSelectFieldsListFactory;\n\n");  // Adjust package as appropriate

        // Class definition with @DependOn
        classContent.append("@DependOn(clzPath=\"").append(entityQualifiedName).append("\")\n");
        classContent.append("public class ").append(dtoClassName).append(" {\n\n");

        // Fields: one for each constructor parameter
        List<? extends VariableElement> params = constructorElement.getParameters();
        // Determine naming for each field
        Set<String> mainFieldNames = new HashSet<>();       // names of main entity fields
        List<ParamInfo> joinParamInfos = new ArrayList<>(); // info for join parameters
        for (VariableElement param : params) {
            String paramName = param.getSimpleName().toString();
            if (!paramName.contains("_")) {
                mainFieldNames.add(paramName);
            } else {
                // Collect join param info (prefix and base name)
                String prefix = paramName.substring(0, paramName.indexOf('_'));
                String baseName = paramName.substring(paramName.indexOf('_') + 1);
                // Check for @PfxAlias on this parameter
                PfxAlias aliasAnn = param.getAnnotation(PfxAlias.class);
                String aliasPrefix = (aliasAnn != null ? aliasAnn.name() : null);
                joinParamInfos.add(new ParamInfo(prefix, baseName, aliasPrefix));
            }
        }
        // Compute frequency of base names among join params that do not have a custom alias
        Map<String, Long> baseNameCount = new HashMap<>();
        for (ParamInfo pi : joinParamInfos) {
            if (pi.aliasPrefix == null) {
                baseNameCount.put(pi.baseName, baseNameCount.getOrDefault(pi.baseName, 0L) + 1);
            }
        }
        // Determine final DTO field names for each parameter
        List<FieldSpec> fieldSpecs = new ArrayList<>();
        for (VariableElement param : params) {
            String paramName = param.getSimpleName().toString();
            TypeMirror paramType = param.asType();
            String fieldName;
            if (!paramName.contains("_")) {
                // Main field: name stays the same
                fieldName = paramName;
            } else {
                String prefix = paramName.substring(0, paramName.indexOf('_'));
                String baseName = paramName.substring(paramName.indexOf('_') + 1);
                PfxAlias aliasAnn = param.getAnnotation(PfxAlias.class);
                if (aliasAnn != null) {
                    // Use custom prefix
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
            // Determine the Java type for the field (use simple name for output if possible)
            String fieldType = getTypeString(paramType, packageName);
            fieldSpecs.add(new FieldSpec(fieldName, fieldType, param));
        }

        // Generate field declarations
        for (FieldSpec fs : fieldSpecs) {
            classContent.append("    private ").append(fs.type).append(" ").append(fs.name).append(";\n");
        }
        classContent.append("\n");

        // Generate constructor
        String entityParamName = decapitalize(entitySimpleName);
        classContent.append("    public ").append(dtoClassName)
                .append("(").append(entitySimpleName).append(" ").append(entityParamName).append(") {\n");
        // We will build join chain expressions as we iterate fields in original param order
        int aliasCounter = 1;
        // Maps to keep track of active join aliases and object access paths
        Map<String, String> prefixToAlias = new HashMap<>();
        Map<String, String> prefixToObjectPath = new HashMap<>();
        boolean chainActive = false;
        String currentChainPrefix = null;
        String currentObjectPath = null;
        // We'll also keep track of the last seen join level prefix for continuing deeper joins
        String lastJoinPrefix = null;
        for (VariableElement param : params) {
            String paramName = param.getSimpleName().toString();
            // Find corresponding FieldSpec (to get DTO field name)
            FieldSpec fs = fieldSpecs.stream()
                    .filter(f -> f.paramElement.equals(param))
                    .findFirst().orElse(null);
            if (fs == null) continue;
            String dtoFieldName = fs.name;
            if (!paramName.contains("_")) {
                // Main field mapping: direct from entity
                // End any active chain if present
                chainActive = false;
                prefixToAlias.clear();
                prefixToObjectPath.clear();
                lastJoinPrefix = null;
                // Assign using entity's getter
                String getterName = "get" + capitalize(paramName);
                classContent.append("        this.").append(dtoFieldName)
                        .append(" = ").append(entityParamName).append(".")
                        .append(getterName).append("();\n");
            } else {
                // Joined field mapping
                String prefix = paramName.substring(0, paramName.indexOf('_'));
                String baseName = paramName.substring(paramName.indexOf('_') + 1);
                JoinLevel joinAnn = param.getAnnotation(JoinLevel.class);
                Next nextAnn = param.getAnnotation(Next.class);
                if (joinAnn != null || !chainActive) {
                    // Start a new join chain
                    prefixToAlias.clear();
                    prefixToObjectPath.clear();
                    chainActive = true;
                    lastJoinPrefix = prefix;
                    // Assign a new alias for this join level
                    String alias = "t" + aliasCounter++;
                    prefixToAlias.put(prefix, alias);
                    // Build object access path for this association
                    String getterName = "get" + capitalize(prefix);
                    currentObjectPath = entityParamName + "." + getterName + "()";
                    prefixToObjectPath.put(prefix, currentObjectPath);
                    // Map field from the joined object
                    String fieldGetter = "get" + capitalize(baseName);
                    classContent.append("        this.").append(dtoFieldName)
                            .append(" = ").append(currentObjectPath).append(".")
                            .append(fieldGetter).append("();\n");
                } else if (nextAnn != null) {
                    // Continue to next level in the existing chain
                    // Extend the object path from the last prefix
                    String prevObjectPath = currentObjectPath;
                    // Assign a new alias for the next join level
                    String alias = "t" + aliasCounter++;
                    prefixToAlias.put(prefix, alias);
                    // Build object access for next association
                    String getterName = "get" + capitalize(prefix);
                    currentObjectPath = prevObjectPath + "." + getterName + "()";
                    lastJoinPrefix = prefix;
                    prefixToObjectPath.put(prefix, currentObjectPath);
                    // Map field from the new joined object
                    String fieldGetter = "get" + capitalize(baseName);
                    classContent.append("        this.").append(dtoFieldName)
                            .append(" = ").append(currentObjectPath).append(".")
                            .append(fieldGetter).append("();\n");
                } else {
                    // Additional field from an already joined object (same level or earlier in chain)
                    if (prefixToObjectPath.containsKey(prefix)) {
                        // Use existing join object path
                        String objectPath = prefixToObjectPath.get(prefix);
                        String fieldGetter = "get" + capitalize(baseName);
                        classContent.append("        this.").append(dtoFieldName)
                                .append(" = ").append(objectPath).append(".")
                                .append(fieldGetter).append("();\n");
                    } else {
                        // This scenario shouldn't normally occur (undefined join prefix without annotation)
                        classContent.append("        // Warning: undefined join prefix '").append(prefix)
                                .append("' for field ").append(dtoFieldName).append("\n");
                    }
                }
            }
        }
        classContent.append("    }\n\n");

        // Generate getter and setter methods for each field
        for (FieldSpec fs : fieldSpecs) {
            String fieldName = fs.name;
            String fieldType = fs.type;
            // Getter
            String capName = capitalize(fieldName);
            classContent.append("    public ").append(fieldType).append(" get").append(capName)
                    .append("() {\n        return this.").append(fieldName).append(";\n    }\n\n");
            // Setter
            classContent.append("    public void set").append(capName).append("(")
                    .append(fieldType).append(" ").append(fieldName).append(") {\n")
                    .append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n    }\n\n");
        }

        // Static block for DTOFactory and DTOSelectFieldsListFactory registration
        classContent.append("    static {\n");
        // Register DTO class in DTOFactory
        classContent.append("        DTOFactory.register(").append(entitySimpleName).append(".class, \"")
                .append(dtoId).append("\", ").append(dtoClassName).append(".class);\n");
        // Build select field path list
        classContent.append("        List<String> __selectFields = new ArrayList<>();\n");
        aliasCounter = 1;
        prefixToAlias.clear();
        // We will iterate again through parameters to add field paths (t0, t1, etc.) in order
        boolean chainActiveForPaths = false;
        lastJoinPrefix = null;
        for (VariableElement param : params) {
            String paramName = param.getSimpleName().toString();
            if (!paramName.contains("_")) {
                // main field
                classContent.append("        __selectFields.add(\"t0.").append(paramName).append("\");\n");
                chainActiveForPaths = false;
                prefixToAlias.clear();
                lastJoinPrefix = null;
            } else {
                String prefix = paramName.substring(0, paramName.indexOf('_'));
                String baseName = paramName.substring(paramName.indexOf('_') + 1);
                JoinLevel joinAnn = param.getAnnotation(JoinLevel.class);
                Next nextAnn = param.getAnnotation(Next.class);
                if (joinAnn != null || !chainActiveForPaths) {
                    // new chain or chain start
                    String alias = "t" + aliasCounter++;
                    prefixToAlias.clear();
                    prefixToAlias.put(prefix, alias);
                    chainActiveForPaths = true;
                    lastJoinPrefix = prefix;
                    classContent.append("        __selectFields.add(\"").append(alias).append(".").append(baseName).append("\");\n");
                } else if (nextAnn != null) {
                    // next level in existing chain
                    String alias = "t" + aliasCounter++;
                    prefixToAlias.put(prefix, alias);
                    lastJoinPrefix = prefix;
                    classContent.append("        __selectFields.add(\"").append(alias).append(".").append(baseName).append("\");\n");
                } else {
                    // additional field from existing join level (reuse current alias for that prefix)
                    if (prefixToAlias.containsKey(prefix)) {
                        String alias = prefixToAlias.get(prefix);
                        classContent.append("        __selectFields.add(\"").append(alias).append(".").append(baseName).append("\");\n");
                    } else {
                        // Should not happen normally
                        classContent.append("        __selectFields.add(\"t?.").append(baseName).append("\"); // prefix '")
                                .append(prefix).append("' not found\n");
                    }
                }
            }
        }
        classContent.append("        DTOSelectFieldsListFactory.register(")
                .append(entitySimpleName).append(".class, \"").append(dtoId)
                .append("\", __selectFields);\n");
        classContent.append("    }\n");

        // Close class
        classContent.append("}\n");

        // Write the generated class to a .java file
        try {
            JavaFileObject fileObject = processingEnv.getFiler()
                    .createSourceFile(packageName.isEmpty() ? dtoClassName : packageName + "." + dtoClassName, entityClass);
            try (Writer writer = fileObject.openWriter()) {
                writer.write(classContent.toString());
            }
        } catch (IOException e) {
            // Log error if file creation fails
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Failed to write DTO class: " + e.getMessage(), entityClass);
        }
    }

    /** Helper to capitalize the first letter of a string. */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        // If second letter is uppercase, we still capitalize first (to preserve acronyms properly).
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Helper to convert a string with possible underscores to camel case.
     * If capitalizeFirst is true, the first letter of the result will be uppercase.
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
     * Helper to get the type name as a string for field declarations, handling imports.
     */
    private String getTypeString(TypeMirror typeMirror, String currentPackage) {
        // Primitive types:
        if (typeMirror.getKind().isPrimitive()) {
            return typeMirror.toString();  // e.g., "int", "long"
        }
        // Declared types (classes):
        if (typeMirror instanceof DeclaredType) {
            TypeElement typeElem = (TypeElement) ((DeclaredType) typeMirror).asElement();
            String qualName = processingEnv.getElementUtils().getBinaryName(typeElem).toString();
            String simpleName = typeElem.getSimpleName().toString();
            // Use simple name for java.lang classes or classes in same package
            if (qualName.startsWith("java.lang.")) {
                return simpleName;
            }
            if (!currentPackage.isEmpty() && qualName.startsWith(currentPackage + ".")) {
                return simpleName;
            }
            // Otherwise, use fully qualified name (or import in class header, but here we output FQCN to be safe)
            return qualName;
        }
        // Other types (arrays, etc.)
        return typeMirror.toString();
    }

    /** Simple struct to hold join parameter info for naming. */
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

    /** Simple struct to hold field specifications for generation. */
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

    /** Decapitalize the first letter (for entity instance variable naming). */
    private String decapitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        if (str.length() > 1 && Character.isUpperCase(str.charAt(0))
                && Character.isUpperCase(str.charAt(1))) {
            // If first two are uppercase (acronym), leave as is to avoid changing "URL" to "uRL"
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }
}
