package com.doth.selector.coordinator.supports.sqlgenerator.tool.temp;

import com.doth.selector.anno.DependOn;

/**
 * 策略工厂：根据 @DependOn 决定使用哪个初始化策略
 */
public class QueryStrategyFactory {
    public static QueryInitializationStrategy getStrategy(Class<?> entityClass) {
        return entityClass.isAnnotationPresent(DependOn.class)
                ? new DtoInitializationStrategy()
                : new GeneralInitializationStrategy();
    }
}
