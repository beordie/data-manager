package com.tipdm.framework.controller.dmserver.dto;

import com.tipdm.framework.model.dmserver.DataTable;
//import io.swagger.annotations.ApiModel;
//import io.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * Created by TipDM on 2016/12/26.
 * E-mail:devp@tipdm.com
 */
//@ApiModel
public class Table {

//    @ApiModelProperty(value = "数据表名称", position = 0)
    private String tableName;

//    @ApiModelProperty(value = "数据库连接信息ID，可为空", position = 1)
    private long connId;

//    @ApiModelProperty(value = "表的数据来源类别", position = 1)
    private DataTable.TableType tableType = DataTable.TableType.FLAT_FILE;

//    @ApiModelProperty(value = "表字段信息集合", position = 2)
    private List<DataColumn> columns;

//    @ApiModelProperty(value = "保存时长,单位：天，最长可保存180天", allowableValues = "range[1,180]", position = 3)
    private Integer duration=180;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public long getConnId() {
        return connId;
    }

    public void setConnId(long connId) {
        this.connId = connId;
    }

    public List<DataColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<DataColumn> columns) {
        this.columns = columns;
    }

    public DataTable.TableType getTableType() {
        return tableType;
    }

    public void setTableType(DataTable.TableType tableType) {
        this.tableType = tableType;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public enum ExcludeType{
        NONE(-1),
        FLAT_FILE(0),
        RDBMS(1),
        HIVE(2),
        HDFS(3);

        private ExcludeType(Integer value){
            this.value = value;
        }

        private Integer value;

        public Integer getValue(){
            return value;
        }
    }
}
