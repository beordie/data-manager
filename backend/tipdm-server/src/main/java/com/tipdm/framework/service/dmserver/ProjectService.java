package com.tipdm.framework.service.dmserver;

import com.tipdm.framework.common.token.model.TokenModel;
import com.tipdm.framework.model.dmserver.Document;
import com.tipdm.framework.model.dmserver.Project;
import com.tipdm.framework.service.BaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Created by TipDM on 2016/12/15.
 * E-mail:devp@tipdm.com
 */
public interface ProjectService extends BaseService<Project, Long> {


    /**
     * 条件查找
     * @param params
     * @param pageable
     * @return
     */
    public Page<Project> findTableByCondition(Map<String, Object> params, Pageable pageable);

    /**
     * 添加文档
     * @param document
     * @return
     */
    public Long saveDocument(Document document);

    /**
     * 删除文档目录
     * @param documentId
     * @param contextPath
     */
    public void deleteDocument(Long documentId, String contextPath) throws IOException;

    /**
     * 删除工程，同时删除对应的工程文件
     * @param projectId
     * @param contextPath
     */
    public void deleteProject(Long projectId, String contextPath);

    /**
     * 获取文档路径
     * @param documentId
     * @return
     */
    public String getRealPathByDocumentId(Long documentId);

    public List<Document> getChild(Long documentId, Long creatorId);

    public Document findDocumentByProjectId(Long projectId);

    /**
     * 将工程另存为模板
     * @param projectId
     * @return
     */
    public Long saveAsTemplate(Long projectId, String[] tags);

    public Long cloneProject(Long projectId, TokenModel tokenModel, Long parentId, String projectName) throws Exception;

    /**
     *
     * @param projectId
     * @param tokenModel
     * @param parentId
     * @param projectName
     * @return
     * @throws Exception
     */
    public Long saveAs(Long projectId, TokenModel tokenModel, Long parentId, String projectName) throws Exception;

    public Document findProjectId(Long projectId);

    public String generateSharedUrl(String creator, Long projectId);

    public void modifyDesc(Long projectId, String desc) throws NoSuchElementException;

    public File export(Long[] ids) throws IOException;

    public void save(TokenModel tokenModel, Long parentId, boolean supportParalleled, File ... files) throws IOException;
}
