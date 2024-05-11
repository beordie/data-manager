package com.tipdm.framework.dmserver.core.algo.unparallel.preprocessing;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.tipdm.framework.common.utils.SpringUtils;
import com.tipdm.framework.common.utils.StringKit;
import com.tipdm.framework.dmserver.exception.AlgorithmException;
import com.tipdm.framework.dmserver.core.algo.unparallel.AbstractAlgorithm;
import com.tipdm.framework.service.dmserver.DataTableService;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.helpers.MessageFormatter;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by TipDM on 2017/3/2.
 * E-mail:devp@tipdm.com
 * 记录选择
 */
public class Selection extends AbstractAlgorithm {

    private DataTableService tableService = SpringUtils.getBean("tableService", DataTableService.class);

    @Override
    protected void execute() throws AlgorithmException {

        //cyl < 6 & vs == 1 & carb == 1
        Map<String, String> params = component.getParameters();
        List<Map<String, Object>> terms = JSON.parseObject(params.get("term"), new TypeReference<List<Map<String, Object>>>() {
        });

        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> map : terms) {
            String concat = (String) map.get("concat");
            sb.append(concat).append(" ");
            sb.append(map.get("field")).append(" ");
            String condition = operation.get(map.get("condition"));
            Object value = map.get("value");

            if ("like".equals(condition) || "not like".equals(condition)) {
                value = value + "%";
            }
            if (!NumberUtils.isNumber((String) value)) {
                value = "'" + value + "'";
            }
            sb.append(condition).append(" ");
            sb.append(value).append(" ");
        }
        String term = sb.toString();

        String inputTable = getInputs().get("input");
        if (!StringKit.contains(inputTable, "\"" + component.getCreatorName() + "\".")) {
            inputTable = "\"" + component.getCreatorName() + "\"." + inputTable;
        }
        String outputTable = "\"" + component.getCreatorName() + "\".\"" + getOutputs().get("output") + "\"";
        String truncateSQL = "drop table if exists " + outputTable;
        String querySQL = MessageFormatter.arrayFormat("create table {} as select {} from {} where 1=1 {}", new Object[]{outputTable, getParams().get("columns"), inputTable, term}).getMessage();
        String[] sqls = new String[]{truncateSQL, querySQL};
        try {
            tableService.batchExecuteSQL(sqls);
            tableService.changeTableOwner(outputTable, component.getCreatorName());
        } catch (SQLException e) {
//            logger.error("记录选择组件运行错误，错误信息：{}", e.getMessage());
            throw new AlgorithmException("记录选择组件运行错误，错误信息：" + e.getMessage());
        }
    }

    private static Map<String, String> operation = new HashMap<>();

    static {
        operation.put("gt", ">");
        operation.put("geq", ">=");
        operation.put("equal", "=");
        operation.put("lt", "<");
        operation.put("leq", "<=");
        operation.put("notEqual", "!=");
        operation.put("contains", "~");
        operation.put("notContains", "!~");
        operation.put("like", "~");
        operation.put("notLike", "!~");
        operation.put("rlike", "like");
        operation.put("notRlike", "not like");
    }
}
