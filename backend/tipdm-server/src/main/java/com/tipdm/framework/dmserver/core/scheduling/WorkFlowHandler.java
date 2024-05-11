package com.tipdm.framework.dmserver.core.scheduling;

import com.github.dexecutor.core.ExecutionConfig;
import com.tipdm.framework.common.utils.RedisUtils;
import com.tipdm.framework.dmserver.rpc.MessageManager;
import com.tipdm.framework.dmserver.utils.Constants;
import com.tipdm.framework.dmserver.utils.RedissonUtils;
import com.tipdm.framework.dmserver.websocket.dto.Type;
import com.tipdm.framework.dmserver.websocket.dto.WorkFlowMessage;
import com.tipdm.framework.service.dmserver.MessageService;
import org.quartz.Scheduler;
import org.redisson.api.RMapCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Semaphore;


/**
 * Created by TipDM on 2017/7/20.
 * E-mail:devp@tipdm.com
 */
@SuppressWarnings("all")
public class WorkFlowHandler extends Thread {

    private final static Logger logger = LoggerFactory.getLogger(WorkFlowHandler.class);

    private WorkFlow workFlow;

    private String workFlowId;

    private Scheduler scheduler;

    private Semaphore semaphore;

    private WorkFlowDexecutor dexecutor;

    public WorkFlowHandler(String workFlowId, Scheduler scheduler, WorkFlowDexecutor dexecutor, Semaphore semaphore) {
        this.workFlowId = workFlowId;
        this.scheduler = scheduler;
        this.dexecutor = dexecutor;
        this.semaphore = semaphore;
    }

    @Override
    public void run() {
        this.dexecutor.execute(new ExecutionConfig().nonTerminating());
        release();
    }


    public void abort() {
        dexecutor.abort();
        release();
    }

    private void release() {
        //释放信号锁
        semaphore.release();
        RedisUtils.removeFromMap("executing_flows", workFlowId);
        String accessToken = dexecutor.getAccessToken();

        dexecutor.shutdown();
        WorkFlowMessage message = new WorkFlowMessage();
        message.setType(Type.WORKFLOW);
        message.setState(com.tipdm.framework.dmserver.core.scheduling.State.COMPLETE);

        RMapCache<String, Map<String, String>> mapCache = RedissonUtils.getRMapCache(Constants.WS_CLIENTS);
        Map<String, String> mapping = mapCache.get(accessToken);
        String sessionId = mapping.get("sessionId");
        String rpcAddress = mapping.get("rpcAddress");
        MessageService messageService = MessageManager.getService(rpcAddress);
        messageService.notifyWorkFlowExecStatus(workFlowId, sessionId, message);
        logger.info("workFlowId: {} is done, exit...", workFlowId);
    }
}