package com.tipdm.framework.controller.dmserver.dto;

import com.tipdm.framework.model.dmserver.ComponentIO;
//import io.swagger.annotations.ApiModel;
//import io.swagger.annotations.ApiModelProperty;

/**
 * Created by TipDM on 2017/1/4.
 * E-mail:devp@tipdm.com
 */
//@ApiModel()
public class Input {

//    @ApiModelProperty(position = 1, required = true, notes = "数据源名称")
    private String key;//参数名称

//    @ApiModelProperty(position = 2, required = true, notes = "输入", hidden = true, readOnly = true)
    private ComponentIO.IOType type = ComponentIO.IOType.INPUT;

//    @ApiModelProperty(name = "cat", position = 3, notes = "数据类别", required = true)
    private ComponentIO.Category cat = ComponentIO.Category.DATA;

//    @ApiModelProperty(position = 5, notes = "描述", required = true)
    private String description;

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

    public ComponentIO.Category getCat() {
        return cat;
    }

    public void setCat(ComponentIO.Category cat) {
        this.cat = cat;
    }

    public ComponentIO.IOType getType() {
        return type;
    }
}
