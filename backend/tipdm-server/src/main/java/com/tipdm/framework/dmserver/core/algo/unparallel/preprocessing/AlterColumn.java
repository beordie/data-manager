package com.tipdm.framework.dmserver.core.algo.unparallel.preprocessing;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.tipdm.framework.common.utils.StringKit;
import com.tipdm.framework.dmserver.core.algo.unparallel.AbstractAlgorithm;
import com.tipdm.framework.dmserver.exception.AlgorithmException;
import org.slf4j.helpers.MessageFormatter;

import java.util.List;
import java.util.Map;

/**
 * Created by TipDM on 2017/6/1.
 * E-mail:devp@tipdm.com
 * 修改列名
 */
public class AlterColumn extends AbstractAlgorithm {

    @Override
    public void execute() throws AlgorithmException {
        try {
            Map<String, String> params = component.getParameters();
            String text = params.get("columns");
            List<Column> columnList = JSON.parseObject(text, new TypeReference<List<Column>>() {
            });
            if (!getInputs().containsKey("data_in")) {
                throw new AlgorithmException("输入项【data_in】缺失");
            }

            String inputTable = getInputs().get("data_in");
            if (!StringKit.contains(inputTable, "\"" + component.getCreatorName() + "\".")) {
                inputTable = "\"" + component.getCreatorName() + "\"." + inputTable;
            }

            if (!getOutputs().containsKey("data_out")) {
                throw new AlgorithmException("输出项【data_out】缺失");
            }
            String outputTable = "\"" + component.getCreatorName() + "\".\"" + getOutputs().get("data_out") + "\"";
            String truncateSQL = "drop table if exists " + outputTable;
            String querySQL = MessageFormatter.arrayFormat("create table {} as select {} from {}", new Object[]{outputTable, StringKit.join(columnList, ","), inputTable}).getMessage();
            String[] sqls = new String[]{truncateSQL, querySQL};
            tableService.batchExecuteSQL(sqls);
            tableService.changeTableOwner(outputTable, component.getCreatorName());
        } catch (Exception ex) {
            throw new AlgorithmException("AlterColumn运行错误，错误信息：" + ex.getMessage());
        }
    }

    static class Column {

        private String origName;

        private String targetName;

        public String getOrigName() {
            return origName;
        }

        public void setOrigName(String origName) {
            this.origName = origName;
        }

        public String getTargetName() {
            return targetName;
        }

        public void setTargetName(String targetName) {
            this.targetName = targetName;
        }

        @Override
        public String toString() {
            return origName + " as " + targetName;
        }
    }

}
