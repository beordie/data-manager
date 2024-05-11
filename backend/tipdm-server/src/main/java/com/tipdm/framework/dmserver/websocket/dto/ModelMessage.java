package com.tipdm.framework.dmserver.websocket.dto;


/**
 * Created by TipDM on 2017/3/15.
 * E-mail:devp@tipdm.com
 */
public class ModelMessage extends Message{

    private String modelName;

    private String workFlowId;

    private String modelPath;

    public ModelMessage(){
        super(Category.MODEL);
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public String getWorkFlowId() {
        return workFlowId;
    }

    public void setWorkFlowId(String workFlowId) {
        this.workFlowId = workFlowId;
    }
}
