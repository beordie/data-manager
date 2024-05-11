package com.tipdm.framework.controller.dmserver.dto;

import com.tipdm.framework.model.dmserver.ComponentIO;
//import io.swagger.annotations.ApiModel;
//import io.swagger.annotations.ApiModelProperty;

/**
 * Created by TipDM on 2017/1/4.
 * E-mail:devp@tipdm.com
 */
//@ApiModel()
public class Output {

//    @ApiModelProperty(position = 1, required = true, notes = "输入/输出数据源名称")
    private String key;//参数名称

//    @ApiModelProperty(position = 2, required = true, notes = "输出", hidden = true, readOnly = true)
    private ComponentIO.IOType type = ComponentIO.IOType.OUTPUT;

//    @ApiModelProperty(name = "cat", position = 3, notes = "输入/输出类别", required = true)
    private ComponentIO.Category cat = ComponentIO.Category.DATA;

//    @ApiModelProperty(position = 4, notes = "输出节点是否能够预览（不支持模型和非机构化文件）")
    private Boolean canPreview = Boolean.FALSE;

//    @ApiModelProperty(position = 5, notes = "输入/输出数据的描述", required = true)
    private String description;

//    @ApiModelProperty(position = 6, notes = "元数据获得方式", required = true)
    private ComponentIO.MetaDataAccess access = ComponentIO.MetaDataAccess.SAME;

//    @ApiModelProperty(position = 7, notes = "追加的字段，格式：json", required = false)
    private String columns = "[]";

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Boolean getCanPreview() {
        return canPreview;
    }

    public void setCanPreview(Boolean canPreview) {
        this.canPreview = canPreview;
        if(cat != ComponentIO.Category.DATA){
            this.canPreview = false;
        }
    }

    public ComponentIO.Category getCat() {
        return cat;
    }

    public void setCat(ComponentIO.Category cat) {
        this.cat = cat;
    }

    public ComponentIO.IOType getType() {
        return type;
    }

    public String getColumns() {
        return columns;
    }

    public void setColumns(String columns) {
        if(getAccess() == ComponentIO.MetaDataAccess.APPEND) {
            this.columns = columns;
        }
    }

    public ComponentIO.MetaDataAccess getAccess() {
        return access;
    }

    public void setAccess(ComponentIO.MetaDataAccess access) {
        this.access = access;
    }
}
