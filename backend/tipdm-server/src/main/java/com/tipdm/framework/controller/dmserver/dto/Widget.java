package com.tipdm.framework.controller.dmserver.dto;

//import io.swagger.annotations.ApiModel;
//import io.swagger.annotations.ApiModelProperty;

/**
 * Created by TipDM on 2017/2/6.
 * E-mail:devp@tipdm.com
 */
//@ApiModel
public class Widget {

//    @ApiModelProperty(value = "显示名称",notes = "控件的中文显示名称，全局唯一", example = "文本框", position = 1)
    private String name;

//    @ApiModelProperty(value = "控件名",notes = "英文名称，全局唯一", example = "widget", position = 2)
    private String codeName;

//    @ApiModelProperty(value = "描述",notes = "对控件的一些描述", example = "文本信息", position = 3)
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCodeName() {
        return codeName;
    }

    public void setCodeName(String codeName) {
        this.codeName = codeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
