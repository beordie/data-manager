package com.tipdm.framework.dmserver.core.scheduling.listener;

import com.tipdm.framework.common.utils.SpringUtils;
import com.tipdm.framework.dmserver.core.scheduling.job.LogCleanJobBean;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * Scheduler监听器
 * 调度器成功启动后自动创建数据源
 * 有效期扫描任务
 *
 * Created by TipDM on 2017/5/23.
 * E-mail:devp@tipdm.com
 */
public class SchedulerListener implements org.quartz.SchedulerListener {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerListener.class);

    @Override
    public void jobScheduled(Trigger trigger) {

    }

    @Override
    public void jobUnscheduled(TriggerKey triggerKey) {
//        logger.error("jobUnscheduled: {}", triggerKey.getName());
    }

    @Override
    public void triggerFinalized(Trigger trigger) {

//        logger.error("triggerFinalized: {}", trigger.getKey().getName());
    }

    @Override
    public void triggerPaused(TriggerKey triggerKey) {

    }

    @Override
    public void triggersPaused(String s) {

    }

    @Override
    public void triggerResumed(TriggerKey triggerKey) {

    }

    @Override
    public void triggersResumed(String s) {

    }

    @Override
    public void jobAdded(JobDetail jobDetail) {

    }

    @Override
    public void jobDeleted(JobKey jobKey) {

    }

    @Override
    public void jobPaused(JobKey jobKey) {

    }

    @Override
    public void jobsPaused(String s) {

    }

    @Override
    public void jobResumed(JobKey jobKey) {

    }

    @Override
    public void jobsResumed(String s) {

    }

    @Override
    public void schedulerError(String s, SchedulerException e) {
        logger.error(s);
        e.printStackTrace();
    }

    @Override
    public void schedulerInStandbyMode() {

    }

    private final static String TRIGGER_NAME = "trigger_clear_log";

    private final static String GROUP_NAME = "grp_clear_log";

    private Scheduler scheduler;

    @Override
    public void schedulerStarted() {

        logger.info("schedulerStarted");
        scheduler  = SpringUtils.getBean("scheduler", Scheduler.class);

        TriggerKey triggerKey = TriggerKey.triggerKey(TRIGGER_NAME, GROUP_NAME);
        JobKey jobKey = new JobKey(triggerKey.getName(), triggerKey.getGroup());
        try {
            //检查任务是否存在
            boolean exists = scheduler.checkExists(triggerKey);
            if(exists){
                scheduler.deleteJob(jobKey);
            }
        } catch (SchedulerException e) {
            logger.error("删除日志清除任务失败，错误信息: {}", e.getMessage());
        }

        //每周日的零点执行
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("0 0 0 ? * 1");
        // 按新的表达式构建一个新的trigger
        CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).withSchedule(scheduleBuilder).build();

        JobDetail jobDetail = JobBuilder.newJob(LogCleanJobBean.class).withIdentity(jobKey).build();
        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            logger.error("日志定时清理任务启动失败，错误信息：{}", e.getMessage());
        }

//        Thread thread = new Thread(new FailedJobHandler(scheduler));
//        thread.setDaemon(true);
//        thread.start();
    }

    @Override
    public void schedulerStarting() {
        logger.info("schedulerStarting");
    }

    @Override
    public void schedulerShutdown() {
        logger.info("schedulerShutdown");
    }


    @Override
    public void schedulerShuttingdown() {
        logger.info("schedulerShuttingdown");

        JobKey jobKey = new JobKey(TRIGGER_NAME, GROUP_NAME);
        try {
            scheduler.deleteJob(jobKey);
            logger.info("日志定时清理任务删除成功");
        } catch (SchedulerException e) {
            logger.error("删除日志定时清理任务失败，错误信息：{}", e.getMessage());
        }
    }

    @Override
    public void schedulingDataCleared() {

    }
}
