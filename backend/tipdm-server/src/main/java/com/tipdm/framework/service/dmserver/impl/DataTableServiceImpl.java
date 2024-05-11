package com.tipdm.framework.service.dmserver.impl;

import com.alibaba.fastjson.JSONObject;
import com.tipdm.framework.common.utils.*;
import com.tipdm.framework.controller.dmserver.dto.DataColumn;
import com.tipdm.framework.controller.dmserver.dto.UploadInfo;
import com.tipdm.framework.dmserver.exception.DuplicateException;
import com.tipdm.framework.dmserver.exception.ElementNotFoundException;
import com.tipdm.framework.dmserver.exception.IllegalOperationException;
import com.tipdm.framework.dmserver.utils.CommonUtils;
import com.tipdm.framework.dmserver.utils.DBUtils;
import com.tipdm.framework.model.dmserver.*;
import com.tipdm.framework.repository.dmserver.*;
import com.tipdm.framework.service.AbstractBaseServiceImpl;
import com.tipdm.framework.service.dmserver.DataTableService;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.persistence.criteria.*;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * Created by TipDM on 2016/12/15.
 * E-mail:devp@tipdm.com
 */
@SuppressWarnings("all")
@Transactional
@Service("tableService")
public class DataTableServiceImpl extends AbstractBaseServiceImpl<DataTable, Long> implements DataTableService {

    private final static Logger logger = LoggerFactory.getLogger(DataTableServiceImpl.class);

    @Autowired
    private DataTableRepository tableRepository;

    @Autowired
    private DataSchemaRepository dataSchemaRepository;

    @Autowired
    private DBConnectionRepository dbConnectionRepository;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private AudienceRepository audienceRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Value("${db.url}")
    private String url;

    @Value("${db.user}")
    private String username;

    @Value("${db.password}")
    private String password;

    @Override
    public List<DataTable> find(Long... ids) {
        return null;
    }

    @Override
    public Page<DataTable> findTableByCondition(final Map<String, Object> params, Pageable pageable) {

        Specification<DataTable> specification = new Specification<DataTable>() {
            @Override
            public Predicate toPredicate(Root<DataTable> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {

                Predicate predicates = null;
                String creatorName = (String) params.get("creatorName");

                if (StringKit.isNotBlank(creatorName)) {
                    predicates = criteriaBuilder.equal(root.get("creatorName").as(String.class), creatorName);
                    criteriaQuery.where(predicates);
                }

                String showName = (String) params.get("showName");

                if (StringKit.isNotBlank(showName)) {
                    Predicate condition = criteriaBuilder.like(root.get("showName").as(String.class), "%" + showName + "%");
                    if (null == predicates) {
                        predicates = criteriaBuilder.and(condition);
                    } else {
                        predicates = criteriaBuilder.and(predicates, condition);
                    }
                }

                DataTable.Status status = (DataTable.Status) params.get("status");

                if (null != status) {
                    Predicate condition = criteriaBuilder.equal(root.get("status").as(DataTable.Status.class), status);
                    if (null == predicates) {
                        predicates = criteriaBuilder.and(condition);
                    } else {
                        predicates = criteriaBuilder.and(predicates, condition);
                    }
                }


                Date beginTime = null;
                Date endTime = null;
                try {
                    beginTime = (Date) params.get("beginTime");
                    endTime = (Date) params.get("endTime");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (beginTime != null && endTime != null) {
                    Predicate condition = criteriaBuilder.between(root.get("createTime").as(Date.class), beginTime, endTime);

                    if (null == predicates) {
                        predicates = criteriaBuilder.and(condition);
                    } else {
                        predicates = criteriaBuilder.and(predicates, condition);
                    }
                }

                return predicates;
            }
        };

        Page<DataTable> page = tableRepository.findAll(specification, pageable);
        Map<String, Object> map = RedisUtils.getMap(com.tipdm.framework.dmserver.utils.Constants.FILE_UPLOAD_ID);

        page.getContent().parallelStream().forEach(x -> {
            if (x.getTableType() == DataTable.TableType.FLAT_FILE && (x.getStatus() == DataTable.Status.NOTSYNCHRONIZED || x.getStatus() == DataTable.Status.FAILED)) {
                try {
                    Optional optional = map.values().stream().filter(o -> x.getId().equals(((UploadInfo) o).getId()) && ((UploadInfo) o).getCategory() == UploadInfo.Category.FLAT).findFirst();
                    if (optional.isPresent()) {
                        x.setUploadId(((UploadInfo) optional.get()).getUploadId());
                    }
                } catch (Exception ex) {

                }
            }
        });
        return page;
    }


    @Transactional(readOnly = true)
    @Override
    public Page<DataTable> findSharedTables(final Map<String, Object> params, Pageable pageable) {

        return tableRepository.findSharedTables(params, pageable);
    }

    @Override
    public Long createTable(DataTable table, List<DataColumn> columns) throws DuplicateException {

        DataTable duplicateTable = findTableByTableName(table.getTableName());
        if (null != duplicateTable) {
            throw new DuplicateException("数据表[" + table.getShowName() + "]已存在");
        }
        tableRepository.createTable(table.getTableName(), columns);
        tableRepository.changeTableOwner(table.getTableName(), table.getCreatorName());
        tableRepository.save(table);

        //将表名保存到redis, 监听key的过期事件
        RedisUtils.set("@pg:" + table.getTableName(), null, table.getDuration() * (24 * 60 * 60L));
        return table.getId();
    }

    @Override
    public void deleteTable(Long creatorId, Long tableId) throws IllegalAccessException {
        DataTable table = tableRepository.findOne(tableId);
        if (null == table) {
            return;
        }
        if (!creatorId.equals(table.getCreatorId())) {
            logger.info("creatorId:{}", table.getCreatorId());
            logger.info("operator id:{}", creatorId);
            throw new IllegalAccessException("删除数据表失败，操作人员与数据表的创建者不符");
        }

        tableRepository.dropTable(table.getTableName());
        tableRepository.delete(tableId);
    }

    @Override
    public DataTable findTableByTableName(String tableName) {

        DataTable table = tableRepository.findByTableName(tableName);
        return table;
    }

    /**
     * 数据同步
     *
     * @param tableName
     * @throws IllegalOperationException
     */
    @Override
    public void syncTable(String tableName) throws IllegalOperationException {

        DataTable dataTable = tableRepository.findByTableName(tableName);

        if (null == dataTable) {
            throw new IllegalOperationException("要同步的数据源不存在");
        }

        if (null == dataTable.getConn()) {
            throw new IllegalOperationException("无法获取数据表的数据库连接信息");
        }

        if (dataTable.getStatus() == DataTable.Status.SYNCING) {
            throw new IllegalOperationException("数据表正在同步中，请等待同步完成后再执行此操作");
        }

        dataTable.setStatus(DataTable.Status.SYNCING);
        DBUtils.DataBase dataBase = null;
        try {
            dataBase = DBUtils.validURL(dataTable.getConn().getUrl());
        } catch (SQLException e) {
            throw new IllegalOperationException(e.getMessage());
        }
        String reader = dataBase.getReader();

        JSONObject readerPlugin = null;
        try {
            readerPlugin = DBUtils.getReaderTemplate(reader);
        } catch (IOException e) {
            throw new IllegalOperationException("数据读取插件[" + reader + "]不存在，清检查配置");
        }
        JSONObject parameter = readerPlugin.getJSONObject("parameter");
        parameter.put("username", dataTable.getConn().getUserName());
        parameter.put("password", dataTable.getConn().getPassword());
        Map<String, Object> connection = new HashMap<>();
        connection.put("jdbcUrl", new String[]{dataTable.getConn().getUrl()});
        connection.put("querySql", new String[]{dataTable.getConn().getSql()});
        parameter.getJSONArray("connection").set(0, connection);
        readerPlugin.put("parameter", parameter);

        JSONObject job = DBUtils.initJob(url, username, password, dataTable.getTableName(), true);
        job.getJSONObject("job").getJSONArray("content").getJSONObject(0).put("reader", readerPlugin);

        sync(dataTable, job);
    }


    /**
     * 数据同步
     *
     * @param dataTable
     * @param dataFile
     * @param delimiter
     * @param encoding
     * @throws IllegalOperationException
     */
    @Override
    public void syncTable(DataTable dataTable, File dataFile, String delimiter, String encoding) throws IllegalOperationException {

        if (dataTable.getStatus() == DataTable.Status.SYNCING) {
            throw new IllegalOperationException("数据表正在同步中，请等待同步完成后再执行此操作");
        }

        dataTable.setStatus(DataTable.Status.SYNCING);
        JSONObject readerPlugin = null;
        try {
            readerPlugin = DBUtils.getReaderTemplate("txtfilereader");
        } catch (IOException e) {
            throw new IllegalOperationException("数据读取插件[txtfilereader]不存在，清检查配置");
        }
        JSONObject parameter = readerPlugin.getJSONObject("parameter");
        parameter.getJSONArray("column").set(0, "*");
        parameter.getJSONArray("path").set(0, dataFile.getAbsolutePath().replaceAll("\\\\", "\\\\\\\\"));
        parameter.put("encoding", encoding);
        parameter.put("fieldDelimiter", delimiter);
        readerPlugin.put("parameter", parameter);

        JSONObject job = DBUtils.initJob(url, username, password, dataTable.getTableName(), true);
        job.getJSONObject("job").getJSONArray("content").getJSONObject(0).put("reader", readerPlugin);
        sync(dataTable, job);
    }

    /**
     * 数据同步
     *
     * @param tableName
     * @param job
     */
    private void sync(DataTable dataTable, JSONObject job) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String accessToken = request.getHeader("accessToken");
        DataTable table = new DataTable();
        table.setShowName(dataTable.getShowName());
        table.setTableName(dataTable.getTableName());
        table.setId(dataTable.getId());
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean res = DBUtils.dataSync(accessToken, table, job);
                int status = res ? DataTable.Status.FINISH.getValue() : DataTable.Status.FAILED.getValue();
                tableRepository.updateStatus(dataTable.getId(), status);
                if (res) {
                    tableRepository.analyzeDataTable(dataTable.getTableName());
                }
            }
        });
        t.start();
    }


    @Override
    public Page<Map<String, Object>> previewData(Long tableId, Pageable pageable) throws ElementNotFoundException {
        DataTable table = tableRepository.findOne(tableId);
        if (null == table) {
            throw new ElementNotFoundException("数据表不存在");
        }
        return tableRepository.previewData("select * from " + table.getTableName(), pageable);
    }

    @Override
    public List<Map<String, Object>> filterDataTable(Long creatorId, String prefix, List<Integer> excludeType, Integer limit) {

        return tableRepository.findDataTableByPrefix(creatorId, prefix, excludeType, limit);
    }

    @Override
    public List<Map<String, Object>> getTableStructure(String table) throws FileNotFoundException {

        String schema = null;
        String tableName = null;
        if (NumberUtils.isDigits(table)) {
            DataTable dataTable = tableRepository.findOne(Long.parseLong(table));
            if (null != dataTable) {
                schema = dataTable.getCreatorName();
                tableName = dataTable.getShowName();
            }
        } else {
            table = StringKit.replaceAll(table, "\"", "");
            String[] tmp = StringKit.split(table, ".");
            schema = tmp[0];
            tableName = tmp[1];
        }
        if (StringKit.isBlank(schema) || StringKit.isBlank(tableName)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> struct = tableRepository.getTableStructure(schema, tableName);
        Map<String, String> dataTypes = PropertiesUtil.getProperties("/sysconfig/dataType-mapping.properties");
        if (null == dataTypes) {
            throw new FileNotFoundException("数据类型映射配置文件/sysconfig/dataType-mapping.properties不存在，配置方式：double = numeric");
        }

        for (Map<String, Object> x : struct) {
            String key = "dataType";
            String dataType = (String) x.get(key);
            String[] tmp = dataType.split(" ");
            if (!dataTypes.containsKey(tmp[0])) {
                throw new ElementNotFoundException("/sysconfig/dataType-mapping.properties不存在【" + tmp[0] + "】的映射");
            }
            x.put(key, dataTypes.get(tmp[0]));
        }
        return struct;
    }


    @Override
    public void update(DataTable dataTable) {
        tableRepository.updateStatus(dataTable.getId(), dataTable.getStatus().getValue());
    }

    @Override
    public void shareDataTable(Long dataTableId, List<com.tipdm.framework.controller.dmserver.dto.datasource.Audience> audiences) {

        DataTable table = tableRepository.findOne(dataTableId);
        Assert.notNull(table, "数据表不存在，Id:" + dataTableId);
        List<Audience> list = new ArrayList<>();
        for (com.tipdm.framework.controller.dmserver.dto.datasource.Audience item : audiences) {
            Audience audience = new Audience();
            audience.setUserId(item.getUserId());
            audience.setUserName(item.getUserName());
            audience.setSharedObjectId(dataTableId);
            audience.setObjectType(ShareType.DATASOURCE);
            list.add(audience);
            DataSchema dataSchema = dataSchemaRepository.findByName(audience.getUserName());
            if (null == dataSchema) {
                dataSchema = new DataSchema();
                dataSchema.setName(audience.getUserName());
                String password = RandomStringUtils.randomAlphabetic(8);
                dataSchema.setPassword(password);
                dataSchemaRepository.createSchema(audience.getUserName(), password);
                dataSchemaRepository.save(dataSchema);
            }
            // 授予被分享表的查询权限
            tableRepository.grantPrivilege(table.getCreatorName(), table.getTableName(), audience.getUserName());
        }
        audienceRepository.save(list);
    }

    @Override
    public void dropExpiredTable(String... tables) {

        if (ArrayUtils.isNotEmpty(tables)) {
            for (String table : tables) {
                logger.info("drop table:{}", table);
                tableRepository.dropTable(table);
            }
        }
    }

    @Override
    public void batchExecuteSQL(String... sqls) throws SQLException {
        tableRepository.batchExecuteSQL(sqls);
    }

    @Override
    public Page<Map<String, Object>> findDataByOutputId(Long projectId, String outputId, Pageable pageable) throws ElementNotFoundException {

        Project project = projectRepository.findOne(projectId);
        if (null == project) {
            throw new ElementNotFoundException("不存在id为" + projectId + "的工程");
        }
        if (NumberUtils.isDigits(outputId)) {
            return previewData(Long.parseLong(outputId), pageable);
        } else {
            String tableName = CommonUtils.generateTableName(project.getCreatorName(), outputId);
            return tableRepository.previewData("select * from " + tableName, pageable);
        }
    }

    @Override
    public void changeTableOwner(String tableName, String owner) {
        tableRepository.changeTableOwner(tableName, owner);
    }

}
