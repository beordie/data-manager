package com.tipdm.framework.controller.dmserver.dto;

//import io.swagger.annotations.ApiModel;
//import io.swagger.annotations.ApiModelProperty;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by TipDM on 2017/1/4.
 * E-mail:devp@tipdm.com
 */
//@ApiModel
public class Tab {

//    @ApiModelProperty(value = "选项卡名称", required = true, position = 1)
    private String tabName;

//    @ApiModelProperty(value = "参数", position = 2)
    private Set<Element> elements = new LinkedHashSet<>();

    public String getTabName() {
        return tabName;
    }

    public void setTabName(String tabName) {
        this.tabName = tabName;
    }

    public Set<Element> getElements() {
        return elements;
    }

    public void setElements(Set<Element> elements) {
        this.elements = elements;
    }
}
