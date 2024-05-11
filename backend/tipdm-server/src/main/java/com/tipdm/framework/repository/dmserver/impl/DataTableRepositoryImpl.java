package com.tipdm.framework.repository.dmserver.impl;

import com.alibaba.druid.sql.PagerUtils;
import com.alibaba.druid.util.JdbcConstants;
import com.tipdm.framework.common.utils.StringKit;
import com.tipdm.framework.controller.dmserver.dto.DataColumn;
import com.tipdm.framework.persist.transform.AliasToEntityLinkedMapResultTransformer;
import com.tipdm.framework.dmserver.utils.PagerKit;
import com.tipdm.framework.model.dmserver.ShareType;
import com.tipdm.framework.model.dmserver.DataTable;
import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.SQLQuery;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.transform.Transformers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by TipDM on 2016/12/15.
 * E-mail:devp@tipdm.com
 */
public class DataTableRepositoryImpl {

    private final static Logger logger = LoggerFactory.getLogger(DataTableRepositoryImpl.class);

    @PersistenceContext
    private EntityManager em;

    public void setEm(EntityManager em) {
        this.em = em;
    }


    public void createTable(String tableName, List<DataColumn> columns){

        StringBuilder sb = new StringBuilder("create table " + tableName + "(");

        List<String> colStr = new ArrayList<>();
        List<String> commentStr = new ArrayList<>();

        for(DataColumn column : columns){
            colStr.add(column.toString());
            commentStr.add(column.getComment(tableName));
        }

        sb.append(StringKit.join(colStr, ",")).append(");");
        sb.append(StringKit.join(commentStr, ""));
        Query query = em.createNativeQuery(sb.toString());

        query.executeUpdate();
    }

    public void changeTableOwner(String tableName, String owner){
        Query query = em.createNativeQuery("ALTER TABLE "+ tableName+ "OWNER TO " + owner);
        query.executeUpdate();
    }

    public void truncate(String tableName) {

        String sql = "DELETE  FROM  "+tableName;

        Query query = em.createNativeQuery(sql);
        try {
            query.executeUpdate();
        }catch (Exception e){
            logger.error("truncate error", e);
        }
    }

    /**
     * 删除数据源对应的表
     * @param tableName
     */
    public void dropTable(String tableName){

        try {
            String sql = "DROP table IF EXISTS " + tableName;
            Query query = em.createNativeQuery(sql);
            query.executeUpdate();
        } catch (Exception ex){
            logger.error("数据表删除失败，错误信息：{}", ex.getMessage());
        }
    }


    public Page<Map<String, Object>> previewData(String sql, Pageable pageable) {

        String countSQL = PagerUtils.count(sql, JdbcConstants.POSTGRESQL);
        Query countQuery = em.createNativeQuery(countSQL);
        int count = ((BigInteger)countQuery.getSingleResult()).intValue();
        if(count == 0){
            return new PageImpl<Map<String, Object>>(new ArrayList<>(), pageable, count);
        }

        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        String pageSQL = PagerKit.limit(sql, JdbcConstants.POSTGRESQL, pageNumber * pageSize, pageSize);
        Query pageQuery = em.createNativeQuery(pageSQL);
        return new PageImpl<Map<String, Object>>(pageQuery.unwrap(SQLQuery.class).setResultTransformer(AliasToEntityLinkedMapResultTransformer.INSTANCE).list(), pageable, count);
    }


    public List<Map<String, Object>> findDataTableByPrefix(Long creatorId, String prefix, List<Integer> excludeType, Integer limit){

        String sql = " select * from ( " +
                " select show_name as NAME , id from dm_data_table where creator_id = :creatorId and table_type NOT IN :excludeType and show_name like :prefix " +
                " union " +
                " select t.show_name as NAME , t.id from dm_audience s inner join dm_data_table t " +
                " on s.shared_object_id = t.id and s.object_type = :objectType " +
                " where s.user_id = :creatorId " +
                " and t.table_type NOT IN :excludeType " +
                " and t.show_name like :prefix " +
                " ) tmp limit :limit";

        Query query = em.createNativeQuery(sql);
        query.setParameter("creatorId", creatorId);
        query.setParameter("prefix", prefix);
        query.setParameter("limit", limit);
        query.setParameter("excludeType", excludeType);
        query.setParameter("objectType", ShareType.DATASOURCE.getValue());

        query.unwrap(SQLQuery.class).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);

        return query.getResultList();
    }

    public List<Map<String, Object>> getTableStructure(String schemaName, String tableName){

        String sql = "select info.column_name as name, info.data_type as \"dataType\", array_to_string(ps.most_common_vals, ',', '') as values " +
                " from INFORMATION_SCHEMA.COLUMNS info left outer join pg_stats ps " +
                " on  ps.tablename = info.table_name and ps.attname = info.column_name " +
                " and ps.schemaname = info.table_schema " +
                " where info.table_schema = :schemaName and info.table_name = :tableName" +
                " order by info.ordinal_position";

        Query query = em.createNativeQuery(sql);
        query.setParameter("schemaName", schemaName);
        query.setParameter("tableName", tableName);
        query.unwrap(SQLQuery.class).setResultTransformer(AliasToEntityLinkedMapResultTransformer.INSTANCE);
        return query.getResultList();
    }

    @SuppressWarnings("all")
    public Page<DataTable> findSharedTables(final Map<String, Object> params, Pageable pageable){

        StringBuilder sb = new StringBuilder("select distinct u " +
                "from DataTable u inner join Audience s on u.id = s.sharedObjectId " +
                "where s.objectType = :objectType and s.userId = :creatorId ");

        Long creatorId = (Long)params.get("creatorId");
        String showName = (String)params.get("showName");

        if(StringKit.isNotBlank(showName)){
            sb.append(" and u.showName like :showName");
        }

        DataTable.Status status = (DataTable.Status)params.get("status");

        if(null != status){
            sb.append(" and u.status = :status");
        }

        Date beginTime = null;
        Date endTime = null;
        try {
            beginTime = (Date)params.get("beginTime");
            endTime = (Date)params.get("endTime");
        } catch (Exception e) {

        }

        if(beginTime != null && endTime != null){
            sb.append("  and ( u.createTime between :beginTime and :endTime )");
        }

        String sql = sb.toString();
        Query query = em.createQuery(sql + " order by u.id desc ");

        String countSQL = StringKit.replace(sql, "distinct u", "count(distinct u)");
        Query countQuery = em.createQuery(countSQL);

        query.setParameter("objectType", ShareType.DATASOURCE);
        query.setParameter("creatorId", creatorId);

        countQuery.setParameter("objectType", ShareType.DATASOURCE);
        countQuery.setParameter("creatorId", creatorId);

        if(StringKit.isNotBlank(showName)) {
            query.setParameter("showName", "%"+showName+"%");
            countQuery.setParameter("showName", "%"+showName+"%");
        }

        if(null != status) {
            query.setParameter("status", status);
            countQuery.setParameter("status", status);
        }
        if(beginTime != null && endTime != null) {
            query.setParameter("beginTime", beginTime);
            query.setParameter("endTime", endTime);

            countQuery.setParameter("beginTime", beginTime);
            countQuery.setParameter("endTime", endTime);
        }

        Long count = (Long)countQuery.getSingleResult();
        query.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());
        query.setMaxResults(pageable.getPageSize());

        Page<DataTable> page = new PageImpl<DataTable>(query.getResultList(), pageable, count);
        return page;
    }

    public void batchExecuteSQL(String... sqls) throws SQLException {
        if(ArrayUtils.isEmpty(sqls)){
            return;
        }
        SessionImplementor session = em.unwrap(SessionImplementor.class);
        Connection connection = session.connection();
        try {
            connection.setAutoCommit(false);
            Statement statement = connection.createStatement();
            for(String sql : sqls) {
                statement.addBatch(sql);
            };
            statement.executeBatch();
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {

            }
            throw e;
        }
    }

    public void grantPrivilege(String schemaname, String tableName, String username) {
        Query query = em.createNativeQuery("GRANT USAGE on SCHEMA " + schemaname + " to " + username +";GRANT SELECT ON TABLE " + tableName + " TO " + username);
        query.executeUpdate();
    }

    public void analyzeDataTable(String tableName) {
        Query query = em.createNativeQuery("analyze verbose " + tableName);
        query.executeUpdate();
    }
}
