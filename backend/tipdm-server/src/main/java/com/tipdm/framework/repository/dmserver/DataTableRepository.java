package com.tipdm.framework.repository.dmserver;

import com.tipdm.framework.controller.dmserver.dto.DataColumn;
import com.tipdm.framework.model.dmserver.DataTable;
import com.tipdm.framework.persist.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Created by TipDM on 2016/12/15.
 * E-mail:devp@tipdm.com
 */
@Transactional
public interface DataTableRepository extends BaseRepository<DataTable, Long> {


    public Page<DataTable> findSharedTables(final Map<String, Object> params, Pageable pageable);

    /**
     * 根据表名查找数据表
     * @param tableName
     * @return
     */
    public DataTable findByTableName(String tableName);

    /**
     * 建表
     * @param tableName
     * @param columns
     */
    public void createTable(String tableName, List<DataColumn> columns);

    /**
     * 修改表的所有者
     * @param tableName
     * @param owner
     */
    public void changeTableOwner(String tableName, String owner);

    /**
     * 删除表
     * @param tableName
     */
    public void dropTable(String tableName);

    public Page<Map<String, Object>> previewData(String sql, Pageable pageable);
    /**
     * 根据数据表名前缀过滤
     * @param prefix
     * @param limit
     * @return
     */
    public List<Map<String, Object>> findDataTableByPrefix(Long creatorId, String prefix, List<Integer> excludeType, Integer limit);

    /**
     * 获取表的数据结构
     * @param schemaName
     * @param tableName
     * @return
     */
    public List<Map<String, Object>> getTableStructure(String schemaName, String tableName);

    /**
     * 更新表同步状态
     * @param status
     * @param id
     */
    @Transactional
    @Modifying
    @Query(value = "UPDATE dm_data_table SET status =?2 WHERE id=?1",nativeQuery = true)
    public void updateStatus(Long id, Integer status);

    public void batchExecuteSQL(String... sqls) throws SQLException;

    public void grantPrivilege(String schemaname, String tableName, String user);

    public void analyzeDataTable(String tableName);

}
