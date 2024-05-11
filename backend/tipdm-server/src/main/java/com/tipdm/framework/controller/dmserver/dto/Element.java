package com.tipdm.framework.controller.dmserver.dto;

//import io.swagger.annotations.ApiModel;
//import io.swagger.annotations.ApiModelProperty;

import java.util.HashMap;

/**
 * Created by TipDM on 2017/1/4.
 * E-mail:devp@tipdm.com
 */
//@ApiModel
public class Element {

//    @ApiModelProperty(value = "参数名称", required = true, position = 1)
    private String name;

//    @ApiModelProperty(value = "Label", required = true, position = 2)
    private String label;

//    @ApiModelProperty(value = "UI组件类型，数据来源：/api/elementtype/list", required = true, position = 3)
    private Long elementType;

//    @ApiModelProperty(value = "默认值", position = 4)
    private String defaultValue;

//    @ApiModelProperty(value = "参数值", position = 5)
    private String value;

//    @ApiModelProperty(value = "placeholder", position = 6)
    private String placeholder;

//    @ApiModelProperty(value = "悬浮提示", position = 7)
    private String toolTip;

//    @ApiModelProperty(value = "排序索引", position = 8)
    private Integer sequence = 1;

//    @ApiModelProperty(value = "是否必填", position = 9)
    private Boolean isRequired = Boolean.FALSE;

//    @ApiModelProperty(value = "是否可见, default:true", position = 10)
    private Boolean isVisible = Boolean.TRUE;

//    @ApiModelProperty(value = "options, 组件类型为下拉框时使用， 数据格式：选项一:1;选项二:2", position = 11)
    private String options;

//    @ApiModelProperty(value = "正则表达式，验证输入是否合法", position = 12)
    private String rexp;

//    @ApiModelProperty(value = "扩展信息, 数据格式为键值对", position = 13)
    private HashMap<String, String> extra = new HashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getToolTip() {
        return toolTip;
    }

    public void setToolTip(String toolTip) {
        this.toolTip = toolTip;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Boolean getRequired() {
        return isRequired;
    }

    public void setRequired(Boolean required) {
        isRequired = required;
    }

    public Long getElementType() {
        return elementType;
    }

    public void setElementType(Long elementType) {
        this.elementType = elementType;
    }

    public HashMap<String, String> getExtra() {
        return extra;
    }

    public void setExtra(HashMap<String, String> extra) {
        this.extra = extra;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public String getRexp() {
        return rexp;
    }

    public void setRexp(String rexp) {
        this.rexp = rexp;
    }

    public Boolean getVisible() {
        return isVisible;
    }

    public void setVisible(Boolean visible) {
        isVisible = visible;
    }
}
