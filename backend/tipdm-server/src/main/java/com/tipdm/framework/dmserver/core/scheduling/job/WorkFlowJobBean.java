package com.tipdm.framework.dmserver.core.scheduling.job;

import com.tipdm.framework.common.utils.PropertiesUtil;
import com.tipdm.framework.dmserver.core.algo.IAlgorithm;
import com.tipdm.framework.dmserver.core.scheduling.JobContext;
import com.tipdm.framework.dmserver.core.scheduling.model.Job;
import com.tipdm.framework.model.dmserver.Component;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * @author zhoulong E-mail:devp@tipdm.com
 * @version 创建时间：2016年11月7日 下午2:25:17 类说明
 */
//@PersistJobDataAfterExecution
//@DisallowConcurrentExecution
public class WorkFlowJobBean extends QuartzJobBean {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    public void executeInternal(JobExecutionContext context) throws JobExecutionException {

        try {
            JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
            Job job = (Job) jobDataMap.get("job");
            Object attachment = job.getAttachment().get("component");
            Component component = (Component) attachment;

            truncateLog(component);
            Class clazz;
            try {
                clazz = Class.forName(job.getTargetClazz());
            }catch (ClassNotFoundException ex){
                logger.warn("无法载入算法类：{}，尝试以通用算法执行算法！", job.getTargetClazz());
                clazz = Class.forName("com.tipdm.framework.dmserver.core.algo.unparallel.CommonAlgorithm");
            }
            IAlgorithm algorithm = (IAlgorithm) clazz.newInstance();
            if (!component.getEnabled()) {
                throw new IllegalAccessException("组件初始化失败，错误信息：当前组件已被管理员禁用");
            }

            String accessToken = jobDataMap.getString("accessToken");
            JobContext.addEntry("accessToken", accessToken);
            JobContext.addEntry("job", job);
            algorithm.run(component);
        } catch (Exception e) {
            throw new JobExecutionException(e);
        } finally {
            JobContext.clean();
        }
    }

    @SuppressWarnings("all")
    private void truncateLog(Component component) {
        String id = component.getClientId().toString();
        String log_home = PropertiesUtil.getValue("sysconfig/system.properties", "LOG_HOME");
        try {
            PrintWriter writer = new PrintWriter(new File(log_home + "/" + id + ".log"));
            writer.print("");
            writer.flush();
            writer.close();
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException: {}", e);
        }
        MDC.put("component", id);
    }
}
