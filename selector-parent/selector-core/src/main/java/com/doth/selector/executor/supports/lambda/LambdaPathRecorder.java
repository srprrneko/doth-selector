package com.doth.selector.executor.supports.lambda;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>serve for class "Lambda2FieldNameResolver".. </p>
 */
public class LambdaPathRecorder {
    // 路径
    private final List<String> path = new ArrayList<>();

    public void append(String field) {
        path.add(field);
    }

    public boolean isEmpty() {
        return path.isEmpty();
    }

    public int size() {
        return path.size();
    }

    public String getLastFieldPath() {
        if (path.isEmpty()) return null;
        return "t" + (path.size() - 1) + "." + path.get(path.size() - 1);
    }

    public String getFullPath() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) sb.append(".");
            sb.append("t").append(i).append(".").append(path.get(i));
        }
        return sb.toString();
    }

    public List<String> getSegments() {
        return path;
    }
}