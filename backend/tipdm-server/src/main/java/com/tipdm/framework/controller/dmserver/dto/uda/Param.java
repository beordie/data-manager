package com.tipdm.framework.controller.dmserver.dto.uda;

//import io.swagger.annotations.ApiModel;
//import io.swagger.annotations.ApiModelProperty;

/**
 * Created by TipDM on 2017/4/6.
 * E-mail:devp@tipdm.com
 */
//@ApiModel(value = "算法参数")
public class Param {

//    @ApiModelProperty(value = "参数名称")
    private String name;

//    @ApiModelProperty(value = "参数值")
    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
