package com.tipdm.framework.dmserver.core.scheduling.listener;

import com.tipdm.framework.dmserver.core.scheduling.State;
import com.tipdm.framework.dmserver.core.scheduling.model.Job;
import com.tipdm.framework.dmserver.rpc.MessageManager;
import com.tipdm.framework.dmserver.utils.Constants;
import com.tipdm.framework.dmserver.utils.RedissonUtils;
import com.tipdm.framework.dmserver.websocket.dto.WorkFlowMessage;
import com.tipdm.framework.service.dmserver.MessageService;
import org.quartz.*;
import org.redisson.api.RMapCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by TipDM on 2017/1/11.
 * E-mail:devp@tipdm.com
 */
@SuppressWarnings("all")
public class WorkFlowJobListener implements JobListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String listenerName;

    private final static String LISTENER_NAME = "workFlowJobListener";

    private State state;

    private AtomicBoolean isDone;

    public WorkFlowJobListener(String listenerName) {
        this.listenerName = listenerName;
        this.state = State.INIT;
        isDone = new AtomicBoolean(false);
    }

    @Override
    public String getName() {
        return this.listenerName;
    }

    public State getState() {
        return state;
    }

    /**
     * job执行之前
     *
     * @param context
     */
    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        Job job = (Job) jobDataMap.get("job");

        String accessToken = jobDataMap.getString("accessToken");
        RMapCache<String, Map<String, String>> mapCache = RedissonUtils.getRMapCache(Constants.WS_CLIENTS);
        Map<String, String> mapping = mapCache.get(accessToken);
        String sessionId = mapping.get("sessionId");
        String rpcAddress = mapping.get("rpcAddress");
        MessageService messageService = MessageManager.getService(rpcAddress);
        //更新状态并推送到前端
        WorkFlowMessage message = new WorkFlowMessage();
        message.setNodeId(job.getJobId());
        message.setState(State.RUNNING);
        messageService.notifyWorkFlowExecStatus(job.getJobGroup(), sessionId, message);
        logger.info("job '" + context.getJobDetail().getKey().getName() + "' will be execute");
    }

    /**
     * 当TriggerListener中的vetoJobExecution方法返回true时,执行这个方法.
     * 这个方法如果被执行，jobToBeExecuted和jobWasExecuted就不会执行
     *
     * @param context
     */
    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        isDone.compareAndSet(false, true);
        state = State.ABORT;
        logger.error("job '" + context.getJobDetail().getKey().getName() + "' will be cancel");
    }

    /**
     * 任务执行完成后执行,jobException如果不为空则说明任务在执行过程中出现了异常
     *
     * @param context
     * @param e
     */
    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException e) {

        logger.info("jobWasExecuted...");
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        Job job = job = (Job) jobDataMap.get("job");
        logger.info("job full id: {}", job.getFullJobId());

        String accessToken = jobDataMap.getString("accessToken");
        RMapCache<String, Map<String, String>> mapCache = RedissonUtils.getRMapCache(Constants.WS_CLIENTS);
        Map<String, String> mapping = mapCache.get(accessToken);
        String sessionId = mapping.get("sessionId");
        String rpcAddress = mapping.get("rpcAddress");
        MessageService messageService = MessageManager.getService(rpcAddress);
        WorkFlowMessage message;
        if (null == e) {
            state = State.COMPLETE;
            //推送状态
            message = new WorkFlowMessage();
            message.setNodeId(job.getJobId());
            message.setState(state);
            messageService.notifyWorkFlowExecStatus(job.getJobGroup(), sessionId, message);
        } else {
            state = State.ERROR;
            e.setUnscheduleAllTriggers(true);
            //推送状态
            message = new WorkFlowMessage();
            message.setNodeId(job.getJobId());
            message.setState(state);
            messageService.notifyWorkFlowExecStatus(job.getJobGroup(), sessionId, message);
        }
        isDone.compareAndSet(false, true);
    }

    public boolean isDone(){
        return isDone.get();
    }
}
