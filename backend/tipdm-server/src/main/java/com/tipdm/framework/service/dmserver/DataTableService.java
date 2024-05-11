package com.tipdm.framework.service.dmserver;

import com.tipdm.framework.controller.dmserver.dto.DataColumn;
import com.tipdm.framework.dmserver.exception.DuplicateException;
import com.tipdm.framework.dmserver.exception.ElementNotFoundException;
import com.tipdm.framework.dmserver.exception.IllegalOperationException;
import com.tipdm.framework.model.dmserver.Audience;
import com.tipdm.framework.model.dmserver.DataTable;
import com.tipdm.framework.service.BaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * Created by TipDM on 2016/12/15.
 * E-mail:devp@tipdm.com
 */
public interface DataTableService extends BaseService<DataTable, Long> {

    /**
     * 条件查找
     * @param params
     * @param pageable
     * @return
     */
    public Page<DataTable> findTableByCondition(Map<String, Object> params, Pageable pageable);

    public Page<DataTable> findSharedTables(Map<String, Object> params, Pageable pageable);

    /**
     * 创建表
     * @param table
     * @param columns
     * @return
     */
    public Long createTable(DataTable table, List<DataColumn> columns) throws DuplicateException;

    public void deleteTable(Long creatorId, Long tableId) throws IllegalAccessException;

    public DataTable findTableByTableName(String tableName);

    public void syncTable(String tableName) throws IllegalOperationException;

    public void syncTable(DataTable dataTable, File dataFile, String delimiter, String encoding) throws IllegalOperationException;

    public Page<Map<String, Object>> previewData(Long tableId, Pageable pageable);

    public List<Map<String, Object>> filterDataTable(Long creatorId, String prefix, List<Integer> excludeType, Integer limit);

    public List<Map<String, Object>> getTableStructure(String table) throws FileNotFoundException;

    public void update(DataTable DataTable);

    public void shareDataTable(Long dataTableId, List<com.tipdm.framework.controller.dmserver.dto.datasource.Audience> audiences);

    /**
     * 删除过期的数据源
     */
    public void dropExpiredTable(String... tables);

    public void batchExecuteSQL(String... sqls) throws SQLException;

    /**
     * 查看流程中组件的输出数据
     * NumberUtils.isDigits(outputId) == true 表明是输入节点
     * @param projectId
     * @param outputId
     * @return
     */
    public Page<Map<String, Object>> findDataByOutputId(Long projectId, String outputId, Pageable pageable) throws ElementNotFoundException;


    public void changeTableOwner(String tableName, String owner);
}
