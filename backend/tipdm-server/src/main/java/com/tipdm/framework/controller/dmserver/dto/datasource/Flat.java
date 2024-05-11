package com.tipdm.framework.controller.dmserver.dto.datasource;

import com.tipdm.framework.controller.dmserver.dto.DataColumn;
import com.tipdm.framework.model.dmserver.PreViewMode;
//import io.swagger.annotations.ApiModel;
//import io.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * Created by TipDM on 2017/7/21.
 * E-mail:devp@tipdm.com
 */
//@ApiModel
public class Flat {

//    @ApiModelProperty(value = "数据表名称", position = 0, example = "tab_example")
    private String tableName;

//    @ApiModelProperty(value = "数据文件的MD5信息", position=1)
    private String fileMD5;

//    @ApiModelProperty(value = "字段信息集合", position = 2)
    private List<DataColumn> columns;

//    @ApiModelProperty(value = "保存时长,单位：天，最长可保存180天", allowableValues = "range[1,180]", example = "180", position = 3)
    private Integer duration = 180;

//    @ApiModelProperty(value = "数据预览模式，支持仅前100条和分页模式", position = 4)
    private PreViewMode previewMode = PreViewMode.ONLY100;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<DataColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<DataColumn> columns) {
        this.columns = columns;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getFileMD5() {
        return fileMD5;
    }

    public void setFileMD5(String fileMD5) {
        this.fileMD5 = fileMD5;
    }

    public PreViewMode getPreviewMode() {
        return previewMode;
    }

    public void setPreviewMode(PreViewMode previewMode) {
        this.previewMode = previewMode;
    }
}
