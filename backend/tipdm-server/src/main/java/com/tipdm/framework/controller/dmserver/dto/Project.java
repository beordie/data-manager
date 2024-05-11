package com.tipdm.framework.controller.dmserver.dto;

//import io.swagger.annotations.ApiModel;
//import io.swagger.annotations.ApiModelProperty;

/**
 * Created by TipDM on 2016/12/29.
 * E-mail:devp@tipdm.com
 */
//@ApiModel
public class Project {

//    @ApiModelProperty(position = 1, value = "工程名称")
    private String name;

//    @ApiModelProperty(position = 2, value = "分类ID")
    private Long parentId = 0L;

//    @ApiModelProperty(position = 3, value = "是否支持分布式计算")
    private Boolean isParalleled = Boolean.FALSE;

//    @ApiModelProperty(position = 4, value = "对工程的一些描述")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Boolean getParalleled() {
        return isParalleled;
    }

    public void setParalleled(Boolean paralleled) {
        isParalleled = paralleled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
