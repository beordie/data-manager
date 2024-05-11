package com.tipdm.framework.controller.dmserver;

import com.tipdm.framework.common.controller.Result;
import com.tipdm.framework.common.controller.base.BaseController;
import com.tipdm.framework.common.token.TokenManager;
import com.tipdm.framework.common.token.model.TokenModel;
import com.tipdm.framework.service.dmserver.ProjectService;
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
//import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
//import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("all")
//@ApiIgnore
@Controller
@RequestMapping("/api/user")
//@Api(value = "/api/user", position = 5, tags = "用户信息管理")
public class UserController extends BaseController {

	@Autowired
	private ProjectService projectService;

	@Autowired
	private TokenManager tokenManager;

	@RequestMapping(value = "/info", method = RequestMethod.GET)
//	@ApiOperation(value = "获取用户信息")
	public @ResponseBody Result getPermissions(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken, HttpServletResponse response) {

		Result result = new Result();

		TokenModel tokenModel = tokenManager.getPermissions(accessToken);
		if(null != tokenModel) {
			Map<String, Object> data = new HashMap<>();
			data.put("username", tokenModel.getUsername());
			data.put("permissions", tokenModel.getPermissions());
			result.setData(data);
			result.setStatus(Result.Status.SUCCESS);
		} else {
			result.setStatus(Result.Status.FAIL);
			result.setMessage("token无效或已过期");
		}
		return result;
	}

	@RequiresPermissions("user:shardUsers")
	@RequestMapping(value = "/shardUsers", method = RequestMethod.GET)
//	@ApiOperation(value = "获取可查看的用户资源")
	public @ResponseBody
	Result getShardUsers(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken, HttpServletResponse response) {
		Result result = new Result();
		TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        result.setData(tokenModel.getUserRoleOrg());
		return result;
	}
}
