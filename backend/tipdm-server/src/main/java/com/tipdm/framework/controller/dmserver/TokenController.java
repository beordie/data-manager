package com.tipdm.framework.controller.dmserver;

import com.tipdm.framework.common.controller.Result;
import com.tipdm.framework.common.controller.base.BaseController;
import com.tipdm.framework.common.token.TokenManager;
import com.tipdm.framework.common.token.model.TokenModel;
import com.tipdm.framework.common.utils.RedisUtils;
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
//import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by TipDM on 2017/1/3.
 * E-mail:devp@tipdm.com
 */
@Controller
@RequestMapping("/token")
//@Api(value = "/token", position = 1, tags = "Token管理")
@SuppressWarnings("all")
public class TokenController extends BaseController{


    @Autowired
    private TokenManager tokenManager;

    @RequestMapping(value = "/check", method = RequestMethod.GET)
//    @ApiOperation(value = "检查Token的有效性")
    public @ResponseBody
    Result check(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken, HttpServletResponse response) {

        Result result = new Result();

        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        if(null != tokenModel) {
            result.setMessage("您的token可以正常使用");
            result.setData(accessToken);
            result.setStatus(Result.Status.SUCCESS);
        } else {
            result.setStatus(Result.Status.FAIL);
            result.setMessage("token无效或已过期");
        }
        return result;
    }

    @RequestMapping(value = "/info", method = RequestMethod.GET)
//    @ApiOperation(value = "获取用户信息", notes = "返回信息包含用户名、拥有的权限、可分享的用户资源及当前系统的单机算法使用的语言引擎")
    public @ResponseBody Result getPermissions(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken, HttpServletResponse response) {

        Result result = new Result();

        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        if(null != tokenModel) {
            Map<String, Object> data = new HashMap<>();
            data.put("username", tokenModel.getUsername());
            data.put("permissions", tokenModel.getPermissions());
            data.put("shareable", tokenModel.getUserRoleOrg());
            result.setData(data);
            result.setStatus(Result.Status.SUCCESS);
        } else {
            result.setStatus(Result.Status.FAIL);
            result.setMessage("token无效或已过期");
        }
        return result;
    }

    @RequestMapping(value = "/invalidate", method = RequestMethod.GET)
//    @ApiOperation(value = "销毁Token")
    public @ResponseBody Result invalidate(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                                           HttpServletRequest request,
                                           HttpServletResponse response) throws Exception{

        Result result = new Result();
        TokenModel tokenModel = tokenManager.getPermissions(accessToken);

        if(null == tokenModel){
            result.setStatus(Result.Status.FAIL);
            result.setMessage("token无效或已过期");
            return result;
        }

        RedisUtils.remove(accessToken);
        result.setMessage("令牌销毁成功");
        return result;
    }
}
