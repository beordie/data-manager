package com.tipdm.framework.dmserver.pyserve;

import com.jcraft.jsch.*;
import com.tipdm.framework.common.utils.RedisUtils;
import com.tipdm.framework.common.utils.StringKit;
import com.tipdm.framework.dmserver.exception.AlgorithmException;
import com.tipdm.framework.dmserver.utils.Constants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by TipDM on 2017/5/29.
 * E-mail:devp@tipdm.com
 * ssh会话, 支持在远程服务器上执行本地的Python文件
 */
public class PySession {

    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(PySession.class);

    private final static String SCRIPT_DIR = "/script/python";

    private Session session;

    private GenericObjectPool<PySession> pool;

    private ChannelSftp channelSftp;

    private ChannelExec channelExec;

    public PySession(Session session){
        this.session = session;
        buildChannel();
        Assert.notNull(channelSftp, "can not builder ChannelSftp");
    }

    private void buildChannel(){
        try {
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect(1000 * 30);
            channelSftp.setFilenameEncoding("UTF-8");

            //检查目录
            mkdir(Constants.PYSERVER_COMMON_DIR);
            mkdir(Constants.PYSERVER_COMMON_DIR + "/report");
            mkdir(Constants.PYSERVER_COMMON_DIR + "/model");
            mkdir(Constants.PYSERVER_COMMON_DIR + "/imageFile");

            String path = this.getClass().getClassLoader().getResource("/").getPath() + SCRIPT_DIR;
            //如果是window系统，去除开头的"/"
            if(SystemUtils.IS_OS_WINDOWS && path.startsWith("/")){
                path = StringKit.substringAfter(path, "/");
            }
            File dir = new File(path);
            Collection<File> files = FileUtils.listFiles(dir, new String[]{"py"}, true);

            for(File file : files){
                try {
                    channelSftp.ls(Constants.PYSERVER_COMMON_DIR + "/" + file.getName());
                } catch (SftpException e) {
                    channelSftp.put(file.getAbsolutePath(), Constants.PYSERVER_COMMON_DIR);
                }
            }

        } catch (JSchException e) {
            logger.error(e.getMessage());
            channelSftp = null;
        } catch (SftpException e){
            logger.error(e.getMessage());
            channelSftp = null;
        } catch(Exception e){
            logger.error(e.getMessage());
            channelSftp = null;
        }finally {
            if(channelSftp != null) {
                channelSftp.disconnect();
            }
        }
    }

    /**
     * 传输文件到远程服务器
     * @param localFile
     */
    public void sendFile(File localFile, String remoteFile) throws IOException, JSchException, SftpException{
        if(!localFile.exists()){
            return;
        }

        try(FileInputStream is = new FileInputStream(localFile)) {
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect(1000 * 30);
            channelSftp.setFilenameEncoding("UTF-8");
            String fileName = localFile.getName();
            channelSftp.put(is, remoteFile, ChannelSftp.OVERWRITE);
        } finally {
            if(channelSftp != null){
                channelSftp.disconnect();
            }
        }
    }

    private void mkdir(String dir) throws SftpException {
        try {
            channelSftp.ls(dir);
        } catch (SftpException e) {
            channelSftp.mkdir(dir);
        }
    }

    /**
     * 远程执行python
     * @param localFile 本地Python文件
     */
    public void train(File localFile, String reportFile, String model) throws FileNotFoundException, AlgorithmException {
        List<String> msgQueue = new ArrayList<>();
        FileInputStream is = null;
        try {
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect(1000 * 30);
            channelSftp.setFilenameEncoding("UTF-8");
            String fileName = localFile.getName();
            is = new FileInputStream(localFile);
            AtomicBoolean isDone = new AtomicBoolean(false);
            PySftpProgressMonitor monitor = new PySftpProgressMonitor(session, msgQueue, isDone);
            channelSftp.put(is,  Constants.PYSERVER_COMMON_DIR + "/"+fileName, monitor, ChannelSftp.OVERWRITE);

            while (!isDone.get()) {
                //nothing to do // TODO: 2017/7/13
            }
            logger.info("python 算法执行完毕...");
            try {
                //检测是否有算法报告文件
                logger.info("check exists algorithm report");
                channelSftp.ls( Constants.PYSERVER_COMMON_DIR + "/report/"+reportFile);
                channelSftp.get(Constants.PYSERVER_COMMON_DIR + "/report/"+reportFile, RedisUtils.get(Constants.REPORT_DIR, String.class) + "/" + reportFile);
            }catch (SftpException e){
                logger.warn("can't download reportFile from {} faild:{}", Constants.PYSERVER_COMMON_DIR + "/report/", e.getMessage());
            }
            try {
                //检测是否有return模型
                logger.info("check exists model file");
                channelSftp.ls(Constants.PYSERVER_COMMON_DIR + "/model/"+model);
                channelSftp.get(Constants.PYSERVER_COMMON_DIR + "/model/"+model, RedisUtils.get(Constants.MODEL_DIR, String.class) + "/" + model);
            }catch (SftpException e){
                logger.warn("can't download model from {} faild:{}", Constants.PYSERVER_COMMON_DIR + "/model/", e.getMessage());
            }
        } catch (JSchException e) {
            throw new AlgorithmException(e.getMessage());
        } catch (SftpException e) {
            throw new AlgorithmException(e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(null != channelSftp){
                channelSftp.disconnect();
            }

            if (null != channelExec) {
                channelExec.disconnect();
            }
            if(is != null){
                try {
                    is.close();
                    localFile.delete();
                } catch (IOException e) {

                }
            }
        }
    }

    public void predict(File localFile, String reportFile, File model) throws FileNotFoundException, AlgorithmException {
        List<String> msgQueue = new ArrayList<>();
        FileInputStream is = null;
        try {
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect(1000 * 30);
            channelSftp.setFilenameEncoding("UTF-8");
            //上传模型文件
            channelSftp.put(new FileInputStream(model), Constants.PYSERVER_COMMON_DIR + "/model/"+ model.getName(), ChannelSftp.OVERWRITE);
            String fileName = localFile.getName();
            is = new FileInputStream(localFile);
            AtomicBoolean isDone = new AtomicBoolean(false);
            PySftpProgressMonitor monitor = new PySftpProgressMonitor(session, msgQueue, isDone);
            channelSftp.put(is, Constants.PYSERVER_COMMON_DIR + "/"+fileName, monitor, ChannelSftp.OVERWRITE);

            while (!isDone.get()) {
                //nothing to do // TODO: 2017/7/13
            }
            logger.info("python 算法执行完毕...");
            try {
                //检测是否有算法报告文件
                logger.info("check exists algorithm report");
                channelSftp.ls(Constants.PYSERVER_COMMON_DIR + "/report/"+reportFile);
                channelSftp.get(Constants.PYSERVER_COMMON_DIR + "/report/"+reportFile, RedisUtils.get(Constants.REPORT_DIR, String.class) + "/" + reportFile);
            }catch (SftpException e){
                logger.warn("can't download reportFile from {} faild:{}", Constants.PYSERVER_COMMON_DIR + "/report/", e.getMessage());
            }
        } catch (JSchException e) {
            throw new AlgorithmException(e.getMessage());
        } catch (SftpException e) {
            throw new AlgorithmException(e.getMessage());
        } finally {
            if(null != channelSftp){
                channelSftp.disconnect();
            }
            if(is != null){
                try {
                    is.close();
                    localFile.delete();
                } catch (IOException e) {

                }
            }
        }
    }

    protected void SetPool(GenericObjectPool<PySession> pool){
        this.pool = pool;
    }

    protected void disconnect(){
        if(null != session) {
            try {
                session.disconnect();
            } catch (Exception ex){
                session = null;
            }
        }
    }

    protected boolean connected(){
        return session.isConnected();
    }

    public void close(){
        try{
            pool.returnObject(this);
        }catch(Exception ex){

        }
    }
}
