package com.tipdm.framework.controller.dmserver.dto.uda;

import com.tipdm.framework.controller.dmserver.dto.Input;
import com.tipdm.framework.controller.dmserver.dto.Output;
import com.tipdm.framework.controller.dmserver.dto.Tab;
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
//@ApiModel(value = "自定义组件")
public class UDAComponent {

//    @ApiModelProperty(position = 1, required = true, notes = "组件名称")
    private String name;

//    @ApiModelProperty(position = 4, notes = "自定义算法的主函数")
    private String mainClass;
//
//    @ApiModelProperty(position = 4, value = "分类ID")
//    private Long parentId = 0L;

//    @ApiModelProperty(position = 2, required = true, value = "算法类别, cluster: 聚类; classify:分类/预测; other:其它")
//    private ComponentExtra.UDAType udaType;

//    @ApiModelProperty(position = 3, required = true, notes = "算法引擎，可选项有：[R, PYTHON, SPARK, HADOOP]")
    private ComponentExtra.ENGINE engine;

//    @ApiModelProperty(position = 5, value = "是否支持并行计算")
//    private Boolean paralleled = Boolean.FALSE;

//    @ApiModelProperty(position = 6, value = "图标")
//    private String iconPath;

//    @ApiModelProperty(position = 6, required = true, notes = "脚本，JSON格式，key允许的取值范围:[MAIN,PREDICT,EVALUATE]")
    private Map<Step, String> script = new LinkedHashMap<Step, String>();

//    @ApiModelProperty(position = 7, notes = "这个组件的描述")
    private String description;

//    @ApiModelProperty(position = 8, notes = "组件输入")
    private LinkedHashSet<Input> inputs = new LinkedHashSet<>();

//    @ApiModelProperty(position = 9, notes = "组件输出")
    private LinkedHashSet<Output> outputs = new LinkedHashSet<>();

//    @ApiModelProperty(position = 10, notes = "参数选项卡")
    private LinkedHashSet<Tab> tabs = new LinkedHashSet<>();

//    @ApiModelProperty(position = 11, notes = "定义组件输入数据源的最小数目")
    private Integer minimumInput = 0;//定义组件输入数据源的最小数目

//    @ApiModelProperty(position = 12, notes = "是否支持导出PMML")
    private Boolean supportPMML = Boolean.FALSE;//是否支持导出PMML

//    @ApiModelProperty(position = 13, notes = "是否能够查看源码")
    private Boolean allowViewSource = Boolean.FALSE;

//    @ApiModelProperty(position = 14, notes = "是否有输出运行报告，等于false时不显示查看报告的右键菜单")
    private Boolean hasReport = Boolean.FALSE;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

//    public ComponentExtra.UDAType getUdaType() {
//        return udaType;
//    }

//    public void setUdaType(ComponentExtra.UDAType udaType) {
//        this.udaType = udaType;
//    }


//    public Boolean getParalleled() {
//        return paralleled;
//    }
//
//    public void setParalleled(Boolean paralleled) {
//        paralleled = paralleled;
//    }

    public Integer getMinimumInput() {
        return minimumInput;
    }

    public void setMinimumInput(Integer minimumInput) {
        this.minimumInput = minimumInput;
    }

    public Map<Step, String> getScript() {
        return script;
    }

    public void setScript(Map<Step, String> script) {
        this.script = script;
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
        return supportPMML;
    }

    public void setSupportPMML(Boolean supportPMML) {
        this.supportPMML = supportPMML;
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

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public ComponentExtra.ENGINE getEngine() {
        return engine;
    }

    public void setEngine(ComponentExtra.ENGINE engine) {
        this.engine = engine;
    }
}
