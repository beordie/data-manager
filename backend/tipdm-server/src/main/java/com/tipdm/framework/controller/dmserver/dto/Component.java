package com.tipdm.framework.controller.dmserver.dto;

import com.tipdm.framework.model.dmserver.ComponentExtra;
import com.tipdm.framework.model.dmserver.Step;
//import io.swagger.annotations.ApiModel;
//import io.swagger.annotations.ApiModelProperty;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by TipDM on 2017/1/3.
 * E-mail:devp@tipdm.com
 */
//@ApiModel(value = "组件")
public class Component {

//    @ApiModelProperty(position = 1, required = true, value = "组件名称")
    private String name;

//    @ApiModelProperty(position = 3, required = true, value = "对应的算法服务类，数据来源：/api/algorithm/list")
    private String targetAlgorithm;

//    @ApiModelProperty(position = 4,required = true, value = "分类ID")
    private Long parentId = 0L;

//    @ApiModelProperty(position = 5, value = "对应的算法是否支持并行, default:false")
//    private Boolean paralleled = Boolean.FALSE;
//    @ApiModelProperty(position = 5, required = true, notes = "算法引擎")
    private ComponentExtra.ENGINE engine = ComponentExtra.ENGINE.PYTHON;

//    @ApiModelProperty(position = 6, value = "图标")
    private String iconPath;

//    @ApiModelProperty(position = 11, value = "定义组件输入数据源的最小数目")
    private Integer minimumInput = 0;//定义组件输入数据源的最小数目

//    @ApiModelProperty(position = 7, required = true, value = "这个组件的描述")
    private String description;

//    @ApiModelProperty(position = 8, value = "组件输入")
    private LinkedHashSet<Input> inputs = new LinkedHashSet<>();

//    @ApiModelProperty(position = 9, value = "组件输出")
    private LinkedHashSet<Output> outputs = new LinkedHashSet<>();

//    @ApiModelProperty(position = 10, value = "参数选项卡")
    private LinkedHashSet<Tab> tabs = new LinkedHashSet<>();

//    @ApiModelProperty(position = 11, value = "是否支持导出PMML")
    private Boolean isSupportPMML = Boolean.FALSE;//是否支持导出PMML

//    @ApiModelProperty(position = 12, value = "是否能够查看源码")
    private Boolean allowViewSource = Boolean.FALSE;

//    @ApiModelProperty(position = 13, value = "是否有输出运行报告，等于false时不显示查看报告的右键菜单")
    private Boolean hasReport = Boolean.FALSE;

//    @ApiModelProperty(position = 14, value = "脚本，JSON格式，key允许的取值范围:[MAIN,PREDICT,EVALUATE]")
    private Map<Step, String> script = new LinkedHashMap<Step, String>();

//    @ApiModelProperty(position = 15, value = "组件是否可用，属性值为false时当前组件不会在流程设计器的组件面板显示")
    private Boolean isEnabled = Boolean.TRUE;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTargetAlgorithm() {
        return targetAlgorithm;
    }

    public void setTargetAlgorithm(String targetAlgorithm) {
        this.targetAlgorithm = targetAlgorithm;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

//    public Boolean getParalleled() {
//        return this.paralleled;
//    }
//
//    public void setParalleled(Boolean paralleled) {
//        this.paralleled = paralleled;
//    }

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

    public Integer getMinimumInput() {
        return minimumInput;
    }

    public void setMinimumInput(Integer minimumInput) {
        this.minimumInput = minimumInput;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LinkedHashSet<Input> getInputs() {
        return inputs;
    }

    public void setInputs(LinkedHashSet<Input> inputs) {
        this.inputs = inputs;
    }

    public LinkedHashSet<Output> getOutputs() {
        return outputs;
    }

    public void setOutputs(LinkedHashSet<Output> outputs) {
        this.outputs = outputs;
    }

    public Set<Tab> getTabs() {
        return tabs;
    }

    public void setTabs(LinkedHashSet<Tab> tabs) {
        this.tabs = tabs;
    }

    public Boolean getSupportPMML() {
        return isSupportPMML;
    }

    public void setSupportPMML(Boolean supportPMML) {
        isSupportPMML = supportPMML;
    }

    public Boolean getAllowViewSource() {
        return allowViewSource;
    }

    public void setAllowViewSource(Boolean allowViewSource) {
        this.allowViewSource = allowViewSource;
    }

    public Boolean getHasReport() {
        return hasReport;
    }

    public void setHasReport(Boolean hasReport) {
        this.hasReport = hasReport;
    }

    public Map<Step, String> getScript() {
        return script;
    }

    public void setScript(Map<Step, String> script) {
        this.script = script;
    }

    public Boolean getEnabled() {
        return isEnabled;
    }

    public void setEnabled(Boolean enabled) {
        isEnabled = enabled;
    }

    public ComponentExtra.ENGINE getEngine() {
        return engine;
    }

    public void setEngine(ComponentExtra.ENGINE engine) {
        this.engine = engine;
    }
}
