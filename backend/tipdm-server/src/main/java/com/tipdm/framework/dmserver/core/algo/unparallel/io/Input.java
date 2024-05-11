package com.tipdm.framework.dmserver.core.algo.unparallel.io;

import com.tipdm.framework.common.utils.StringKit;
import com.tipdm.framework.dmserver.core.algo.unparallel.AbstractAlgorithm;
import com.tipdm.framework.dmserver.exception.AlgorithmException;
import com.tipdm.framework.model.dmserver.DataTable;
import com.tipdm.framework.model.dmserver.DataTable;

/**
 * Created by TipDM on 2017/2/13.
 * E-mail:devp@tipdm.com
 */
public class Input extends AbstractAlgorithm {


    @Override
    protected void execute() throws AlgorithmException {
        String table = this.getParams().get("table");
        if(StringKit.isBlank(table)){
            throw new AlgorithmException("输入源组件的table参数不能为空");
        }
        Long tableId = Long.parseLong(table);

        DataTable userTable = tableService.findOne(tableId);
        if(null == userTable){
            throw new AlgorithmException("数据源不存在或已被删除！");
        }
    }
}
