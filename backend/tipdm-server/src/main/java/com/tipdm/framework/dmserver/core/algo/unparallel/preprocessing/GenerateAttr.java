package com.tipdm.framework.dmserver.core.algo.unparallel.preprocessing;

import com.tipdm.framework.common.utils.StringKit;
import com.tipdm.framework.dmserver.exception.AlgorithmException;
import com.tipdm.framework.dmserver.core.algo.unparallel.AbstractAlgorithm;
import org.slf4j.helpers.MessageFormatter;

import java.sql.SQLException;
import java.util.Map;

/**
 * Created by TipDM on 2017/2/27.
 * E-mail:devp@tipdm.com
 * 衍生变量
 */
public class GenerateAttr extends AbstractAlgorithm {

    @Override
    protected void execute() throws AlgorithmException {

        try {
            Map<String, String> params = component.getParameters();
            //变量名
            String varname = params.get("varname");
            String columns = params.get("columns");
            if (StringKit.isBlank(columns)) {
                columns = "*";
            }
            //表达式
            String expression = params.get("expression");
            expression = "(" + expression + ") as " + varname;
            String inputTable = getInputs().get("input");
            if (!StringKit.contains(inputTable, "\"" + component.getCreatorName() + "\".")) {
                inputTable = "\"" + component.getCreatorName() + "\"." + inputTable;
            }
            String outputTable = "\"" + component.getCreatorName() + "\".\"" + getOutputs().get("output") + "\"";

            String truncateSQL = "drop table if exists " + outputTable;
            String querySQL = MessageFormatter.arrayFormat("create table {} as select {}, {} from {}", new Object[]{outputTable, columns, expression, inputTable}).getMessage();

            String[] sqls = new String[]{truncateSQL, querySQL};
            tableService.batchExecuteSQL(sqls);
            tableService.changeTableOwner(outputTable, component.getCreatorName());
        } catch (SQLException e) {
            throw new AlgorithmException(e.getMessage());
        } catch (Exception e) {
            throw new AlgorithmException(e.getMessage());
        }
//        StringBuilder sb = new StringBuilder();
//        sb.append("source(dbUtils.R);\n" +
//                "conn <- tipdm.getConnection(host, port, dbname, user, password);\n");
//
//        sb.append("tipdm.dropTable(conn, outputs$output);\n");
//        String var = "("+expression+ ") as "+ varname;
//        Map<String, String> inputs = new HashMap<>();
//
//        for(ComponentIO io : component.getInputs()){
//            if(StringKit.isNotBlank(io.getTempTable())){
//                sb.append("sql <- paste('select *, ', '"+ var +"', ' into ', outputs$output, ' from ', inputs$"+ io.getKey() +");\n");
//                sb.append("data <- tipdm.query(conn, sql);\n");
//                break;
//            }
//        }
////        sb.append("dbWriteTable(conn, outputs$output, data);\n")
//        sb.append("tipdm.closeConnection(conn);");
//        String rscript = sb.toString();
//        script = RScript.createFromScriptString(rscript);
//        try {
//            script.execute(session);
//        } catch (Exception e) {
//            throw new AlgorithmException(e);
//        }
    }

}
