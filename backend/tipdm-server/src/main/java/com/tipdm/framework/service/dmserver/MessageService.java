package com.tipdm.framework.service.dmserver;

import com.tipdm.framework.dmserver.websocket.dto.Message;

/**
 * Created by TipDM on 2019/5/5.
 */
public interface MessageService {

    //推送流程运行状态
    public void notifyWorkFlowExecStatus(String workFlowId, String sessionId, Message msg);

    public void pushUnSavedModel(String workFlowId, String sessionId, Message msg);
}
