package com.tipdm.framework.controller.dmserver.dto;

//import io.swagger.annotations.ApiModel;
//import io.swagger.annotations.ApiModelProperty;

/**
 * Created by TipDM on 2017/3/16.
 * E-mail:devp@tipdm.com
 */
//@ApiModel(value = "模型")
public class Model {

//    @ApiModelProperty(value = "工程调度任务提交后返回的流程Id", required = true)
    private String workFlowId;

//    @ApiModelProperty(value = "模型对应组件的客户端ID(唯一)", required = true)
    private String nodeClientId;

//    @ApiModelProperty(value = "模型名称", required = true)
    private String modelName;

    public String getWorkFlowId() {
        return workFlowId;
    }

    public void setWorkFlowId(String workFlowId) {
        this.workFlowId = workFlowId;
    }

    public String getNodeClientId() {
        return nodeClientId;
    }

    public void setNodeClientId(String nodeClientId) {
        this.nodeClientId = nodeClientId;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
}
