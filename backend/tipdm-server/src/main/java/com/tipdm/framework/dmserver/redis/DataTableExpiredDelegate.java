package com.tipdm.framework.dmserver.redis;


import com.tipdm.framework.common.utils.StringKit;
import com.tipdm.framework.service.dmserver.DataTableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by TipDM on 2017/8/15.
 * E-mail:devp@tipdm.com
 */
@Component
public class DataTableExpiredDelegate implements MessageDelegate{

    private final static Logger logger = LoggerFactory.getLogger(DataTableExpiredDelegate.class);

    @Autowired
    private DataTableService tableService;

    @Override
    public void handleMessage(String message) {
        logger.info("handle String message");
    }

    @Override
    public void handleMessage(Map message) {
        logger.info("handle Map message");
    }

    @Override
    public void handleMessage(byte[] message) {
        logger.info("handle byte[] message");
    }

    @Override
    public void handleMessage(Serializable message) {
        logger.info("handle Serializable message");
    }

    @Override
    public void handleMessage(Serializable key, String channel) {
        logger.info("handle event:{}, key:{}", channel, key);

        String trueKey = (String) key;
        //存储在postgresql中的表
        if(trueKey.startsWith("@pg:")){
            //从postgresql库删除表
            tableService.dropExpiredTable(StringKit.substringAfter(trueKey, "@pg:"));
        }
    }
}
