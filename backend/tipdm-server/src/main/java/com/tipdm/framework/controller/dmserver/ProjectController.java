package com.tipdm.framework.controller.dmserver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tipdm.framework.common.Constants;
import com.tipdm.framework.common.controller.Result;
import com.tipdm.framework.common.controller.base.BaseController;
import com.tipdm.framework.common.token.TokenManager;
import com.tipdm.framework.common.token.model.TokenModel;
import com.tipdm.framework.common.utils.FileKit;
import com.tipdm.framework.common.utils.PropertiesUtil;
import com.tipdm.framework.common.utils.RedisUtils;
import com.tipdm.framework.common.utils.StringKit;
import com.tipdm.framework.controller.dmserver.dto.Doc;
import com.tipdm.framework.dmserver.core.scheduling.model.Node;
import com.tipdm.framework.dmserver.core.scheduling.Command;
import com.tipdm.framework.dmserver.core.scheduling.WorkFlow;
import com.tipdm.framework.dmserver.core.scheduling.WorkFlowScheduler;
import com.tipdm.framework.dmserver.exception.IllegalOperationException;
import com.tipdm.framework.dmserver.utils.Base62;
import com.tipdm.framework.model.dmserver.*;
import com.tipdm.framework.service.dmserver.*;
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
//import io.swagger.annotations.ApiParam;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by TipDM on 2016/12/21.
 * E-mail:devp@tipdm.com
 */

@SuppressWarnings("all")
@RestController
@RequestMapping("/api/project")
//@PropertySource(value = "classpath:sysconfig/system.properties")
//@Api(value = "/api/project", position = 4, tags = "挖掘工程管理")
public class ProjectController extends BaseController {

    private final Logger logger = LoggerFactory.getLogger(com.tipdm.framework.controller.dmserver.ProjectController.class);

    @Autowired
    private ProjectService projectService;

    @Autowired
    private DataTableService tableService;

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private WorkFlowScheduler workFlowScheduler;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private ComponentService componentService;

    @Autowired
    private UDAService udaService;

    @Autowired
    private ModelService modelService;

    @RequiresPermissions("project:addCat")
    @RequestMapping(value = "/cat", method = RequestMethod.POST)
//    @ApiOperation(value = "新增分类", notes = "添加新的工程分类")
    public Result createDocument(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                                 /*@ApiParam(value = "工程分类", required = true, name = "cat")*/ @RequestBody Doc doc,
                                 HttpServletRequest request) {

        Result result = new Result();

        TokenModel tokenModel = tokenManager.getPermissions(accessToken);

        String docDir = RedisUtils.get(com.tipdm.framework.dmserver.utils.Constants.DOCUMENT_DIR, String.class) + "/" + tokenModel.getUsername();

        Document document = new Document();
        BeanUtils.copyProperties(doc, document);

        document.setCreatorId(tokenModel.getUserId());
        document.setCreatorName(tokenModel.getUsername());
        document.setDelete(Boolean.FALSE);
        document.setLeaf(Boolean.FALSE);

        Long docId = projectService.saveDocument(document);

        File dir = new File(docDir + "/" + projectService.getRealPathByDocumentId(docId));
        dir.mkdirs();
        result.setMessage("工程分类添加成功");
        result.setData(docId);
        return result;
    }

    @RequiresPermissions("project:child")
    @RequestMapping(value = "/{documentId}/child", method = RequestMethod.GET)
//    @ApiOperation(value = "获取子级节点", notes = "获取指定分类下的工程分类和挖掘工程")
    public Result getChild(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                           /*@ApiParam(value = "工程分类Id", required = true)*/ @PathVariable(name = "documentId") Long documentId) {
        Result result = new Result();

        TokenModel tokenModel = tokenManager.getPermissions(accessToken);

        List<Document> docList = projectService.getChild(documentId, tokenModel.getUserId());

        result.setData(docList);
        result.setMessage("数据加载成功");

        return result;
    }

    @RequiresPermissions("project:up")
    @RequestMapping(value = "/{documentId}/up", method = RequestMethod.PUT)
//    @ApiOperation(value = "上移", notes = "组件的排序值减1")
    public Result up(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                     /*@ApiParam(value = "文档Id", required = true)*/ @PathVariable(name = "documentId") Long documentId) {
        Result result = new Result();
        TokenModel tokenModel = tokenManager.getPermissions(accessToken);

        Integer sequence = documentService.update(documentId, "up");
        result.setMessage("上移成功");
        result.setData(sequence);
        return result;
    }

    @RequiresPermissions("project:down")
    @RequestMapping(value = "/{documentId}/down", method = RequestMethod.PUT)
//    @ApiOperation(value = "下移", notes = "组件的排序值加1")
    public Result down(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                       /*@ApiParam(value = "文档Id", required = true)*/ @PathVariable(name = "documentId") Long documentId) {
        Result result = new Result();
        TokenModel tokenModel = tokenManager.getPermissions(accessToken);

        Integer sequence = documentService.update(documentId, "down");
        result.setData(sequence);
        result.setMessage("下移成功");
        return result;
    }

    @RequiresPermissions("project:deleteCat")
    @RequestMapping(value = "/cat/{documentId}", method = RequestMethod.DELETE)
//    @ApiOperation(value = "删除工程分类", notes = "删除分类会同时删除分类下的所有子分类和工程")
    public Result deleteCategory(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                                 /*@ApiParam(value = "分类Id", required = true)*/ @PathVariable(name = "documentId") Long documentId,
                                 HttpServletRequest request) throws IOException {
        Result result = new Result();

        TokenModel tokenModel = tokenManager.getPermissions(accessToken);

        Document doc = documentService.findOne(documentId);//查询，先获取getSequence()
        List<Document> child = documentService.findChild(doc.getParentId(), doc.getCreatorId());//根据父节点的ID，获取该父节点下所有子节点
        Integer sequence = doc.getSequence();
        if (sequence == child.size() - 1) {//已是最底下不需要更新
        } else {
            Integer number = sequence + 1;//删除一个，从下一个排序的数据开始更新所有sequence
            for (int i = number; i < child.size(); i++) {
                Document docList = child.get(i);
                docList.setSequence(docList.getSequence() - 1);
                documentService.updateDocument(docList);
            }
        }

        String docDir = RedisUtils.get(com.tipdm.framework.dmserver.utils.Constants.DOCUMENT_DIR, String.class) + "/" + tokenModel.getUsername();
        projectService.deleteDocument(documentId, docDir);

        result.setMessage("工程分类删除成功");
        return result;
    }

    @RequiresPermissions("project:create")
    @RequestMapping(value = "/", method = RequestMethod.POST)
//    @ApiOperation(value = "创建数据挖掘工程")
    public Result create(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                         /*@ApiParam(value = "工程", required = true)*/ @RequestBody com.tipdm.framework.controller.dmserver.dto.Project project,
                         HttpServletRequest request) throws IOException {

        Result result = new Result();
        Project pro = new Project();

        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        String docDir = RedisUtils.get(com.tipdm.framework.dmserver.utils.Constants.DOCUMENT_DIR, String.class) + "/" + tokenModel.getUsername();

        BeanUtils.copyProperties(project, pro);
        pro.setCreatorName(tokenModel.getUsername());
        pro.setCreatorId(tokenModel.getUserId());
        projectService.save(pro);

        File parentDir = new File(docDir + "/" + projectService.getRealPathByDocumentId(pro.getParentId()));
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        File file = new File(parentDir, pro.getName() + ".json");
        file.createNewFile();
        FileKit.writeStringToFile(file, "{\n" +
                " style: {\n" +
                "      width: 100,\n" +
                "      height: 120,\n" +
                "      isMini: false,\n" +
                "      curHeight: 0,\n" +
                "      curWidth: 0\n" +
                "    },\n" +
                " nodes: [],\n" +
                " links: []\n" +
                "}");
        result.setMessage("工程创建成功");
        result.setData(pro.getId());
        return result;
    }

    @RequiresPermissions("project:getProject")
    @RequestMapping(value = "/{projectId}", method = RequestMethod.GET)
//    @ApiOperation(value = "获取工程")
    public Result getProject(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                             /*@ApiParam(value = "工程ID", required = true, name = "projectId")*/ @PathVariable(name = "projectId") Long projectId,
                             HttpServletRequest request) throws Exception {
        Result result = new Result();
        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        Project project = projectService.findOne(projectId);
        if (null == project) {
            result.setMessage("工程不存在或已被删除");
            result.setStatus(Result.Status.FAIL);
            return result;
        }
        project.setLastOpenTime(Calendar.getInstance().getTime());
        Document doc = projectService.findDocumentByProjectId(projectId);
        String docDir = RedisUtils.get(com.tipdm.framework.dmserver.utils.Constants.DOCUMENT_DIR, String.class) + "/" + tokenModel.getUsername();
        File parentDir = new File(docDir + "/" + projectService.getRealPathByDocumentId(doc.getParentId()));
        File file = new File(parentDir, project.getName() + ".json");
        //获取历史版本信息
        Map<String, Map<String, String>> versions = (Map<String, Map<String, String>>) RedisUtils.getFromMap(com.tipdm.framework.dmserver.utils.Constants.PROJECT_HISTORY_VERSION, projectId.toString());
        if(versions == null){
            versions = new HashMap<>();
        }
        project.setVersions(new TreeMap(versions));

        List<Component> udComponents = udaService.findList(0L, tokenModel.getUserId());
        List<Map<String, Object>> list = udComponents.stream().map(x -> {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("serverId", x.getId());
            map.put("last_update_time", x.getUpdateTime());
            return map;
        }).collect(Collectors.toList());
        list.addAll(componentService.findAllInBuiltComponent().stream().map(x -> {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("serverId", x.getId());
            map.put("last_update_time", x.getUpdateTime());
            return map;
        }).collect(Collectors.toList()));

        String content = FileUtils.readFileToString(file, Constants.CHARACTER);
        JSONObject jsonObject = JSON.parseObject(content);
        if(jsonObject == null){
            jsonObject = new JSONObject();
        }
        jsonObject.put("summary", list);
        project.setJson(jsonObject.toJSONString());
        result.setData(project);
        result.setMessage("工程获取成功");

        return result;
    }

    @RequiresPermissions("project:version")
    @RequestMapping(value = "/{projectId}", method = RequestMethod.PUT)
//    @ApiOperation(value = "创建历史版本")
    public Result createHistory(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                                /*@ApiParam(value = "工程ID", required = true, name = "projectId")*/ @PathVariable(name = "projectId") Long projectId,
                                /*@ApiParam(value = "描述")*/ @RequestParam(name = "description", required = false) String description,
                                HttpServletRequest request) throws Exception {

        Result result = new Result();
        TokenModel tokenModel = tokenManager.getPermissions(accessToken);

        Project project = projectService.findOne(projectId);
        if (null == project) {
            result.setMessage("工程不存在或已被删除");
            result.setStatus(Result.Status.FAIL);
            return result;
        }
        Document doc = projectService.findDocumentByProjectId(projectId);
        String docDir = RedisUtils.get(com.tipdm.framework.dmserver.utils.Constants.DOCUMENT_DIR, String.class) + "/" + tokenModel.getUsername();
        File parentDir = new File(docDir + "/" + projectService.getRealPathByDocumentId(doc.getParentId()));
        File file = new File(parentDir, project.getName() + ".json");
        Calendar calendar = Calendar.getInstance();
        String createTime = DateFormatUtils.format(calendar, com.tipdm.framework.dmserver.utils.Constants.DFT_YYYY_MM_DD_HH_MM_SS);
        String ver = Base62.encode(calendar.getTimeInMillis());
        String destFile = project.getName() + "_" + ver + ".json";
        Map<String, String> version = new HashMap<>();
        version.put("createTime", createTime);
        version.put("description", description);
        Map<String, Map<String, String>> versions = (Map<String, Map<String, String>>) RedisUtils.getFromMap(com.tipdm.framework.dmserver.utils.Constants.PROJECT_HISTORY_VERSION, projectId.toString());
        if (null == versions) {
            versions = new HashMap<>();
        }
        versions.put(ver, version);
        RedisUtils.putToMap(com.tipdm.framework.dmserver.utils.Constants.PROJECT_HISTORY_VERSION, projectId.toString(), versions);
        FileKit.copyFile(file, new File(parentDir, destFile));
        result.setData(versions);

        return result;
    }

    @RequiresPermissions("project:getByVersion")
    @RequestMapping(value = "/{projectId}/{version}", method = RequestMethod.GET)
//    @ApiOperation(value = "查看工程的历史版本")
    public Result getByVersion(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                               /*@ApiParam(value = "工程ID", required = true, name = "projectId")*/ @PathVariable(name = "projectId") Long projectId,
                               /*@ApiParam(value = "版本号", required = true, name = "version")*/ @PathVariable(name = "version") String version,
                               HttpServletRequest request) throws Exception {

        Result result = new Result();
        TokenModel tokenModel = tokenManager.getPermissions(accessToken);

        Project project = projectService.findOne(projectId);
        if (null == project) {
            result.setMessage("工程不存在或已被删除");
            result.setStatus(Result.Status.FAIL);
            return result;
        }
        Document doc = projectService.findDocumentByProjectId(projectId);
        String docDir = RedisUtils.get(com.tipdm.framework.dmserver.utils.Constants.DOCUMENT_DIR, String.class) + "/" + tokenModel.getUsername();
        //获取历史版本信息
        Map<String, Map<String, String>> versions = (Map<String, Map<String, String>>) RedisUtils.getFromMap(com.tipdm.framework.dmserver.utils.Constants.PROJECT_HISTORY_VERSION, projectId.toString());
        Map<String, String> verMap = versions.get(version);

        if (null == verMap) {
            result.setMessage("未能找到对应的版本文件");
            result.setStatus(Result.Status.FAIL);
            return result;
        }
        String destFile = project.getName() + "_" + version + ".json";
        File parentDir = new File(docDir + "/" + projectService.getRealPathByDocumentId(doc.getParentId()));
        File current = new File(parentDir, destFile);
        project.setJson(FileUtils.readFileToString(current, Constants.CHARACTER));
        result.setData(project);
        result.setMessage("工程获取成功");

        return result;
    }

    @RequiresPermissions("project:recoverToVersion")
    @RequestMapping(value = "/{projectId}/recover/{version}", method = RequestMethod.PUT)
//    @ApiOperation(value = "恢复到指定版本")
    public Result recover(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                          /*@ApiParam(value = "工程ID", required = true, name = "projectId")*/ @PathVariable(name = "projectId") Long projectId,
                          /*@ApiParam(value = "版本号", required = true, name = "version")*/ @PathVariable(name = "version") String version,
                          HttpServletRequest request) throws Exception {

        Result result = new Result();
        TokenModel tokenModel = tokenManager.getPermissions(accessToken);

        Project project = projectService.findOne(projectId);
        if (null == project) {
            result.setMessage("工程不存在或已被删除");
            result.setStatus(Result.Status.FAIL);
            return result;
        }
        Document doc = projectService.findDocumentByProjectId(projectId);
        String docDir = RedisUtils.get(com.tipdm.framework.dmserver.utils.Constants.DOCUMENT_DIR, String.class) + "/" + tokenModel.getUsername();
        //获取历史版本信息
        Map<String, Map<String, String>> versions = (Map<String, Map<String, String>>) RedisUtils.getFromMap(com.tipdm.framework.dmserver.utils.Constants.PROJECT_HISTORY_VERSION, projectId.toString());
        Map<String, String> verMap = versions.get(version);
        if (null == verMap) {
            result.setMessage("未能找到对应的版本文件");
            result.setStatus(Result.Status.FAIL);
            return result;
        }
        String destFile = project.getName() + "_" + version + ".json";
        File parentDir = new File(docDir + "/" + projectService.getRealPathByDocumentId(doc.getParentId()));
        File current = new File(parentDir, destFile);
        File file = new File(parentDir, project.getName() + ".json");
        FileKit.copyFile(current, file, true);
        project.setJson(FileUtils.readFileToString(current, Constants.CHARACTER));
        result.setData(project);
        result.setMessage("成功恢复到指定版本");
        return result;
    }

    @RequiresPermissions("project:deleteVersion")
    @RequestMapping(value = "/{projectId}/{version}", method = RequestMethod.DELETE)
//    @ApiOperation(value = "删除历史版本")
    public Result deleteByVersion(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                                  /*@ApiParam(value = "工程ID", required = true, name = "projectId")*/ @PathVariable(name = "projectId") Long projectId,
                                  /*@ApiParam(value = "版本号", required = true, name = "version")*/ @PathVariable(name = "version") String version,
                                  HttpServletRequest request) throws Exception {

        Result result = new Result();
        TokenModel tokenModel = tokenManager.getPermissions(accessToken);

        Project project = projectService.findOne(projectId);
        if (null == project) {
            result.setMessage("工程不存在或已被删除");
            result.setStatus(Result.Status.FAIL);
            return result;
        }
        Document doc = projectService.findDocumentByProjectId(projectId);
        String docDir = RedisUtils.get(com.tipdm.framework.dmserver.utils.Constants.DOCUMENT_DIR, String.class) + "/" + tokenModel.getUsername();

        //获取历史版本信息
        Map<String, Map<String, String>> versions = (Map<String, Map<String, String>>) RedisUtils.getFromMap(com.tipdm.framework.dmserver.utils.Constants.PROJECT_HISTORY_VERSION, projectId.toString());
        Map<String, String> verMap = versions.get(version);
        if (null == verMap) {
            result.setMessage("未能找到对应的版本文件");
            result.setStatus(Result.Status.FAIL);
            return result;
        }
        String destFile = project.getName() + "_" + version + ".json";
        File parentDir = new File(docDir + "/" + projectService.getRealPathByDocumentId(doc.getParentId()));
        File file = new File(parentDir, destFile);
        file.delete();
        versions.remove(version);
        RedisUtils.putToMap(com.tipdm.framework.dmserver.utils.Constants.PROJECT_HISTORY_VERSION, projectId.toString(), versions);
        result.setData(versions);
        result.setMessage("成功删除历史版本");
        return result;
    }

    @RequiresPermissions("project:delete")
    @RequestMapping(value = "/{projectId}", method = RequestMethod.DELETE)
//    @ApiOperation(value = "删除数据挖掘工程")
    public Result delete(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                         /*@ApiParam(value = "工程ID", required = true, name = "projectId")*/ @PathVariable(name = "projectId") Long projectId,
                         HttpServletRequest request) throws Exception {
        Result result = new Result();
        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        String docDir = RedisUtils.get(com.tipdm.framework.dmserver.utils.Constants.DOCUMENT_DIR, String.class) + "/" + tokenModel.getUsername();

        projectService.deleteProject(projectId, docDir);
        result.setMessage("工程删除成功");
        return result;
    }

    @RequiresPermissions("project:syncProject")
    @RequestMapping(value = "/{projectId}", method = RequestMethod.POST, consumes = {"multipart/form-data"})
//    @ApiOperation(value = "同步工程内容", notes = "客户端可通过定时调用此接口来同步工程内容")
    public Result syncProject(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                              /*@ApiParam(value = "工程Id", required = true, name = "projectId")*/ @PathVariable(name = "projectId") Long projectId,
                              /*@ApiParam(value = "工程信息", required = true, name = "content")*/ @RequestPart(required = false, name = "content") String content,
                              MultipartHttpServletRequest request) throws Exception {

        if (StringKit.isNotBlank(request.getParameter("content"))) {
            content = request.getParameter("content");
        } else {
            throw new MissingServletRequestPartException("Required request part 'content' is not present");
        }

        Result result = new Result();
        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        Project project = projectService.findOne(projectId);
        if (project == null || (!tokenModel.getUsername().equals(project.getCreatorName()))) {
            throw new IllegalOperationException("当前用户跟挖掘工程的创建者不一致");
        }
        project.setLastOpenTime(Calendar.getInstance().getTime());
        Document doc = projectService.findDocumentByProjectId(projectId);
        String docDir = RedisUtils.get(com.tipdm.framework.dmserver.utils.Constants.DOCUMENT_DIR, String.class) + "/" + tokenModel.getUsername();

        File parentDir = new File(docDir + "/" + projectService.getRealPathByDocumentId(doc.getParentId()));
        File file = new File(parentDir, project.getName() + ".json");

        FileUtils.write(file, content, Constants.CHARACTER, false);

        result.setMessage("工程内容更新成功");
        return result;
    }

    @RequiresPermissions("project:apply")
    @RequestMapping(value = "/execute/{projectId}/apply", method = RequestMethod.POST)
//    @ApiOperation(value = "申请执行流程", notes = "申请获准后返回executionId")
    public Result apply(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                        /*@ApiParam(value = "工程ID", required = true, name = "projectId")*/ @PathVariable(name = "projectId") Long projectId,
                        HttpServletRequest request) throws Exception {
        Result result = new Result();
        result.setData(StringKit.getBase64FromUUID());
        return result;
    }

    @RequiresPermissions("project:execute")
    @RequestMapping(value = "/execute/{projectId}", method = RequestMethod.GET)
//    @ApiOperation(value = "执行流程")
    public Result execute(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                          /*@ApiParam(value = "工程ID", required = true, name = "projectId")*/ @PathVariable(name = "projectId") Long projectId,
                          /*@ApiParam(value = "executionId", required = true)*/ @RequestParam("executionId") String executionId,
                          HttpServletRequest request) throws Exception {
        Result result = new Result();

        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        Project project = projectService.findOne(projectId);
        project.setLastOpenTime(Calendar.getInstance().getTime());
        Document doc = projectService.findDocumentByProjectId(projectId);
        String docDir = RedisUtils.get(com.tipdm.framework.dmserver.utils.Constants.DOCUMENT_DIR, String.class) + "/" + tokenModel.getUsername();
        logger.info("流程存放目录: " + docDir);
        File parentDir = new File(docDir + "/" + projectService.getRealPathByDocumentId(doc.getParentId()));
        logger.info("parentDir: " + parentDir.getAbsolutePath());
        File file = new File(parentDir, project.getName() + ".json");
        logger.info("json file: " + file.getAbsolutePath());
        String content = FileUtils.readFileToString(file, Constants.CHARACTER);
        WorkFlow workFlow = new WorkFlow(executionId, tokenModel.getUsername(), content);
        String workFlowId = workFlowScheduler.execute(workFlow);

        result.setMessage("流程调度成功");
        result.setData(workFlowId);

        return result;
    }

    @RequiresPermissions("project:shutdown")
    @RequestMapping(value = "/shutdown/{workFlowId}", method = RequestMethod.GET)
//    @ApiOperation(value = "停止流程调度", notes = "workFlowId为流程调度任务提交成功后返回的唯一标识")
    public Result shutdown(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                           /*@ApiParam(value = "工作流ID", required = true, name = "workFlowId")*/ @PathVariable(name = "workFlowId") String workFlowId,
                           HttpServletRequest request) throws Exception {
        Result result = new Result();

        TokenModel tokenModel = tokenManager.getPermissions(accessToken);

        boolean isStop = workFlowScheduler.shutdownWorkFlow(workFlowId);
        result.setMessage(isStop ? "流程停止成功" : "流程停止失败");
        result.setData(isStop);
        return result;
    }

    @RequiresPermissions("project:executeNode")
    @RequestMapping(value = "/execute/{projectId}/only/{componentId}", method = RequestMethod.GET)
//    @ApiOperation(value = "执行该节点")
    public Result execute(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                          /*@ApiParam(value = "工程ID", required = true, name = "projectId")*/ @PathVariable(name = "projectId") Long projectId,
                          /*@ApiParam(value = "组件ID", required = true, name = "componentId")*/ @PathVariable(name = "componentId") Long componentId,
                          /*@ApiParam(value = "executionId", required = true) @RequestParam("executionId")*/ String executionId,
                          HttpServletRequest request) throws Exception {
        Result result = new Result();

        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        Project project = projectService.findOne(projectId);
        project.setLastOpenTime(Calendar.getInstance().getTime());
        Document doc = projectService.findDocumentByProjectId(projectId);
        String docDir = RedisUtils.get(com.tipdm.framework.dmserver.utils.Constants.DOCUMENT_DIR, String.class) + "/" + tokenModel.getUsername();
        File parentDir = new File(docDir + "/" + projectService.getRealPathByDocumentId(doc.getParentId()));
        File file = new File(parentDir, project.getName() + ".json");

        String content = FileUtils.readFileToString(file, Constants.CHARACTER);
        WorkFlow workFlow = new WorkFlow(executionId, tokenModel.getUsername(), content, "" + componentId, Command.ONLY);
        String workFlowId = workFlowScheduler.execute(workFlow);

        result.setMessage("流程调度成功");
        result.setData(workFlowId);

        return result;
    }

    @RequiresPermissions("project:startAt")
    @RequestMapping(value = "/execute/{projectId}/startAt/{componentId}", method = RequestMethod.GET)
//    @ApiOperation(value = "从此节点执行")
    public Result startAt(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                          /*@ApiParam(value = "工程ID", required = true, name = "projectId")*/ @PathVariable(name = "projectId") Long projectId,
                          /*@ApiParam(value = "组件ID", required = true, name = "componentId")*/ @PathVariable(name = "componentId") Long componentId,
                          /*@ApiParam(value = "executionId", required = true)*/ @RequestParam("executionId") String executionId,
                          HttpServletRequest request) throws Exception {
        Result result = new Result();

        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        Project project = projectService.findOne(projectId);
        project.setLastOpenTime(Calendar.getInstance().getTime());
        Document doc = projectService.findDocumentByProjectId(projectId);
        String docDir = RedisUtils.get(com.tipdm.framework.dmserver.utils.Constants.DOCUMENT_DIR, String.class) + "/" + tokenModel.getUsername();
        File parentDir = new File(docDir + "/" + projectService.getRealPathByDocumentId(doc.getParentId()));
        File file = new File(parentDir, project.getName() + ".json");

        String content = FileUtils.readFileToString(file, Constants.CHARACTER);
        WorkFlow workFlow = new WorkFlow(executionId, tokenModel.getUsername(), content, "" + componentId, Command.STARTAT);
        String workFlowId = workFlowScheduler.execute(workFlow);

        result.setMessage("流程调度成功");
        result.setData(workFlowId);

        return result;
    }

    @RequiresPermissions("project:endAt")
    @RequestMapping(value = "/execute/{projectId}/endAt/{componentId}", method = RequestMethod.GET)
//    @ApiOperation(value = "执行到此处")
    public Result endAt(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                        /*@ApiParam(value = "工程ID", required = true, name = "projectId")*/ @PathVariable(name = "projectId") Long projectId,
                        /*@ApiParam(value = "组件ID", required = true, name = "componentId")*/ @PathVariable(name = "componentId") Long componentId,
                        /*@ApiParam(value = "executionId", required = true)*/ @RequestParam("executionId") String executionId,
                        HttpServletRequest request) throws Exception {
        Result result = new Result();

        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        Project project = projectService.findOne(projectId);
        project.setLastOpenTime(Calendar.getInstance().getTime());
        Document doc = projectService.findDocumentByProjectId(projectId);
        String docDir = RedisUtils.get(com.tipdm.framework.dmserver.utils.Constants.DOCUMENT_DIR, String.class) + "/" + tokenModel.getUsername();
        File parentDir = new File(docDir + "/" + projectService.getRealPathByDocumentId(doc.getParentId()));
        File file = new File(parentDir, project.getName() + ".json");

        String content = FileUtils.readFileToString(file, Constants.CHARACTER);
        WorkFlow workFlow = new WorkFlow(executionId, tokenModel.getUsername(), content, componentId + "", Command.ENDAT);
        String workFlowId = workFlowScheduler.execute(workFlow);

        result.setMessage("流程调度成功");
        result.setData(workFlowId);

        return result;
    }

    @RequiresPermissions("project:result")
    @RequestMapping(value = "/node/{id}/result", method = RequestMethod.GET)
//    @ApiOperation(value = "查看组件的运行结果", notes = "运行结果的过期时间为30分钟")
    public Result getResult(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                            /*@ApiParam(value = "组件的客户端ID", required = true, name = "id")*/ @PathVariable(name = "id") Long id,
                            HttpServletRequest request) throws IOException {

        Result result = new Result();
        String reportDir = RedisUtils.get(com.tipdm.framework.dmserver.utils.Constants.REPORT_DIR, String.class);
        File tmp = new File(reportDir, id + ".html");
        if (!tmp.exists()) {
            result.setData("");
            return result;
        }
        String contextPath = request.getServletContext().getRealPath("/");
        File dir = new File(contextPath, "report");
        if(!dir.exists()){
            dir.mkdirs();
        }
        File destination = new File(dir, id + ".html");
        if(!tmp.getCanonicalPath().equals(destination.getCanonicalPath())) {
            //拷贝到report目录
            FileKit.copyFile(tmp, destination);
        }

        boolean exists = RedisUtils.exists("edv_" + id);
        //加时间戳
        Long timestamp = Calendar.getInstance().getTimeInMillis();
        String url = "";
        if(exists){
            url = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/report/overview/index.html?t=" + timestamp + "&id=" + id;
        } else {
            url = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/report/" + id + ".html?t=" + timestamp;
        }
        result.setData(url);
        return result;
    }

    @RequiresPermissions("project:outputData")
    @RequestMapping(value = "/{projectId}/{outputId}/data", method = RequestMethod.GET)
//    @ApiOperation(value = "查看数据", notes = "查看组件运行成功后的结果数据,只显示前100条记录")
    public Result showData(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                           /*@ApiParam(value = "工程Id", required = true)*/ @PathVariable(name = "projectId") Long id,
                           /*@ApiParam(value = "输出节点Id", required = true)*/ @PathVariable(name = "outputId") String outputId,
                           /*@ApiParam(value = "页码", required = true)*/ @RequestParam(value = "pageNumber", defaultValue = "1") int pageNumber,
                           /*@ApiParam(value = "页大小", required = true)*/ @RequestParam(value = "pageSize", defaultValue = "100") int pageSize,
                           HttpServletRequest request) {
        Result result = new Result();
        Page<Map<String, Object>> data = tableService.findDataByOutputId(id, outputId, buildPageRequest(pageNumber, pageSize));
        result.setData(data);
        return result;
    }

    @RequiresPermissions("project:log")
    @RequestMapping(value = "/node/{id}/log", method = RequestMethod.GET)
//    @ApiOperation(value = "查看组件的运行日志")
    public Result showLog(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                          /*@ApiParam(value = "组件的客户端ID", required = true, name = "id")*/ @PathVariable(name = "id") Long id) throws IOException {

        Result result = new Result();
        String log_home = PropertiesUtil.getValue("sysconfig/system.properties", "LOG_HOME");

        String content = FileUtils.readFileToString(new File(log_home + "/" + id + ".log"), Constants.CHARACTER);
        result.setData(content);
        return result;
    }

    @RequiresPermissions("project:viewsource")
    @RequestMapping(value = "/{projectId}/{nodeId}/viewsource", method = RequestMethod.GET)
//    @ApiOperation(value = "查看算法源码")
    public Result viewSource(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                             /*@ApiParam(value = "工程ID", required = true, name = "projectId")*/ @PathVariable(name = "projectId") Long projectId,
                             /*@ApiParam(value = "组件的客户端ID", required = true, name = "nodeId")*/ @PathVariable(name = "nodeId") String nodeId,
                             HttpServletRequest request) throws IOException, ClassNotFoundException {

        Result result = new Result();

        if ("DBUTILS".equals(nodeId)) {
            String scriptPath = this.getClass().getClassLoader().getResource("com/tipdm/framework/dmserver/core/algo/unparallel/script/r/dbUtils.R").getPath();
            String content = FileUtils.readFileToString(new File(scriptPath), Constants.CHARACTER);
            result.setData(content);
            return result;
        }

        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        Project project = projectService.findOne(projectId);
        project.setLastOpenTime(Calendar.getInstance().getTime());
        Document doc = projectService.findDocumentByProjectId(projectId);
        String docDir = RedisUtils.get(com.tipdm.framework.dmserver.utils.Constants.DOCUMENT_DIR, String.class) + "/" + tokenModel.getUsername();

        File parentDir = new File(docDir + "/" + projectService.getRealPathByDocumentId(doc.getParentId()));
        File file = new File(parentDir, project.getName() + ".json");

        String content = FileUtils.readFileToString(file, Constants.CHARACTER);

        if (StringKit.isBlank(content)) {
            result.setMessage("流程未保存， 请保存后再查看");
            result.setStatus(Result.Status.FAIL);
            return result;
        }

        JSONObject jsonObject = JSONObject.parseObject(content);
        List<Node> nodes = JSON.parseArray(jsonObject.getString("nodes"), Node.class);
        Optional<Node> optional = nodes.stream().filter(x -> nodeId.equals(x.getId())).findFirst();

        if (!optional.isPresent()) {
            result.setMessage("无法在流程中获取节点对应的信息，请保存后再试");
            result.setStatus(Result.Status.FAIL);
            return result;
        }

        Node node = optional.get();
        Component component = componentService.findOne(node.getServerId());

        if (component.getScript().size() > 0 && component.getScript().containsKey(Step.MAIN)) {
            result.setData(component.getScript().get(Step.MAIN));
        } else {
            Class clazz = Class.forName(node.getTargetAlgorithm());
            String algorithm = clazz.getSimpleName();
            String scriptPath = clazz.getResource("script/" + algorithm + ".R").getPath();

            content = FileUtils.readFileToString(new File(scriptPath), Constants.CHARACTER);
            result.setData(content);
        }
        return result;
    }


    @RequiresPermissions("project:getOwnedModels")
    @RequestMapping(value = "/owned/models", method = RequestMethod.GET)
//    @ApiOperation(value = "获取个人的模型列表")
    public Result getOwnedModels(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken) {
        Result result = new Result();
        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        Map<String, Object> params = new HashMap<>();
        params.put("creatorId", tokenModel.getUserId());
        Page<Model> page = modelService.findOwnModels(params, buildPageRequest(1, Integer.MAX_VALUE));
        List<Model> models = formatter(page.getContent());
        result.setData(models);
        return result;
    }

    private List<Model> formatter(List<Model> models) {
        return models.stream().map(x -> {
            Model model = new Model();
            model.setId(x.getId());
            model.setModelName(x.getModelName());
            model.setCreateTime(null);
            model.setDeployTime(null);
            model.setVersion(null);
            return model;
        }).collect(Collectors.toList());
    }

    @RequiresPermissions("project:saveAsTemplate")
    @RequestMapping(value = "/{projectId}/saveAsTemplate", method = RequestMethod.PUT)
//    @ApiOperation(value = "另存为模板")
    public Result saveAsTemplate(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                                 /*@ApiParam(value = "工程ID", required = true, name = "projectId")*/ @PathVariable(name = "projectId") Long projectId,
                                 /*@ApiParam(value = "标签", required = false, allowMultiple = true)*/ @RequestParam(required = false) String[] tags
    ) {
        Result result = new Result();
        Long templateId = projectService.saveAsTemplate(projectId, tags);
        result.setData(templateId);
        return result;
    }

    @RequiresPermissions("project:saveAs")
    @RequestMapping(value = "/{projectId}/saveAs/{docId}", method = RequestMethod.POST)
//    @ApiOperation(value = "另存为", notes = "将现有流程另存为新的流程")
    public Result saveAs(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                         /*@ApiParam(value = "工程Id", required = true, name = "projectId")*/ @PathVariable(name = "projectId") Long projectId,
                         /*@ApiParam(value = "保存的目录Id", required = true, name = "docId")*/ @PathVariable(name = "docId") Long docId,
                         /*@ApiParam(value = "工程名称", required = true, name = "asName")*/ @RequestParam(name = "asName") String projectName,
                         HttpServletRequest request) throws Exception {
        Result result = new Result();

        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        Long id = projectService.saveAs(projectId, tokenModel, docId, projectName);
        if (id == -1L) {
            result.setStatus(Result.Status.FAIL);
            result.setMessage("复制流程出错，请确认流程文件是否存在");
            return result;
        }
        result.setData(id);
        return result;
    }

    @RequiresPermissions("project:clone")
    @RequestMapping(value = "/{projectId}/clone/{docId}", method = RequestMethod.POST)
//    @ApiOperation(value = "从模板克隆", notes = "从已有的工程模板克隆新的挖掘工程，拷贝过程中会将流程中输入源对应的数据源共享给当前用户")
    public Result clone(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                        /*@ApiParam(value = "模版工程的projectId", required = true, name = "projectId")*/ @PathVariable(name = "projectId") Long projectId,
                        /*@ApiParam(value = "保存的目录Id", required = true, name = "docId")*/ @PathVariable(name = "docId") Long docId,
                        /*@ApiParam(value = "工程名称", required = true, name = "newName")*/ @RequestParam(name = "newName") String projectName,
                        HttpServletRequest request) throws Exception {
        Result result = new Result();

        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        logger.info("从模版创建,模版ID:{}, 创建时间:{}", projectId, DateFormatUtils.format(Calendar.getInstance(), "yyyy-MM-dd HH:mm:ss"));
        Long id = projectService.cloneProject(projectId, tokenModel, docId, projectName);
        if (id == -1L) {
            result.setStatus(Result.Status.FAIL);
            result.setMessage("模版不存在或已被删除, 请刷新页面重试");
            return result;
        }
        result.setData(id);
        return result;
    }

    @PatchMapping(value = "/modify/{projectId}/desc")
//    @ApiOperation(value = "更新描述", notes = "更新挖掘工程的描述信息")
    public Result modifyDescription(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader("accessToken") String accessToken,
                                    /*@ApiParam(value = "工程ID", required = true, name = "projectId")*/ @PathVariable(name = "projectId") Long projectId,
                                    /*@ApiParam(value = "工程描述", required = true)*/ @RequestParam("desc") String desc) {
        Result result = new Result();
        projectService.modifyDesc(projectId, desc);
        return result;
    }

}
