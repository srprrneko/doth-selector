package com.doth.selector.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 规范转换
public class NamingConvertUtil {

    private static StringBuilder sb;



    public static String camel2SnakeCase(String input) {
        if (input == null || input.isEmpty()) return "";
        sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i != 0) sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // 不需要处理注解的 sqlgenerator 驼峰转换
    public static String camel2SnakeCase(String sql, Boolean isRaw) {

        if (!isRaw) return camel2SnakeCase(sql);

        String regex = "(?<=[a-z0-9])[A-Z]";
        Matcher matcher = Pattern.compile(regex).matcher(sql);
        StringBuilder buffer = new StringBuilder();

        while (matcher.find()) {
            matcher.appendReplacement(buffer, "_" + matcher.group().toLowerCase());
        }

        matcher.appendTail(buffer);

        return buffer.toString().replaceAll("_+", "_");
    }

    // ----------------- 下划线的转化逻辑 -----------------
    public static String snake2CamelCase(String input) {
        if (input == null || input.isEmpty()) return "";

        sb = new StringBuilder();
        boolean nextUpper = false;

        for (int i = 0; i < input.length(); i++) {

            char c = input.charAt(i); // 遍历每一个字符
            if (c == '_') {
                nextUpper = true; // 下划线后，下一个字母需要大写 (避免第一个字母大写)
            } else {
                if (nextUpper) { // 下一个字母需要大写
                    sb.append(Character.toUpperCase(c)); // 将下一个字母大写
                    nextUpper = false; // 重置
                } else {
                    sb.append(Character.toLowerCase(c));
                }
            }
        }
        return sb.toString();
    }


    /**
     * 将小写字母转换为大写，保留 $, _ 和数字字符
     * @param input 字符串, 可能是类名
     */
    public static String toUpperCaseFirstLetter(String input, boolean checkStrict) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // 严格检查，则遍历所有字符
        if (checkStrict) {
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if (!(Character.isLetter(c) || c == '$' || c == '_' || Character.isDigit(c))) {
                    throw new IllegalArgumentException(
                            "非法字符: '" + c + "' 仅允许字母、数字、$ 和 _"
                    );
                }
            }
        }

        // 转换首字母为大写
        char firstChar = input.charAt(0);
        if (Character.isLowerCase(firstChar)) {
            firstChar = Character.toUpperCase(firstChar);
        }

        return firstChar + input.substring(1);
    }

}