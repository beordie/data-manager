package com.tipdm.framework.controller.dmserver;

import com.tipdm.framework.common.controller.Result;
import com.tipdm.framework.common.controller.base.BaseController;
import com.tipdm.framework.common.token.TokenManager;
import com.tipdm.framework.common.token.model.TokenModel;
import com.tipdm.framework.common.utils.RedisUtils;
import com.tipdm.framework.dmserver.exception.AlgorithmException;
import com.tipdm.framework.dmserver.mse.ModelVersion;
import com.tipdm.framework.dmserver.utils.Constants;
import com.tipdm.framework.model.dmserver.Model;
import com.tipdm.framework.service.dmserver.ModelService;
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
//import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import org.dmg.pmml.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by TipDM on 2017/1/3.
 * E-mail:devp@tipdm.com
 */
@SuppressWarnings("all")
@RestController
@RequestMapping("/api/mse")
//@Api(value = "/api/mse", position = 15, tags = "模型管理")
public class MSEController extends BaseController {

    private final static Logger LOG = LoggerFactory.getLogger(MSEController.class);

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private ModelService modelService;


    @RequiresPermissions("mse:owned")
    @RequestMapping(value = "/owned", method = RequestMethod.GET)
//    @ApiOperation(value = "获取个人模型列表", position = 2)
    public Result getOwnedModels(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                               /*@ApiParam(value = "模型名称")*/ @RequestParam(value = "modelName", required = false) String modelName,
                               /*@ApiParam(value = "查询在指定时间段部署的模型，开始时间, 格式: yyyy-MM-dd HH:mm:ss", defaultValue = "2016-01-01 00:00:00")*/ @RequestParam(required = false) Date beginTime,
                               /*@ApiParam(value = "结束时间, 格式: yyyy-MM-dd HH:mm:ss", defaultValue = "2016-01-02 00:00:00")*/ @RequestParam(required = false) Date endTime,
                               /*@ApiParam(value = "页码", required = true)*/ @RequestParam(value = "pageNumber", defaultValue = "1") int pageNumber,
                               /*@ApiParam(value = "页大小", required = true)*/ @RequestParam(value = "pageSize", defaultValue = "10") int pageSize)  {

        Result result = new Result();
        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        Map<String, Object> params = new HashMap<>();
        params.put("creatorId", tokenModel.getUserId());
        params.put("modelName", modelName);
        params.put("beginTime", beginTime);
        params.put("endTime", endTime);
        Page<Model> page = modelService.findOwnModels(params, buildPageRequest(pageNumber,pageSize));
        result.setData(page);
        result.setMessage("个人模型列表加载成功");

        return result;
    }


    @RequiresPermissions("mse:deploy")
    @RequestMapping(value = "/{nodeId}/deploy", method = RequestMethod.POST)
//    @ApiOperation(value = "部署模型", position = 3, notes = "模型更新后，可选择覆盖原有模型或重新部署模型")
    public Result deploy(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                         /*@ApiParam(value = "组件的客户端Id", required = true)*/@PathVariable(name = "nodeId") String nodeId,
                         HttpServletRequest request) throws AlgorithmException, IOException {

        Result result = new Result();
        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        Model unsavedModel = (Model)RedisUtils.getFromMap(Constants.UN_SAVED_MODEL, nodeId);
        if(unsavedModel == null){
            result.setStatus(Result.Status.FAIL);
            result.setMessage("未能找到对应的模型对象");
            return result;
        }
        try {
            unsavedModel.setCreatorId(tokenModel.getUserId());
            Long id = modelService.deployModel(unsavedModel);
            result.setData(id);
            RedisUtils.removeFromMap(Constants.UN_SAVED_MODEL, nodeId);
        } catch (Exception ex){
            result.setMessage("模型部署失败，错误信息：" + ex.getMessage());
            result.setStatus(Result.Status.FAIL);
        }
        return result;
    }


    @RequiresPermissions("mse:redeploy")
    @RequestMapping(value = "/{nodeId}/redeploy/{version}", method = RequestMethod.POST)
//    @ApiOperation(value = "重新部署模型", position = 3)
    public Result redeploy(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                           /*@ApiParam(value = "组件的客户端Id", required = true)*/@PathVariable(name = "nodeId") String nodeId,
                           /*@ApiParam(value = "版本号", required = true)*/ @PathVariable(name = "version")Integer version,
                           HttpServletRequest request) throws AlgorithmException, IOException {

        Result result = new Result();
        TokenModel tokenModel = tokenManager.getPermissions(accessToken);

        Model unsavedModel = (Model)RedisUtils.getFromMap(Constants.UN_SAVED_MODEL, nodeId);
        if(unsavedModel == null){
            result.setStatus(Result.Status.FAIL);
            result.setMessage("未能找到对应的模型对象");
            return result;
        }
        try {
            unsavedModel.setCreatorId(tokenModel.getUserId());
            Long id = modelService.redeployModel(unsavedModel, version);
            result.setData(id);
            RedisUtils.removeFromMap(Constants.UN_SAVED_MODEL, nodeId);
        } catch (Exception ex){
            result.setMessage("模型部署失败，错误信息：" + ex.getMessage());
            result.setStatus(Result.Status.FAIL);
        }
        return result;
    }

    /**
     *
     * @param accessToken
     * @param modelId
     * @param request
     * @return
     * @throws Exception
     */
    @RequiresPermissions("mse:detail")
    @RequestMapping(value = "/{modelId}/", method = RequestMethod.GET)
//    @ApiOperation(value = "查看模型信息", notes = "查看模型的输入列、输出列...", position = 3)
    public Result showModelDetail(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                                  /*@ApiParam(value = "模型ID", required = true)*/@PathVariable(name = "modelId") Long modelId,
                                  HttpServletRequest request) throws AlgorithmException{

        Result result = new Result();
        Model model = modelService.findOne(modelId);
        if(model == null){
            throw new NullPointerException("model not found");
        }
        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        String modelDir = RedisUtils.get(Constants.MODEL_DIR, String.class);

        File modelFile = new File(modelDir, model.getModelPath());
        if(!modelFile.exists()){
            throw new NullPointerException("can not found model file");
        }

        Map<String, List<Field>> schema = new LinkedHashMap<>();
        schema.put("inputFields", model.getFeatures());
        schema.put("targetFields", model.getTarget());
        schema.put("outputFields", model.getOutputs());
        result.setData(schema);
        return result;
    }

    @RequiresPermissions("mse:unSavedMode")
    @RequestMapping(value = "/{nodeId}/preview", method = RequestMethod.GET)
//    @ApiOperation(value = "查看模型信息(未保存的)", notes = "查看模型的输入列、输出列...", position = 3)
    public Result previewModel(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                                  /*@ApiParam(value = "组件的客户端Id", required = true)*/@PathVariable(name = "nodeId") String nodeId,
                                  HttpServletRequest request) throws AlgorithmException{

        Result result = new Result();
        Model model = (Model)RedisUtils.getFromMap(Constants.UN_SAVED_MODEL, nodeId);
        if(model == null){
            throw new NullPointerException("Model Not found");
        }
        ModelVersion modelVersion = (ModelVersion) RedisUtils.getFromMap(Constants.MODEL_VERSION, model.getNodeId());
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("inputFields", model.getFeatures());
        schema.put("targetFields", model.getTarget());
        schema.put("outputFields", model.getOutputs());
        schema.put("versionInfo", modelVersion);
        result.setData(schema);
        return result;
    }
}
