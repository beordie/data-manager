package com.tipdm.framework.dmserver.core.algo.unparallel.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.jayway.jsonpath.JsonPath;
import com.tipdm.framework.common.utils.RedisUtils;
import com.tipdm.framework.common.utils.StringKit;
import com.tipdm.framework.dmserver.core.algo.unparallel.executor.ExecutorContext;
import com.tipdm.framework.dmserver.core.scheduling.model.Job;
import com.tipdm.framework.dmserver.core.scheduling.JobContext;
import com.tipdm.framework.dmserver.exception.AlgorithmException;
import com.tipdm.framework.dmserver.mse.ModelUtil;
import com.tipdm.framework.dmserver.pyserve.PySession;
import com.tipdm.framework.dmserver.rpc.MessageManager;
import com.tipdm.framework.dmserver.utils.Constants;
import com.tipdm.framework.dmserver.utils.RedissonUtils;
import com.tipdm.framework.dmserver.websocket.dto.ModelMessage;
import com.tipdm.framework.model.dmserver.Component;
import com.tipdm.framework.model.dmserver.ComponentIO;

import com.tipdm.framework.model.dmserver.Model;
import com.tipdm.framework.service.dmserver.MessageService;
import org.apache.commons.lang3.ArrayUtils;
import org.dmg.pmml.Field;
import org.redisson.api.RMapCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by TipDM on 2017/6/21.
 * E-mail:devp@tipdm.com
 */
@SuppressWarnings("all")
public class PyAlgorithmListener implements AlgorithmListener {

    private final Logger logger = LoggerFactory.getLogger(PyAlgorithmListener.class);

    @Override
    public void init(ExecutorContext executorContext) throws AlgorithmException {


    }

    @Override
    public void wasExecuted(ExecutorContext executorContext, AlgorithmException ex) {

        //接收算法报告 & 保存模型
        if (null == ex) {
            PySession session = (PySession) executorContext.getSession();
            Component component = executorContext.getComponent();
            //接收报告
            receiveReport(session, component);

            //保存模型
            saveModel(session, component);
        }
    }

    void receiveReport(PySession session, Component component) {

    }

    void saveModel(PySession session, Component component) {
        File file = null;
        try {

            logger.info("检测算法是否有模型输出");
            Optional<ComponentIO> optional = component.getOutputs().stream().filter(x -> "model".equals(x.getKey())).findFirst();
            if (!optional.isPresent()) {
                logger.error("未能检测到模型相关配置");
                return;
            }

            Map<String, String> params = component.getParameters();
            if (!params.containsKey("features")) {
                logger.warn("参数缺失，参数名：{}", "features");
                return;
            }

            logger.info("开始进行模型序列化操作");
            ComponentIO io = optional.get();
            Long clientId = component.getClientId();
            Model model = new Model();
            model.setCreatorName(component.getCreatorName());
            model.setModelName(component.getName());
            model.setReportFile(clientId + ".html");

            //将输出点的ID作为模型文件名
            String modelDir = RedisUtils.get(Constants.MODEL_DIR, String.class);
            file = new File(modelDir, io.getTempTable());

            Map<String, Object> data = JobContext.get();
            String accessToken = (String)data.get("accessToken");
            Job job = (Job)data.get("job");
            net.minidev.json.JSONArray featuresItems = JsonPath.parse(JSON.toJSONString(job.getAttachment().get("node"))).read("$..featuresItems");
            JSONArray features = JSON.parseArray(featuresItems.get(0).toString());

            List<Field> fields = ModelUtil.decodeValues(features);
            List<Field> targetList = ModelUtil.filter(fields, StringKit.split(params.get("label"), ","));
            List<Field> featureList = ModelUtil.filter(fields, StringKit.split(params.get("features"), ","));

            model.setFeatures(JSON.toJSONString(featureList));
            model.setTarget(JSON.toJSONString(targetList));
            model.setOutputs(JSON.toJSONString(fields));

            model.setModelPath(file.getName());
            model.setNodeId(io.getTempTable());
            //保存建模节点的ID，供预测环节获取预测脚本
            model.setComponentId(component.getId());

            RedisUtils.putToMap(Constants.UN_SAVED_MODEL, io.getTempTable(), model, 72L);
            ModelMessage message = new ModelMessage();
            message.setModelName(model.getModelName());
            message.setNodeId(io.getTempTable());
            message.setWorkFlowId(job.getJobGroup());

            //推送到客户端未保存模型列表
            RMapCache<String, Map<String, String>> mapCache = RedissonUtils.getRMapCache(Constants.WS_CLIENTS);
            Map<String, String> mapping = mapCache.get(accessToken);
            String sessionId = mapping.get("sessionId");
            String rpcAddress = mapping.get("rpcAddress");
            MessageService messageService = MessageManager.getService(rpcAddress);
            messageService.pushUnSavedModel(job.getJobGroup(), sessionId, message);

        } catch (Exception e) {
            logger.error("保存算法模型出错，错误信息: {}", e.getMessage());
            if (file != null) {
                file.delete();
            }
        }
    }

    List<Field> filter(List<Field> fields, String[] find) {
        return fields.stream().filter(x -> ArrayUtils.contains(find, x.getName())).collect(Collectors.toList());
    }
}
