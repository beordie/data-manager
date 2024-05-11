package com.tipdm.framework.dmserver.core.algo.unparallel;

import com.tipdm.framework.common.utils.SpringUtils;
import com.tipdm.framework.dmserver.exception.AlgorithmException;
import com.tipdm.framework.dmserver.pyserve.PySession;
import com.tipdm.framework.dmserver.pyserve.PySessionPool;
import com.tipdm.framework.model.dmserver.Component;

/**
 * @author E-mail:devp@tipdm.com
 * @version 创建时间：2016年11月7日 上午10:29:42
 * 单机算法（Python）抽象类
 */
@SuppressWarnings("all")
public abstract class AbstractPythonAlgorithm extends AbstractAlgorithm{

    protected PySessionPool pysessionPool = SpringUtils.getBean("pysessionPool", PySessionPool.class);

    protected PySession pySession;

    @Override
    public final void run(Component component) throws AlgorithmException {
        this.component = component;
        try {
            pySession = pysessionPool.getSession();
            execute();
        } catch (AlgorithmException e){
            throw e;
        } finally {
            pySession.close();
        }
    }

    protected abstract void execute() throws AlgorithmException;

    /**
     * 默认方式，子类若没有特殊需求可直接在execute调用此方法
     * @throws AlgorithmException
     */
    protected void executeByDefault() throws AlgorithmException {


    }
}
