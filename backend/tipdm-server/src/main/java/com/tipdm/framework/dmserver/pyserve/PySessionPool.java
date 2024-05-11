package com.tipdm.framework.dmserver.pyserve;

import com.tipdm.framework.dmserver.exception.ConnectionException;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by TipDM on 2017/5/31.
 * E-mail:devp@tipdm.com
 */
public class PySessionPool {

    private final static Logger logger = LoggerFactory.getLogger(PySessionPool.class);

    private GenericObjectPool<PySession> pool;

    public PySessionPool(GenericObjectPoolConfig config){
        pool = new GenericObjectPool<PySession>(new PySessionFactory(), config);
        addShutdownHook();
    }

    public synchronized PySession getSession() throws ConnectionException {

        try {
            PySession session = pool.borrowObject();
            session.SetPool(pool);
            return session;
        } catch (Exception ex) {
            throw new ConnectionException("暂无可用的PyServe连接处理您的操作，请稍后再试！", ex);
        }
    }
    /**
     * 关闭连接池
     *
     */
    private void close(){
        System.err.println("close PySession pool");
        pool.clear();
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(PySessionPool.class.getSimpleName() + "-ShutdownHook") {
            public void run() {
                PySessionPool.this.close();
            }
        });
    }
}

