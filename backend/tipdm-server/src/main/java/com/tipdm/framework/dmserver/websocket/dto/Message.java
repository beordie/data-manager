package com.tipdm.framework.dmserver.websocket.dto;

import java.io.Serializable;

/**
 * Created by TipDM on 2018/1/13.
 * E-mail:devp@tipdm.com
 */
public class Message implements Serializable{

    private Category category;

    private String nodeId;

    public Message(Category category){
        this.category = category;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
}
