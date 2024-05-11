package com.tipdm.framework.controller.dmserver;

import com.tipdm.framework.common.controller.Result;
import com.tipdm.framework.common.controller.base.BaseController;
import com.tipdm.framework.common.token.TokenManager;
import com.tipdm.framework.model.dmserver.Template;
import com.tipdm.framework.service.dmserver.TemplateService;
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
//import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/template")
//@Api(value = "/api/template", position = 1, tags = "模板管理")
public class TemplateController extends BaseController{

    private final Logger logger = LoggerFactory.getLogger(TemplateController.class);

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private TemplateService templateService;

    @RequiresPermissions("template:list")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
//    @ApiOperation(value = "模板列表")
    public Result getOwnList(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                             /*@ApiParam(value = "页码", required = true)*/ @RequestParam(value = "pageNumber", defaultValue = "1") int pageNumber,
                             /*@ApiParam(value = "页大小", required = true)*/ @RequestParam(value = "pageSize", defaultValue = "10") int pageSize
                             ) {

        Result result = new Result();
        Page<Template> templates = templateService.findTemplateList(buildPageRequest(pageNumber, pageSize));
        result.setData(templates);
        result.setStatus(Result.Status.SUCCESS);
        return result;
    }

    @RequiresPermissions("template:delete")
    @RequestMapping(value = "/{templateId}", method = RequestMethod.DELETE)
//    @ApiOperation(value = "删除模板")
    public Result getOwnList(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                             /*@ApiParam(value = "模版ID", required = true)*/ @PathVariable(value = "templateId") Long templateId){
        Result result = new Result();
        try {
            templateService.delete(templateId);
            result.setData("模板删除成功");
        } catch (Exception ex){
            result.setData("模版不存在或已被删除");
            result.setStatus(Result.Status.FAIL);
        }
        return result;
    }

}
