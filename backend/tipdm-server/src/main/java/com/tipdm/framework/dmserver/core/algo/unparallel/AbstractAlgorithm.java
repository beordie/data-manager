package com.tipdm.framework.dmserver.core.algo.unparallel;

import com.tipdm.framework.common.utils.SpringUtils;
import com.tipdm.framework.dmserver.core.algo.IAlgorithm;
import com.tipdm.framework.dmserver.exception.AlgorithmException;
import com.tipdm.framework.dmserver.exception.ConnectionException;
import com.tipdm.framework.model.dmserver.Component;
import com.tipdm.framework.model.dmserver.ComponentIO;
import com.tipdm.framework.service.dmserver.ComponentService;
import com.tipdm.framework.service.dmserver.DataTableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by TipDM on 2017/6/8.
 * E-mail:devp@tipdm.com
 * 单机算法抽象类
 */
public abstract class AbstractAlgorithm implements IAlgorithm {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected DataTableService tableService = SpringUtils.getBean("tableService", DataTableService.class);

    protected ComponentService componentService = SpringUtils.getBean("componentService", ComponentService.class);

    protected Component component;

    @Override
    public void run(Component component) throws ConnectionException, AlgorithmException {

        this.component = component;
        try {
            MDC.put("component", this.component.getClientId().toString());
            this.execute();
        } catch (ConnectionException e) {
            logger.error("获取连接出错, {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("algorithm execute error: {}", e.getMessage());
            throw new AlgorithmException(e);
        } finally {
            MDC.remove("component");
        }
    }

    protected abstract void execute() throws AlgorithmException;

    protected Map<String, String> getInputs() {
        Map<String, String> inputs = new HashMap<>();
        for (ComponentIO io : component.getInputs()) {
            inputs.put(io.getKey(), io.getTempTable());
        }
        return inputs;
    }

    @SuppressWarnings("all")
    protected Map<String, String> getOutputs() {
        Map<String, String> outputs = new HashMap<>();
        for (ComponentIO io : component.getOutputs()) {
            String table = io.getTempTable();
            outputs.put(io.getKey(), table);
        }
        return outputs;
    }

    protected Map<String, String> getParams() {
        return component.getParameters();
    }
    /**
     * 接收运行报告
     */
    protected void receiveReport() {

    }
}
