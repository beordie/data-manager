package com.tipdm.framework.controller.dmserver.dto.datasource;

//import io.swagger.annotations.ApiModel;
//import io.swagger.annotations.ApiModelProperty;
import org.springframework.util.Assert;

/**
 * Created by TipDM on 2017/7/27.
 * E-mail:devp@tipdm.com
 */
//@ApiModel
public class Connection {

//    @ApiModelProperty(value = "用户名", example = "user", position = 0)
    private String userName;

//    @ApiModelProperty(value = "密码", example = "password", position = 1)
    private String password;

//    @ApiModelProperty(value = "jdbc连接字符串", example = "jdbc:postgresql://192.168.0.1:5432/test", position = 2)
    private String url;

//    @ApiModelProperty(value = "取数SQL", example = "select field_1 from tab_sample", position = 3)
    private String sql;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        Assert.hasLength(url, "URL不能为空");
        this.url = url;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        Assert.hasLength(sql, "取数SQL不能为空");
        this.sql = sql;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        sb.append(this.userName).append(",")
                .append(this.password).append(",")
                .append(this.url).append(",")
                .append(this.sql)
                .append("]");

        return sb.toString();
    }
}
