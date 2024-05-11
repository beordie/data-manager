package com.tipdm.framework.dmserver.core.algo.unparallel.interpreter;

import com.tipdm.framework.common.Constants;
import com.tipdm.framework.common.utils.FileKit;
import com.tipdm.framework.common.utils.PropertiesUtil;
import com.tipdm.framework.dmserver.exception.PythonInterpreterException;
import com.tipdm.framework.common.utils.RedisUtils;
import org.apache.commons.collections.MapUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by TipDM on 2017/4/17.
 * E-mail:devp@tipdm.com
 */
@SuppressWarnings("all")
public class PythonInterpreter {

    private final static String SCRIPT_DIR = "com/tipdm/framework/dmserver/algo/unparallel/script/python";
    private final static String REPORT_DIR = "report/";
    private final static String MODEL_DIR = "model/";

    private final static String TAB = "    ";


    private PythonInterpreter() {

    }

    private static StringBuilder init(String user, String password, String script, Map<String, String> inputs, Map<String, String> outputs, Map<String, String> params){
        LinkedHashMap<String, String> dbConfig = PropertiesUtil.getProperties("sysconfig/database.properties");
        String url = dbConfig.get("db.url");
        URI uri = URI.create(url.substring(5));
        String host = uri.getHost();
        int port = uri.getPort();
        String dbname = uri.getPath().substring(1);
//        user = dbConfig.get("db.user");
//        password = dbConfig.get("db.password");

        StringBuilder sb = new StringBuilder("#coding=utf-8\nimport pandas as pd\n" +
                "import sys\n" +
                "sys.path.append('" + com.tipdm.framework.dmserver.utils.Constants.PYSERVER_COMMON_DIR + "')\n" +
                "import db_utils as db_utils\n" +
                "import report_utils as report_utils\n" +
                "from sklearn.externals import joblib\n" +
                "import psycopg2 as pg\n");

        //import pickle
        sb.append("import pickle").append("\n");
        //
        sb.append(script).append("\n");
        sb.append("conn = None").append("\n");
        sb.append("report = report_utils.Report()").append("\n");
        sb.append("model = None").append("\n");
        sb.append("try:").append("\n");
        sb.append(TAB).append("conn = db_utils.getConnection(host=\"").append(host)
                .append("\", port=").append(port)
                .append(", dbname=\"").append(dbname)
                .append("\", user=\"").append(user)
                .append("\", password=\"").append(password)
                .append("\", schema=\"").append(user)
                .append("\")\n");

        sb.append(TAB).append("inputs = {}\n");
        if (MapUtils.isNotEmpty(inputs)) {
            for (Map.Entry<String, String> input : inputs.entrySet()) {
                sb.append(TAB).append("inputs['" + input.getKey() + "'] = ").append("'" + input.getValue() + "'")
                        .append("\n");
            }
        }

        sb.append(TAB).append("outputs = {}\n");
        if (MapUtils.isNotEmpty(outputs)) {
            for (Map.Entry<String, String> output : outputs.entrySet()) {
                sb.append(TAB).append("outputs['" + output.getKey() + "'] = ").append("'" + output.getValue() + "'")
                        .append("\n");
            }
        }

        sb.append(TAB).append("params = {}\n");
        if (MapUtils.isNotEmpty(params)) {
            for (Map.Entry<String, String> param : params.entrySet()) {
                sb.append(TAB).append("params['" + param.getKey() + "'] = ").append("'" + param.getValue() + "'")
                        .append("\n");
            }
        }

        return sb;
    }

    public static File makePredictFile(String user, String password, String script, Map<String, String> inputs, Map<String, String> outputs, Map<String, String> params, String reportFile, File model) throws PythonInterpreterException {
        File file = null;
        try {
            StringBuilder sb = init(user, password, script, inputs, outputs, params);
            reportFile =   REPORT_DIR + reportFile;
            sb.append(TAB).append("reportFileName = \"" + reportFile).append("\"\n");
            sb.append(TAB).append("with open(\"" + MODEL_DIR + model.getName() + "\",'rb') as f:").append("\n");
            sb.append(TAB).append(TAB).append("model = joblib.load(f)").append("\n");
            sb.append(TAB).append("doPredict(conn, model, inputs, params, outputs, reportFileName)").append("\n");
//            sb.append(TAB).append("doPredict(model, None)").append("\n");
            sb.append("finally:").append("\n");

            //关闭连接
            sb.append(TAB).append("db_utils.closeConnection(conn)").append("\n");
            script = sb.toString();
            file = new File(RedisUtils.get("upload/tmp", String.class), UUID.randomUUID().toString() + ".py");
            FileKit.writeStringToFile(file, script, Constants.CHARACTER);
            return file;
        } catch (IOException e) {
            throw new PythonInterpreterException(e);
        }
    }

    public static File makeTrainFile(String user, String password, String script, Map<String, String> inputs, Map<String, String> outputs, Map<String, String> params, String reportFile, String modelFile) throws PythonInterpreterException {
        File file = null;
        try {
            StringBuilder sb = init(user, password, script, inputs, outputs, params);

            reportFile = REPORT_DIR + reportFile;
            sb.append(TAB).append("reportFileName = \"" + reportFile).append("\"\n");
            sb.append(TAB).append("model = execute(conn, inputs, params, outputs, reportFileName)").append("\n");
            sb.append("finally:").append("\n");
            //存储模型
            sb.append(TAB).append("if model != None:").append("\n");
            modelFile = MODEL_DIR + modelFile;
            sb.append(TAB).append(TAB).append("joblib.dump(model, '" + modelFile + "')").append("\n");
            //关闭连接
            sb.append(TAB).append("db_utils.closeConnection(conn)").append("\n");
            script = sb.toString();
            file = new File(RedisUtils.get("upload/tmp", String.class), UUID.randomUUID().toString() + ".py");
            FileKit.writeStringToFile(file, script, Constants.CHARACTER);
            return file;
        } catch (IOException e) {
            throw new PythonInterpreterException(e);
        }
    }

    public static File makeEvalFile(String user, String password, String script, Map<String, String> inputs, Map<String, String> outputs, Map<String, String> params, String reportFile, File model) throws PythonInterpreterException {
        File file = null;
        try {
            StringBuilder sb = init(user, password, script, inputs, outputs, params);

            reportFile = REPORT_DIR + reportFile;
            sb.append(TAB).append("reportFileName = \"" + reportFile).append("\"\n");
            sb.append(TAB).append("with open(\"" + MODEL_DIR + model.getName() + "\",'rb') as f:").append("\n");
            sb.append(TAB).append(TAB).append("model = joblib.load(f)").append("\n");
            sb.append(TAB).append("evaluate(conn, model, inputs, params, outputs, reportFileName)").append("\n");
            sb.append("finally:").append("\n");
            //关闭连接
            sb.append(TAB).append("db_utils.closeConnection(conn)").append("\n");
            script = sb.toString();
            file = new File(RedisUtils.get("upload/tmp", String.class), UUID.randomUUID().toString() + ".py");
            FileKit.writeStringToFile(file, script, Constants.CHARACTER);
            return file;
        } catch (IOException e) {
            throw new PythonInterpreterException(e);
        }
    }
}
