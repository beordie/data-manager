package com.tipdm.framework.dmserver.websocket.dto;

import com.tipdm.framework.dmserver.core.scheduling.State;

/**
 * Created by TipDM on 2017/1/18.
 * E-mail:devp@tipdm.com
 */
public class WorkFlowMessage extends Message{

    public WorkFlowMessage(){
        super(Category.WORKFLOW);
    }

    private Type type = Type.NODE;

    private State state = State.INIT;

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

}

