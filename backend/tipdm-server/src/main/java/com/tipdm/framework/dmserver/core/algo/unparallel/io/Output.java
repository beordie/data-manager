package com.tipdm.framework.dmserver.core.algo.unparallel.io;


import com.alibaba.fastjson.JSONObject;
import com.tipdm.framework.common.utils.PropertiesUtil;
import com.tipdm.framework.common.utils.StringKit;
import com.tipdm.framework.dmserver.core.algo.IAlgorithm;
import com.tipdm.framework.dmserver.exception.AlgorithmException;
import com.tipdm.framework.dmserver.utils.DBUtils;
import com.tipdm.framework.model.dmserver.Component;
import com.tipdm.framework.model.dmserver.ComponentIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.helpers.MessageFormatter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by TipDM on 2017/4/25.
 * E-mail:devp@tipdm.com
 * 数据输出组件
 */
public class Output implements IAlgorithm {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void run(Component component) throws AlgorithmException {
        //清除上一次运行的日志
        truncateLog(component);
        logger.info("开始写入数据...");
        Map<String, String> params = component.getParameters();

        if (component.getInputs().size() == 0) {
            throw new AlgorithmException("输入源不能为空");
        }
        ComponentIO input = component.getInputs().stream().findFirst().get();
        String sourceTable = input.getTempTable();
        String targetTable = params.get("tableName");

        if (StringKit.isBlank(targetTable)) {
            String error = MessageFormatter.format("参数\"{}\"不能为空", "tableName").getMessage();
            throw new AlgorithmException(error);
        }
        //是否在插入前清除目标表
        Boolean truncate = Boolean.parseBoolean(params.get("truncate"));

        Properties properties = PropertiesUtil.loadProperties("/sysconfig/database.properties");
        String url = properties.getProperty("db.url");
        String user = properties.getProperty("db.user");
        String password = properties.getProperty("db.password");
        try {
            String targetUrl = params.get("url");
            String targetUser = params.get("user");
            String targetPassword = params.get("password");

            JSONObject readerPlugin = DBUtils.getReaderTemplate("postgresqlreader");
            JSONObject parameter = readerPlugin.getJSONObject("parameter");
            parameter.put("username", user);
            parameter.put("password", password);
            Map<String, Object> connection = new HashMap<>();
            connection.put("jdbcUrl", new String[]{url});
            connection.put("querySql", new String[]{"select * from " + sourceTable});
            parameter.getJSONArray("connection").set(0, connection);
            readerPlugin.put("parameter", parameter);

            JSONObject job = DBUtils.initJob(targetUrl, targetUser, targetPassword, targetTable, truncate);
            job.getJSONObject("job").getJSONArray("content").getJSONObject(0).put("reader", readerPlugin);
            DBUtils.dataSync(job);
        } catch (Throwable e) {
            logger.error(e.getMessage());
            throw new AlgorithmException(e);
        } finally {

        }
    }

    @SuppressWarnings("all")
    protected void truncateLog(Component component) {
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
