package com.tipdm.framework.controller.dmserver;

import com.tipdm.framework.common.controller.Result;
import com.tipdm.framework.common.controller.base.BaseController;
import com.tipdm.framework.common.token.TokenManager;
import com.tipdm.framework.common.utils.PropertiesUtil;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/banner")
//@Api(value = "/api/banner", position = 1, tags = "首页横幅管理", description = "配置、更新首页横幅的链接地址")
public class BannerController extends BaseController{

    private final Logger logger = LoggerFactory.getLogger(BannerController.class);


    @Autowired
    private TokenManager tokenManager;

    @RequiresPermissions("banner:bbs")
    @RequestMapping(value = "/bbs/", method = RequestMethod.GET)
//    @ApiOperation(value = "社区", position = 1)
    public Result bbs(@RequestHeader("accessToken") String accessToken,
                       HttpServletRequest request) {

        Result result = new Result();
        LinkedHashMap<String, String> urls = PropertiesUtil.getProperties("sysconfig/bbsBanner.properties");
        result.setData(urls);
        return result;
    }

    @RequiresPermissions("banner:documentation")
    @RequestMapping(value = "/documentation/", method = RequestMethod.GET)
//    @ApiOperation(value = "帮助文档", position = 1)
    public Result documentation(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                       HttpServletRequest request) {

        Result result = new Result();
        LinkedHashMap<String, String> urls = PropertiesUtil.getProperties("sysconfig/docsBanner.properties");
        result.setData(urls);

        return result;
    }
}
