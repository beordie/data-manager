package com.tipdm.framework.model.dmserver;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tipdm.framework.common.utils.StringKit;
import com.tipdm.framework.dmserver.utils.CommonUtils;
import com.tipdm.framework.model.IdEntity;
import org.springframework.util.Assert;

import javax.persistence.*;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by TipDM on 2016/12/9.
 * E-mail:devp@tipdm.com
 */
@Entity
@Table(name = "dm_data_table")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties({"tableName", "md5", "expireTime", "creatorId", "conn"})
public class DataTable extends IdEntity<Long> {

    private static final long serialVersionUID = 781335328334067068L;

    @Column(name = "show_name", length = 25, nullable = false)
    private String showName;

    @Column(name = "table_name", length = 64, nullable = false)
    private String tableName;//tableName = username+"."+ showName

    @Column(name = "status")
    @Enumerated()
    private Status status = Status.NOTSYNCHRONIZED;

    @Column(name = "table_type", nullable = false)
    @Enumerated()
    private TableType tableType = TableType.FLAT_FILE;// 0 : 表的数据来源于文件； 1 ：表数据来自于RDBMS ;  2: 来源于Hive，并存入Hive ;
    // 3： 来源于HDFS，并存入Hive；

    @Column
    private String md5;// MD5值 该字段作用于表的数据来源是文件时

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expire_time")
    private Date expireTime = Calendar.getInstance().getTime();

    @OneToOne(targetEntity = DBConnection.class, cascade = CascadeType.ALL)
    @JoinColumn(name = "conn_id")
    private DBConnection conn;

    @Column
    private Integer duration = 180;

    @Column(name = "preview_mode")
    private PreViewMode previewMode = PreViewMode.ONLY100;

    @Transient
    private Boolean supportDataSync = Boolean.FALSE;//是否支持数据同步，根据tableType计算得出

    public DataTable() {

    }

    public DataTable(String creatorName, Long creatorId, String showName, TableType tableType) {
        super.setCreatorName(creatorName);
        super.setCreatorId(creatorId);
        this.showName = showName;
        this.tableName = CommonUtils.generateTableName(creatorName, showName);
        this.tableType = tableType;
        Assert.isTrue((StringKit.length(this.showName) <= 25), "数据表名的长度不能大于25个字符");
    }

    public DataTable(String creatorName, Long creatorId, String showName, TableType tableType, String md5) {
        this(creatorName, creatorId, showName, tableType);
        this.md5 = md5;
    }

    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String uploadId;

    public String getShowName() {
        return showName;
    }

    public void setShowName(String showName) {
        Assert.isTrue((StringKit.length(showName) <= 25), "数据表名的长度不能大于25个字符");
        this.showName = showName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Status getStatus() {
        return status;
    }

    public TableType getTableType() {
        return tableType;
    }

    /**
     * @param tableType : { 0->file2postgretable  ; 1->sqltable2postgretable  ;
     *                  2-> hive2hivetalbe ; 3-> hdfs2hivetable }
     */
    public void setTableType(TableType tableType) {
        this.tableType = tableType;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Date getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }

    public DBConnection getConn() {
        return conn;
    }

    public void setConn(DBConnection conn) {
        this.conn = conn;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public enum Status {// 同步失败(2), 未同步(3)
        FINISH(0), SYNCING(1), FAILED(2), NOTSYNCHRONIZED(3);

        private Status(Integer value) {
            this.value = value;
        }

        private Integer value;

        public Integer getValue() {
            return value;
        }

        public static Status valueOf(Integer value) {

            switch (value) {
                case 0:
                    return Status.FINISH;
                case 1:
                    return Status.SYNCING;
                default:
                    return null;
            }
        }
    }

    public enum TableType {
        FLAT_FILE(0),
        RDBMS(1),
        /*HIVE(2),
        HDFS(3),
        HBASE(4),
        ES(5),
        UNSTRUCTURED(6)*/;

        private TableType(Integer value) {
            this.value = value;
        }

        private Integer value;

        public Integer getValue() {
            return value;
        }
    }

    public Boolean getSupportDataSync() {
        if (this.tableType == TableType.FLAT_FILE /*|| this.tableType == TableType.HDFS || this.tableType == TableType.UNSTRUCTURED*/) {
            return supportDataSync;
        }
        return true;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public PreViewMode getPreviewMode() {
        if (previewMode == null) {
            this.previewMode = PreViewMode.ONLY100;
        }
        return previewMode;
    }

    public void setPreviewMode(PreViewMode previewMode) {
        this.previewMode = previewMode;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
