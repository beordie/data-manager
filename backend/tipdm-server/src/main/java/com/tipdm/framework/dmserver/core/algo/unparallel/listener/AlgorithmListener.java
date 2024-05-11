package com.tipdm.framework.dmserver.core.algo.unparallel.listener;

import com.tipdm.framework.dmserver.exception.AlgorithmException;
import com.tipdm.framework.dmserver.core.algo.unparallel.executor.ExecutorContext;

/**
 * Created by TipDM on 2017/6/21.
 * E-mail:devp@tipdm.com
 */
public interface AlgorithmListener {

    /**
     * 初始化
     * @throws AlgorithmException
     */
    public void init(ExecutorContext executorContext) throws AlgorithmException;

    /**
     * 算法执行结束后调用
     * @param executorContext
     * @param ex
     */
    public void wasExecuted(ExecutorContext executorContext, AlgorithmException ex);
}
