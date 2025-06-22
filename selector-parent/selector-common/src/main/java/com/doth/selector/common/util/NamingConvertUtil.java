package com.doth.selector.common.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 命名转换工具类：驼峰与下划线转换 + 缓存支持（基于 Caffeine）
 */
public class NamingConvertUtil {

    // 字段名缓存：最大 1000 项，10 分钟过期
    private static final Cache<String, String> CAMEL_TO_SNAKE_CACHE =
            Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .build();

    private static final Cache<String, String> SNAKE_TO_CAMEL_CACHE =
            Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(10, TimeUnit.MINUTES)
                    .build();

    public static String camel2SnakeCase(String input) {
        if (input == null || input.isEmpty()) return "";
        return CAMEL_TO_SNAKE_CACHE.get(input, NamingConvertUtil::doCamel2Snake);
    }

    public static String snake2CamelCase(String input) {
        if (input == null || input.isEmpty()) return "";
        return SNAKE_TO_CAMEL_CACHE.get(input, NamingConvertUtil::doSnake2Camel);
    }

    public static String camel2SnakeCase(String sql, boolean isRaw) {
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

    public static String toUpperCaseFirstLetter(String input, boolean checkStrict) {
        if (input == null || input.isEmpty()) return input;

        if (checkStrict) {
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if (!(Character.isLetter(c) || c == '$' || c == '_' || Character.isDigit(c))) {
                    throw new IllegalArgumentException("非法字符: '" + c + "'");
                }
            }
        }

        char firstChar = input.charAt(0);
        if (Character.isLowerCase(firstChar)) {
            firstChar = Character.toUpperCase(firstChar);
        }

        return firstChar + input.substring(1);
    }

    public static void clearCache() {
        CAMEL_TO_SNAKE_CACHE.invalidateAll();
        SNAKE_TO_CAMEL_CACHE.invalidateAll();
    }

    // 内部真正的转换逻辑
    private static String doCamel2Snake(String input) {
        StringBuilder sb = new StringBuilder();
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

    private static String doSnake2Camel(String input) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '_') {
                nextUpper = true;
            } else {
                sb.append(nextUpper ? Character.toUpperCase(c) : Character.toLowerCase(c));
                nextUpper = false;
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        String s = NamingConvertUtil.camel2SnakeCase("majorId");
        System.out.println("s = " + s);
    }
}
