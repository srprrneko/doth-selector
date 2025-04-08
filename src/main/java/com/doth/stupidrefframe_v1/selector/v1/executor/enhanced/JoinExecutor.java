package com.doth.stupidrefframe_v1.selector.v1.executor.enhanced;

import com.doth.stupidrefframe_v1.selector.v1.coordinator.core.ExecuteCoordinatorService;
import com.doth.stupidrefframe_v1.selector.v1.coordinator.core.process.JoinExecuteCoordinator;
import com.doth.stupidrefframe_v1.selector.v1.executor.basic.BasicKindQueryExecutor;

/**
 * @author 贤
 * @version 1.0
 * @date 2025/4/6 0:20
 * @description 所有需要开启连接映射协调器的次基类
 */
public class JoinExecutor<T> extends BasicKindQueryExecutor<T> {


    @Override
    protected void setCoordinator(ExecuteCoordinatorService coordinator) {
        super.setCoordinator(new JoinExecuteCoordinator());
    }
}