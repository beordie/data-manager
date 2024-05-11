package com.tipdm.framework.controller.dmserver.dto;

import com.tipdm.framework.model.dmserver.DataType;
//import io.swagger.annotations.ApiModel;
//import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * Created by TipDM on 2016/12/20.
 * E-mail:devp@tipdm.com
 * 表字段
 */
//@ApiModel
public class DataColumn implements Serializable{

//    @ApiModelProperty(value = "列名", example = "field_1")
    private String name;

//    @ApiModelProperty(value = "数据类型", example = "character")
    private DataType dataType = DataType.text;

//    @ApiModelProperty(value = "备注", example = "字段备注")
    private String comment;

//    @ApiModelProperty(value = "日期格式")
    private String formatter;

//    @ApiModelProperty(value = "长度", allowableValues = "range[1,255]", example = "1")
    private Integer length = 1;

//    @ApiModelProperty(value = "精度", example = "0")
    private Integer scale;

    public DataColumn(){

    }

    public DataColumn(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public String getComment(String tableName) {
        return "comment on column " + tableName + "."+ name + " is '" + comment + "';";
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Integer getScale() {
        return scale;
    }

    public void setScale(Integer scale) {
        this.scale = scale;
    }

    @Override
    public String toString() {

        String column = this.name + " " + this.dataType;

        if(this.dataType == DataType.numeric){
            if(this.scale == null){
                column += "("+ this.length +")";
            } else {
                column += "("+ this.length +", "+ this.scale +")";
            }
        }

        if(this.dataType == DataType.timestamp){
            column += " without time zone";
        }
        return column;
    }

    public String getFormatter() {
        return formatter;
    }

    public void setFormatter(String formatter) {
        this.formatter = formatter;
    }
}
