package com.tipdm.framework.service.dmserver.impl;

import com.tipdm.framework.dmserver.websocket.SocketServer;
import com.tipdm.framework.dmserver.websocket.dto.Message;
import com.tipdm.framework.service.dmserver.MessageService;

/**
 * Created by zhoulong on 2019/5/5.
 */
public class MessageServiceImpl implements MessageService {

    @Override
    public void notifyWorkFlowExecStatus(String workFlowId, String sessionId, Message msg) {
        SocketServer.notifyWorkFlowExecStatus(workFlowId, sessionId, msg);
    }

    @Override
    public void pushUnSavedModel(String workFlowId, String sessionId, Message msg) {
        SocketServer.notifyWorkFlowExecStatus(workFlowId, sessionId, msg);
    }
}
