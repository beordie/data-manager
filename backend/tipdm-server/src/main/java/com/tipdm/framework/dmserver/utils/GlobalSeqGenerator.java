package com.tipdm.framework.dmserver.utils;

import com.tipdm.framework.common.utils.RedisUtils;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by TipDM on 2016/12/14.
 * E-mail:devp@tipdm.com
 */
public final class GlobalSeqGenerator {

    private final static Long MIN_SEQ_ID = 10000000L;
    private static AtomicLong seq = null;

    private GlobalSeqGenerator(){

    }

    static {
        Long lastSeqId = RedisUtils.get("lastSeqId", Long.class);
        if(null == lastSeqId){
            lastSeqId = MIN_SEQ_ID;
        }
        seq = new AtomicLong(lastSeqId);
    }

    public static synchronized Long getNextId(){
        Long seqId = seq.incrementAndGet();
        RedisUtils.set("lastSeqId", seqId);
        return seqId;
    }
}
