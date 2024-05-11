
package com.tipdm.framework.controller.dmserver;

import com.tipdm.framework.common.controller.Result;
import com.tipdm.framework.common.controller.base.BaseController;
import com.tipdm.framework.common.token.impl.RedisTokenManager;
import com.tipdm.framework.common.token.model.TokenModel;

import com.tipdm.framework.common.utils.RedisUtils;
import com.tipdm.framework.common.utils.StringKit;
import com.tipdm.framework.controller.dmserver.dto.Table;
import com.tipdm.framework.controller.dmserver.dto.UploadInfo;
import com.tipdm.framework.controller.dmserver.dto.datasource.Connection;
import com.tipdm.framework.controller.dmserver.dto.datasource.Flat;
import com.tipdm.framework.controller.dmserver.dto.datasource.RDBMS;
import com.tipdm.framework.dmserver.exception.IllegalOperationException;
import com.tipdm.framework.dmserver.utils.CommonUtils;
import com.tipdm.framework.dmserver.utils.DBUtils;
import com.tipdm.framework.model.dmserver.*;
import com.tipdm.framework.service.dmserver.DataTableService;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.*;
import java.util.Date;

/**
 * Created by TipDM on 2016/12/20.
 * E-mail:devp@tipdm.com
 */
@SuppressWarnings("all")
@PropertySource(value = "classpath:sysconfig/system.properties")
@RestController
@RequestMapping("/api/datasource")
//@Api(value = "/api/datasource", tags = "数据源管理", description = "维护用户在平台创建的各类数据源", position = 2)
public class DataSourceController extends BaseController {

    private final static Logger logger = LoggerFactory.getLogger(DataSourceController.class);

    @Autowired
    private DataTableService tableService;

    @Autowired
    private RedisTokenManager tokenManager;

    @Value("${dataX.home}")
    private String dataXHome;

    @RequiresPermissions("datasource:search")
    @RequestMapping(value = "/search", method = RequestMethod.GET)
//    @ApiOperation(value = "搜索用户的数据表")
    public Result getTables(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                            /*@ApiParam(value = "数据表名称") */ @RequestParam(required = false) String showName,
                            /*@ApiParam(value = "状态") @RequestParam(required = false)*/ DataTable.Status status,
                            /*@ApiParam(value = "开始时间, 格式: yyyy-MM-dd HH:mm:ss", defaultValue = "2016-01-01 00:00:00")*/ @RequestParam(required = false) Date beginTime,
                            /*@ApiParam(value = "结束时间, 格式: yyyy-MM-dd HH:mm:ss", defaultValue = "2016-01-02 00:00:00")*/ @RequestParam(required = false) Date endTime,
                            /*@ApiParam(value = "页码", required = true)*/ @RequestParam(value = "pageNumber", defaultValue = "1") int pageNumber,
                            /*@ApiParam(value = "页大小", required = true)*/ @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        Result result = new Result();

        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        Map<String, Object> searchParams = new HashMap<>();
        searchParams.put("creatorName", tokenModel.getUsername());
        searchParams.put("showName", showName);
        searchParams.put("status", status);
        searchParams.put("beginTime", beginTime);
        searchParams.put("endTime", endTime);

        Page<DataTable> tables = tableService.findTableByCondition(searchParams, buildPageRequest(pageNumber, pageSize));

        result.setData(tables);
        result.setStatus(Result.Status.SUCCESS);
        return result;
    }

    @RequiresPermissions("datasource:shared")
    @RequestMapping(value = "/shared", method = RequestMethod.GET)
//    @ApiOperation(value = "搜索用户的分享列表")
    public Result getSharedTables(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                                  /*@ApiParam(value = "数据表名称") @RequestParam(required = false)*/ String showName,
                                  /*@ApiParam(value = "状态") @RequestParam(required = false)*/ DataTable.Status status,
                                  /*@ApiParam(value = "开始时间, 格式: yyyy-MM-dd HH:mm:ss", defaultValue = "2016-01-01 00:00:00")*/ @RequestParam(required = false) Date beginTime,
                                  /*@ApiParam(value = "结束时间, 格式: yyyy-MM-dd HH:mm:ss", defaultValue = "2016-01-02 00:00:00")*/ @RequestParam(required = false) Date endTime,
                                  /*@ApiParam(value = "页码", required = true)*/ @RequestParam(value = "pageNumber", defaultValue = "1") int pageNumber,
                            /*@ApiParam(value = "页大小", required = true)*/ @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        Result result = new Result();

        TokenModel tokenModel = tokenManager.getPermissions(accessToken);

        Map<String, Object> searchParams = new HashMap<>();
        searchParams.put("creatorId", tokenModel.getUserId());
        searchParams.put("showName", showName);
        searchParams.put("status", status);
        searchParams.put("beginTime", beginTime);
        searchParams.put("endTime", endTime);

        Page<DataTable> tables = tableService.findSharedTables(searchParams, buildPageRequest(pageNumber, pageSize));

        result.setData(tables);
        result.setStatus(Result.Status.SUCCESS);
        return result;
    }

    @RequiresPermissions("datasource:filter")
    @RequestMapping(value = "/filter", method = RequestMethod.GET)
//    @ApiOperation(value = "过滤表", notes = "根据用户输入过滤表，可通过设置limit来调整返回的数据集的大小")
    public Result filterTable(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                              /*@ApiParam(value = "前缀", required = true)*/ @RequestParam String prefix,
                              /*@ApiParam(value = "要排除的数据表类型", required = true, defaultValue = "NONE")*/ @RequestParam Table.ExcludeType[] exclude,
                              /*@ApiParam(value = "返回的数据条数", required = true, allowableValues = "range[1,100]")*/ @RequestParam(value = "limit", defaultValue = "20") int limit) {
        Result result = new Result();

        if (limit > 100) {
            limit = 100;
        }

        if (prefix == null) {
            throw new IllegalArgumentException("必须输入数据表名");
        }
        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        List<Integer> values = new ArrayList<>();
        for (Table.ExcludeType excludeType : exclude) {
            values.add(excludeType.getValue());
        }
        List<Map<String, Object>> data = tableService.filterDataTable(tokenModel.getUserId(), prefix + "%", values, limit);
        result.setMessage("数据加载成功");
        result.setData(data);

        return result;
    }

    @RequiresPermissions("datasource:syncTable")
    @RequestMapping(value = "/syncTable", method = RequestMethod.GET)
//    @ApiOperation(value = "同步表数据", notes = "立即执行数据源同步")
    public Result syncTable(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                            /*@ApiParam(value = "要同步的目标表", required = true)*/ @RequestParam String tableName) throws Exception {
        Result result = new Result();

        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        tableName = CommonUtils.generateTableName(tokenModel.getUsername(), tableName);
        try {
            tableService.syncTable(tableName);
            result.setMessage("数据同步任务提交成功");
        } catch (IllegalOperationException ex) {
            result.setStatus(Result.Status.FAIL);
            result.setMessage(ex.getMessage());
        }
        return result;
    }


    @RequiresPermissions("datasource:preview")
    @RequestMapping(value = "/{tableId}/preview", method = RequestMethod.GET)
//    @ApiOperation(value = "预览表数据", notes = "预览数据源前100条记录")
    public Result preview(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                          /*@ApiParam(value = "数据表id", required = true)*/ @PathVariable(name = "tableId") Long tableId,
                          /*@ApiParam(value = "页码", required = true)*/ @RequestParam(value = "pageNumber", defaultValue = "1") int pageNumber,
                          /*@ApiParam(value = "页大小", required = true)*/ @RequestParam(value = "pageSize", defaultValue = "100") int pageSize) {
        Result result = new Result();
        Page<Map<String, Object>> data = tableService.previewData(tableId, buildPageRequest(pageNumber, pageSize));
        result.setData(data);
        result.setMessage("数据加载成功");
        return result;
    }

    //    @RequiresPermissions("datasource:connectionInfo")
    @RequestMapping(value = "/{tableId}/connection/info", method = RequestMethod.GET)
//    @ApiOperation(value = "查看连接信息", notes = "查看数据源的连接信息(仅支持RDBMS)")
    public Result connectionInfo(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                                 /*@ApiParam(value = "数据表id", required = true)*/ @PathVariable(name = "tableId") Long tableId) {
        Result result = new Result();
        DataTable table = tableService.findOne(tableId);
        result.setData(table.getConn());
        result.setMessage("数据加载成功");
        return result;
    }


    @RequiresPermissions("datasource:test")
    @RequestMapping(value = "/connection/test", method = RequestMethod.POST)
//    @ApiOperation(value = "测试数据库连接", notes = "连接成功后返回元数据信息和前100条数据")
    public Result testDBConn(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                             /*@ApiParam(value = "数据库连接信息", required = true)*/ @RequestBody Connection conn) throws Exception {
        Result result = new Result();
        Map data = DBUtils.testConnection(conn);
        result.setData(data);
        return result;
    }

    /**
     *
     * @param accessToken
     * @param tableName
     * @return
     */
    @RequiresPermissions("datasource:exists")
    @RequestMapping(value = "/{tableName}/exists", method = RequestMethod.GET)
//    @ApiOperation(value = "检查数据表是否存在")
    public Result exists(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                         /*@ApiParam(value = "数据表名称", required = true)*/ @PathVariable(name = "tableName") String tableName) {

        Result result = new Result();
        String lowercase = tableName.toLowerCase();
        if (!lowercase.equals(tableName)) {
            result.setStatus(Result.Status.FAIL);
            result.setMessage("数据表名不能包含大写字母");
            return result;
        }
        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        tableName = CommonUtils.generateTableName(tokenModel.getUsername(), tableName);

        DataTable userTable = tableService.findTableByTableName(tableName);
        result.setData(userTable != null);
        return result;
    }

    @RequiresPermissions("datasource:delete")
    @RequestMapping(value = "/{tableId}", method = RequestMethod.DELETE)
//    @ApiOperation(value = "删除数据表")
    public Result delete(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                         /*@ApiParam(value = "数据表ID", required = true)*/ @PathVariable(name = "tableId") Long tableId) throws IllegalAccessException {

        Result result = new Result();
        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        tableService.deleteTable(tokenModel.getUserId(), tableId);
        result.setMessage("删除数据表成功");
        return result;
    }


    @RequiresPermissions("datasource:flat")
    @RequestMapping(value = "/flat", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
//    @ApiOperation(value = "创建数据表(Flat)", notes = "数据表创建成功后返回全局唯一的上传凭证：uploadId，用于后续的文件上传，uploadId在上传成功后失效")
//    @ApiResponses({
//            @ApiResponse(code = 201, message = "数据表创建成功"),
//            @ApiResponse(code = 400, message = "提交的数据错误"),
//            @ApiResponse(code = 401, message = "无效的token或token已过期"),
//            @ApiResponse(code = 403, message = "您没有权限执行此操作")
//    })
    public Result createUserTableFromFlatFile(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                                              /*@ApiParam(value = "用户数据表信息", required = false, name = "info")*/ @RequestBody Flat flat,
                                              HttpServletResponse response) {
        Result result = new Result();
        String lowercase = flat.getTableName().toLowerCase();
        if (lowercase.equals(flat.getTableName())) {
            TokenModel tokenModel = tokenManager.getPermissions(accessToken);
            DataTable userTable = new DataTable(tokenModel.getUsername(), tokenModel.getUserId(), flat.getTableName(), DataTable.TableType.FLAT_FILE);
            userTable.setPreviewMode(flat.getPreviewMode());
            userTable.setDuration(flat.getDuration());
            Long tableId = tableService.createTable(userTable, flat.getColumns());
            String uploadId = StringKit.getBase64FromUUID();
            RedisUtils.putToMap(com.tipdm.framework.dmserver.utils.Constants.FILE_UPLOAD_ID, uploadId, new UploadInfo(tableId, uploadId, UploadInfo.Category.FLAT));
            result.setData(uploadId);
            response.setStatus(HttpStatus.CREATED.value());
            return result;
        } else {
            result.setStatus(Result.Status.FAIL);
            result.setMessage("数据表名不能包含大写字母");
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return result;
        }
    }


    @RequiresPermissions("datasource:rdbms")
    @RequestMapping(value = "/rdbms", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
//    @ApiOperation(value = "创建数据表(RDBMS)")
//    @ApiResponses({
//            @ApiResponse(code = 201, message = "数据表创建成功"),
//            @ApiResponse(code = 400, message = "提交的数据错误"),
//            @ApiResponse(code = 401, message = "无效的token或token已过期"),
//            @ApiResponse(code = 403, message = "您没有权限执行此操作")
//    })
    public Result createUserTableFromRDBMS(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                                           /*@ApiParam(value = "用户数据表信息", required = true, name = "info")*/ @RequestBody RDBMS rdbms,
                                           HttpServletResponse response) throws SQLException, ClassNotFoundException, IllegalAccessException {
        Result result = new Result();
        String lowercase = rdbms.getTableName().toLowerCase();
        if (!lowercase.equals(rdbms.getTableName())) {
            result.setStatus(Result.Status.FAIL);
            result.setMessage("数据表名不能包含大写字母");
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return result;
        }
        boolean exists = RedisUtils.exists(rdbms.getConnection().toString());
        if (!exists) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            result.setMessage("连接必须先通过测试");
            result.setStatus(Result.Status.FAIL);
            return result;
        }

        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        String tableName = rdbms.getTableName();
        DataTable userTable = new DataTable(tokenModel.getUsername(), tokenModel.getUserId(), tableName, DataTable.TableType.RDBMS);
        userTable.setDuration(rdbms.getDuration());
        userTable.setPreviewMode(rdbms.getPreviewMode());
        DBConnection conn = new DBConnection();
        BeanUtils.copyProperties(rdbms.getConnection(), conn);

        userTable.setConn(conn);
        Long tableId = tableService.createTable(userTable, rdbms.getColumns());
        result.setData(tableId);
        response.setStatus(HttpStatus.CREATED.value());
        return result;
    }

    @RequiresPermissions("datasource:structure")
    @RequestMapping(value = "/table/{table}/structure", method = RequestMethod.GET)
//    @ApiOperation(value = "获取表的结构信息", notes = "返回表的数据结构，vals为字段的值的分布概况")
    public Result getTableStructure(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                                    /*@ApiParam(value = "数据表Id/数据表名称", required = true)*/ @PathVariable(name = "table") String table) throws FileNotFoundException {

        Result result = new Result();

        if (!NumberUtils.isDigits(table)) {
            TokenModel tokenModel = tokenManager.getPermissions(accessToken);
            table = CommonUtils.generateTableName(tokenModel.getUsername(), table);
        }

        List<Map<String, Object>> structures = tableService.getTableStructure(table);
        result.setData(structures);
        result.setMessage("数据加载成功");
        return result;
    }

    @RequiresPermissions("datasource:share")
    @SuppressWarnings("all")
    @RequestMapping(value = "/{dataSourceId}/share", method = RequestMethod.POST)
//    @ApiOperation(value = "分享数据源", position = 6)
    public Result shareDataTable(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                             /*@ApiParam(value = "数据源ID", required = true)*/ @PathVariable(name = "dataSourceId") Long dataSourceId,
                             /*@ApiParam(value = "分享的受众", required = true)*/ @RequestBody List<com.tipdm.framework.controller.dmserver.dto.datasource.Audience> audiences) {
        Result result = new Result();

        if (audiences != null && audiences.size() > 0) {
            tableService.shareDataTable(dataSourceId, audiences);
            result.setMessage("数据源分享成功");
            return result;
        }
        result.setMessage("分享的用户不能为空");
        result.setStatus(Result.Status.FAIL);
        return result;
    }

}
