package com.tipdm.framework.dmserver.core.scheduling;

import com.github.dexecutor.core.task.*;
import com.tipdm.framework.common.utils.DateKit;
import com.tipdm.framework.common.utils.PropertiesUtil;
import com.tipdm.framework.common.utils.RedisUtils;
import com.tipdm.framework.common.utils.SpringUtils;
import com.tipdm.framework.dmserver.core.scheduling.listener.WorkFlowJobListener;
import com.tipdm.framework.dmserver.core.scheduling.model.IO;
import com.tipdm.framework.dmserver.core.scheduling.model.Job;
import com.tipdm.framework.dmserver.core.scheduling.model.Link;
import com.tipdm.framework.dmserver.core.scheduling.model.Node;
import com.tipdm.framework.dmserver.core.scheduling.job.WorkFlowJobBean;
import com.tipdm.framework.dmserver.rpc.MessageManager;
import com.tipdm.framework.dmserver.utils.Constants;
import com.tipdm.framework.dmserver.utils.RedissonUtils;
import com.tipdm.framework.dmserver.websocket.dto.Type;
import com.tipdm.framework.dmserver.websocket.dto.WorkFlowMessage;
import com.tipdm.framework.model.dmserver.Component;
import com.tipdm.framework.model.dmserver.ComponentIO;
import com.tipdm.framework.model.dmserver.DataTable;
import com.tipdm.framework.service.dmserver.ComponentService;
import com.tipdm.framework.service.dmserver.DataTableService;
import com.tipdm.framework.service.dmserver.MessageService;
import org.apache.commons.lang3.math.NumberUtils;
import org.quartz.*;
import org.quartz.impl.matchers.KeyMatcher;
import org.redisson.api.RMapCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Created by zhoulong on 2019/4/26.
 */
public class JobProvider implements TaskProvider<String, Boolean> {

    private final static Logger LOG = LoggerFactory.getLogger(JobProvider.class);

    private static DataTableService tableService = SpringUtils.getBean("tableService", DataTableService.class);

    private static ComponentService componentService = SpringUtils.getBean("componentService", ComponentService.class);

    private Scheduler scheduler;

    private Map<String, Job> jobMap;

    private WorkFlow workFlow;

    private AtomicBoolean abort;

    private String accessToken;

    private static Long expiredSeconds;

    public JobProvider(WorkFlow workFlow, Scheduler scheduler, AtomicBoolean abort, String accessToken) {
        this.scheduler = scheduler;
        this.workFlow = workFlow;
        this.jobMap = workFlow.getJobMap();
        this.abort = abort;
        this.accessToken = accessToken;
    }

    static {
        expiredSeconds = Long.parseLong(PropertiesUtil.getValue("sysconfig/redis.properties", "redis.tableExpired.seconds", "604800"));
    }

    @Override
    public Task<String, Boolean> provideTask(String jobId) {

        return new Task<String, Boolean>() {

            private ExecutorService executorService = Executors.newSingleThreadExecutor();

            @Override
            public Boolean execute() {
                Job job = jobMap.get(jobId);
                if (job == null) {
                    throw new TaskExecutionException("can not found job with id:" + jobId);
                }
                if (abort.get()) {
                    throw new TaskExecutionException("abort job with id:" + jobId);
                }

                try {
                    nodeConvert2Component(job);
//                    scheduleJob(job);

                    Future<State> future = executorService.submit(() -> {
                        scheduleJob(job);
                        ListenerManager listenerManager = scheduler.getListenerManager();
                        WorkFlowJobListener jobListener = (WorkFlowJobListener) listenerManager.getJobListener(job.getFullJobId());
                        //等待
                        while (!jobListener.isDone()){
                            TimeUnit.SECONDS.sleep(2);
                        }
                        State state = jobListener.getState();
                        //执行结束后删除监听器
                        listenerManager.removeJobListener(job.getFullJobId());
                        listenerManager.removeJobListenerMatcher(job.getFullJobId(), KeyMatcher.keyEquals(JobKey.jobKey(job.getJobName(), job.getJobGroup())));
                        return state;
                    });

                    State state = future.get(60, TimeUnit.MINUTES);
                    if(!state.equals(State.COMPLETE)) {
                        LOG.error("Job运行错误，id: {}", jobId);
                        return false;
                    } else {
                        changeLinkState(jobId);
                    }

                    //执行方式运行到此节点则不往后执行
                    return !job.isEndedNode();
                } catch (/*SchedulerException |*/ InterruptedException | ExecutionException | TimeoutException e) {
                    throw new TaskExecutionException(e.getMessage());
                } finally {
                    executorService.shutdown();
                }
            }

            @Override
            public boolean shouldExecute(ExecutionResults<String, Boolean> parentResults) {
                Boolean allowBeExecute = Boolean.TRUE;
                for (ExecutionResult<String, Boolean> res : parentResults.getAll()) {
                    if (res.getResult() == null || !res.getResult()) {
                        allowBeExecute = Boolean.FALSE;
                        break;
                    }
                }

                return allowBeExecute;
            }
        };
    }


    /**
     * 转换流程节点为Component对象
     * @param job
     */
    @SuppressWarnings("all")
    private void nodeConvert2Component(Job job) {
        Component component = new Component();
        component.setCreatorName(workFlow.getCreator());

        Node node = workFlow.getNodes().stream().filter(x -> x.getId().equals(job.getJobId())).findFirst().get();
        Component serverComponent = componentService.findOne(node.getServerId());
        BeanUtils.copyProperties(node, component, new String[]{"inputs", "outputs"});
        component.setClientId(Long.parseLong(node.getId()));
        if (null != serverComponent) {
            component.setId(node.getServerId());
            component.setEnabled(serverComponent.getEnabled());
            component.setScript(serverComponent.getScript());
            component.setExtra(serverComponent.getExtra());
        }

        for (IO io : node.getInputs()) {
            ComponentIO componentIO = new ComponentIO();
            componentIO.setType(ComponentIO.IOType.INPUT);
            componentIO.setModel(io.getIsModel());
            Optional<Link> optional = workFlow.getLinks().stream().filter(x -> x.getInputPortId().equals(io.getId())).findFirst();
            if (optional.isPresent()) {
                Link tmpLink = optional.get();
                Node tmpNode = workFlow.getNodes().stream().filter(x -> x.getId().equals(tmpLink.getSource())).findFirst().get();
                String tempTable = tmpNode.getOutputs().stream().filter(x -> x.getId().equals(tmpLink.getOutputPortId())).findFirst().get().getValue();

                componentIO.setNodeClientId(io.getId());
                if ("model".equals(io.getKey())) {
                    componentIO.setTempTable(tempTable);
                } else {
                    if (NumberUtils.isDigits(tempTable)) {
                        DataTable userTable = tableService.findOne(Long.parseLong(tempTable));
                        if (null != userTable) {
                            componentIO.setTempTable(userTable.getTableName());
                        }
                    } else {
                        componentIO.setTempTable("\"" + tempTable + "\"");
                    }
                }
                componentIO.setKey(io.getKey());
                component.getInputs().add(componentIO);
            }
        }

        for (IO io : node.getOutputs()) {
            ComponentIO componentIO = new ComponentIO();
            componentIO.setType(ComponentIO.IOType.OUTPUT);
            componentIO.setCanPreview(io.getCanPreview());
            componentIO.setModel(io.getIsModel());
            componentIO.setNodeClientId(io.getId());
            String tempTable = io.getValue();
            if ("model".equals(io.getKey())) {
                componentIO.setTempTable(tempTable);
            } else {
                //数据源
                if (NumberUtils.isDigits(tempTable)) {
                    DataTable userTable = tableService.findOne(Long.parseLong(tempTable));
                    if (null != userTable) {
                        componentIO.setTempTable(userTable.getTableName());
                    }
                } else {
                    //临时表
                    componentIO.setTempTable(tempTable);
                    RedisUtils.set("@pg:" + tempTable, null, expiredSeconds);
                }
            }
            componentIO.setKey(io.getKey());
            component.getOutputs().add(componentIO);
        }

        job.getAttachment().put("component", component);
        job.getAttachment().put("node", node);
    }

    /**
     * 调度
     * @param job
     * @throws SchedulerException
     */
    private void scheduleJob(Job job) throws SchedulerException {
        if (null == job) {
            return;
        }

        TriggerKey triggerKey = TriggerKey.triggerKey(job.getJobId(), job.getJobGroup());
        Trigger trigger = newTrigger(triggerKey);
        JobKey jobKey = new JobKey(job.getJobId(), job.getJobGroup());
        JobDetail jobDetail = JobBuilder.newJob(WorkFlowJobBean.class).withIdentity(jobKey).build();
        jobDetail.getJobDataMap().put("job", job);
        jobDetail.getJobDataMap().put("accessToken", accessToken);
        try {
            scheduler.getListenerManager().addJobListener(new WorkFlowJobListener(job.getFullJobId()), KeyMatcher.keyEquals(jobDetail.getKey()));
            Date date = scheduler.scheduleJob(jobDetail, trigger);
            LOG.info("jobName: {} triggerKey: {} date: {}", jobDetail.getKey().getName(), trigger.getKey().getName(),
                    DateKit.convert2Str("yyyy-MM-dd HH:mm:ss", date));
        } catch (SchedulerException e) {
            LOG.error("节点加入调度失败，错误信息: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 构建触发器
     * @param triggerKey
     * @return
     */
    public Trigger newTrigger(TriggerKey triggerKey) {
        Date startTime = DateBuilder.nextGivenSecondDate(null, 1);//延迟一秒钟执行
        LOG.info("job will execute at: {}", DateKit.convert2Str("yyyy-MM-dd HH:mm:ss", startTime));
        SimpleTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .startAt(startTime)
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule()
                                .withMisfireHandlingInstructionFireNow()//任务补偿策略：错过执行时间后立即执行
                                .withIntervalInSeconds(0)
                                .withRepeatCount(0))//不重复执行
                .build();
        return trigger;
    }

    /**
     * 变更连接线的状态
     * @param jobId
     */
    private void changeLinkState(String jobId){

        Set<String> links = workFlow.getLinks().stream().filter(x -> x.getSource().equals(jobId)).map(Link::getId).collect(Collectors.toSet());

        for(String link : links){
            WorkFlowMessage message = new WorkFlowMessage();
            message.setNodeId(link);
            message.setState(State.RUNNING);
            message.setType(Type.LINK);

            RMapCache<String, Map<String, String>> mapCache = RedissonUtils.getRMapCache(Constants.WS_CLIENTS);
            Map<String, String> mapping = mapCache.get(accessToken);
            String sessionId = mapping.get("sessionId");
            String rpcAddress = mapping.get("rpcAddress");
            MessageService messageService = MessageManager.getService(rpcAddress);
            messageService.notifyWorkFlowExecStatus(workFlow.getId(), sessionId, message);
        }
    }
}
