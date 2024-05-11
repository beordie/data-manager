package com.tipdm.framework.controller.dmserver;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.tipdm.framework.common.Constants;
import com.tipdm.framework.common.controller.Result;
import com.tipdm.framework.common.controller.base.BaseController;
import com.tipdm.framework.common.token.impl.RedisTokenManager;
import com.tipdm.framework.common.token.model.TokenModel;
import com.tipdm.framework.common.utils.PropertiesUtil;
import com.tipdm.framework.common.utils.RedisUtils;
import com.tipdm.framework.common.utils.StringKit;
import com.tipdm.framework.controller.dmserver.dto.UploadInfo;
import com.tipdm.framework.model.dmserver.DataTable;
import com.tipdm.framework.service.dmserver.DataTableService;
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
//import io.swagger.annotations.ApiParam;
import org.apache.commons.io.FileUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.*;
import java.util.*;

/**
 * Created by TipDM on 2016/12/10.
 * E-mail:devp@tipdm.com
 */
@SuppressWarnings("all")
@RestController
@PropertySource(value = "classpath:sysconfig/system.properties")
@RequestMapping("/api/file")
//@Api(value = "/api/file", tags = "文件资源管理", position = 3, description = "上传数据文件，自定义算法jar包")
public class FileManagerController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(FileManagerController.class);

    private static Map<String, String> delimiters = new LinkedHashMap<>();

    @Autowired
    private DataTableService tableService;

    @Autowired
    private RedisTokenManager tokenManager;

    @Value("${file.upload.dir}")
    private String uploadDir;

    static{
        delimiters = PropertiesUtil.getProperties("sysconfig/delimiter-mapping.properties");
    }
    /**
     * 先判断系统存在这个文件不，
     * 系统不存在这个文件，返回false
     *
     * @param fileMD5 要上传的目标文件的MD5值
     * @return
     */
    @RequiresPermissions("file:exists")
    @RequestMapping(value = "/existsMD5", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
//    @ApiOperation(value = "文件MD5检测", notes = "上传前检测文件在服务端是否有匹配的记录")
    public Result existsMD5(/*@ApiParam(value = "用户访问令牌")*/ @RequestHeader("accessToken") String accessToken,
                            /*@ApiParam(value = "上传凭证Id")*/ @RequestParam("uploadId") String uploadId,
                            /*@ApiParam(value = "数据文件的MD5值")*/ @RequestParam("fileMD5") String fileMD5) {

        Result result = new Result();
        UploadInfo uploadInfo = (UploadInfo) RedisUtils.getFromMap(com.tipdm.framework.dmserver.utils.Constants.FILE_UPLOAD_ID, uploadId);

        if (uploadInfo == null) {
            throw new IllegalArgumentException("上传凭证无效");
        }
        if (uploadInfo.getCategory() == UploadInfo.Category.FLAT) {
            //数据文件
            boolean isExists = false;
            result.setData(isExists);
        } else if (uploadInfo.getCategory() == UploadInfo.Category.UDC) {
            //自定义算法
            throw new UnsupportedOperationException("sorry, 自定义算法模块不支持此操作");
        }
        return result;
    }


    /**
     * 数据文件上传，支持分片
     *
     * @param file      上传的文件
     *                  //     * @param fileMD5   上传文件的MD5值
     * @param delimiter
     * @param encoding
     * @param chunk     当前分片
     * @param chunks    总分片数
     * @param request
     * @return
     * @throws JsonGenerationException
     * @throws JsonMappingException
     * @throws IOException
     */
    @RequiresPermissions("file:uploadFlat")
    @RequestMapping(value = "/flat/upload", method = RequestMethod.POST, consumes = {"multipart/form-data"})
//    @ApiOperation(value = "上传数据文件", notes = "上传数据文件，支持分片上传,文件上传成功后自动销毁uploadId, 失败后可在列表页面获取uploadId再次上传")
    public Result uploadFlat(/*@ApiParam(value = "用户访问令牌", required = true)*/ @RequestHeader(value = "accessToken") String accessToken,
                             /*@ApiParam(value = "上传凭证Id", required = true)*/ @RequestParam(name = "uploadId") String uploadId,
                             /*@ApiParam(value = "数据文件", required = true)*/ @RequestBody MultipartFile file,
                             /*@ApiParam(value = "列分隔符", required = true)*/ @RequestPart(required = false, name = "delimiter") String delimiter,
                             /*@ApiParam(value = "文件编码", required = true)*/ @RequestPart(required = false, name = "encoding") String encoding,
                             /*@ApiParam(value = "当前分片")*/ @RequestPart(required = false, name = "chunk") Integer chunk,
                             /*@ApiParam(value = "总分片数")*/ @RequestPart(required = false, name = "chunks") Integer chunks,
                             MultipartHttpServletRequest request) {

        Result result = new Result();
        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        DataTable dataTable;

        UploadInfo uploadInfo = (UploadInfo) RedisUtils.getFromMap(com.tipdm.framework.dmserver.utils.Constants.FILE_UPLOAD_ID, uploadId);
        if (uploadInfo == null || uploadInfo.getCategory() != UploadInfo.Category.FLAT) {
            throw new IllegalArgumentException("上传凭证无效");
        } else {
            dataTable = tableService.findOne(uploadInfo.getId());
            if (null == dataTable) {
                throw new IllegalArgumentException("上传凭证无效");
            }

        }

        if (StringKit.isNotBlank(request.getParameter("delimiter"))) {
            delimiter = request.getParameter("delimiter");

            if(delimiters == null || !delimiters.containsKey(delimiter)){
                if (StringKit.startsWith(delimiter, "{{") && StringKit.endsWith(delimiter, "}}")) {
                    delimiter = StringKit.substringBetween(delimiter, "{{", "}}");
                } else {
                    throw new IllegalArgumentException("未能在分隔符配置文件中找到对应的符号，请前往sysconfig/delimiter-mapping.properties检查！");
                }
            }
            delimiter = delimiters.get(delimiter);
        }

        if (StringKit.isNotBlank(request.getParameter("encoding"))) {
            encoding = request.getParameter("encoding");
        }

        if (StringKit.isNotBlank(request.getParameter("chunk"))) {
            chunk = Integer.parseInt(request.getParameter("chunk"));
        }

        if (StringKit.isNotBlank(request.getParameter("chunks"))) {
            chunks = Integer.parseInt(request.getParameter("chunks"));
        }

        try {
            if (null != file) {
                String fileName = file.getOriginalFilename();
                File parentFileDir = new File(tokenModel.getUsername());
                if (!parentFileDir.exists()) {
                    parentFileDir.mkdirs();
                }

                //判断上传的文件是否被分片
                if (null == chunks && null == chunk) {
                    File destFile = new File(parentFileDir, fileName);
                    if (destFile.exists()) {
                        boolean s = destFile.delete();
                    }

                    try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), encoding))) {
                        List<String> lines = new ArrayList<String>();
                        String line = null;
                        while ((line = br.readLine()) != null) {
                            lines.add(line);
                        }
                        FileUtils.writeLines(destFile, Constants.CHARACTER, lines);
                        encoding = Constants.CHARACTER;
                    }
                    tableService.syncTable(dataTable, destFile, delimiter, encoding);
                    result.setMessage("文件上传成功");
                    result.setStatus(Result.Status.SUCCESS);
                    RedisUtils.removeFromMap(uploadInfo.getUploadId(), uploadInfo.getId().toString());
                    return result;
                } else {

                    File f = new File(parentFileDir, fileName + "_" + chunk + ".part");
                    file.transferTo(f);
                    f.createNewFile();
                    // 是否全部上传完成
                    // 所有分片都存在才说明整个文件上传完成
                    boolean isDone = true;
                    for (int i = 0; i < chunks; i++) {
                        File partFile = new File(parentFileDir, fileName + "_" + i + ".part");
                        if (!partFile.exists()) {
                            isDone = false;
                        }
                    }
                    // 所有分片文件都上传完成
                    // 将所有分片文件合并到一个文件中
                    if (isDone) {
                        synchronized (this) {
                            File destTempFile = new File(parentFileDir, fileName + "_tmp");
                            for (int i = 0; i < chunks; i++) {
                                File partFile = new File(parentFileDir, fileName + "_" + i + ".part");
                                FileOutputStream destTempfos = new FileOutputStream(destTempFile, true);
                                FileUtils.copyFile(partFile, destTempfos);
                                destTempfos.close();
                            }
                            File destFile = new File(uploadDir, fileName);
                            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(destTempFile), encoding))) {
                                List<String> lines = new ArrayList<String>();
                                String line = null;
                                while ((line = br.readLine()) != null) {
                                    lines.add(line);
                                }
                                FileUtils.writeLines(destFile, Constants.CHARACTER, lines);
                                encoding = Constants.CHARACTER;
                            }
                            tableService.syncTable(dataTable, destFile, delimiter, encoding);
                            FileUtils.deleteDirectory(parentFileDir);
                            result.setData("文件上传成功");
                            RedisUtils.removeFromMap(uploadInfo.getUploadId(), uploadInfo.getId().toString());
                        }
                    }
                }
            } else {
                result.setMessage("请选择文件");
                result.setData(uploadId);
                result.setStatus(Result.Status.FAIL);
            }
        }catch (Exception ex){
            result.setMessage(ex.getMessage());
            result.setData(uploadId);
            result.setStatus(Result.Status.FAIL);
            try {
                tableService.deleteTable(tokenModel.getUserId(), uploadInfo.getId());
            } catch (IllegalAccessException e) {
                logger.error("数据表删除失败，{}", e.getCause());
            }
        }
        return result;
    }

}
