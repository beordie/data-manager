package com.tipdm.framework.service.dmserver;

import com.tipdm.framework.model.dmserver.Model;
import com.tipdm.framework.model.dmserver.ModelTree;
import com.tipdm.framework.service.BaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by zhoulong on 2017/2/22.
 * E-mail:zhoulong8513@gmail.com
 * 可视化Service
 */
public interface ModelService extends BaseService<Model, Long> {

    /**
     * 查询用户的模型列表
     * @param params
     * @param pageable
     * @return
     */
    Page<Model> findOwnModels(Map<String, Object> params, Pageable pageable);


    /**
     * 部署模型
     * @param model
     * @return id
     */
    public Long deployModel(Model model) throws IOException;

    /**
     * 重新部署模型
     * @param model
     * @return id
     */
    public Long redeployModel(Model model, Integer version) throws IOException;

    /**
     *
     * @param model
     */
    public void updateModel(Model model);

    /**
     * 添加节点
     * @param node
     * @return
     */
    public Long createNode(ModelTree node);

    /**
     * 删除节点
     * @param nodeId
     * @param contextPath
     */
    public void deleteNode(Long nodeId, String contextPath) throws IOException;

    /**
     * 删除模型
     * @param modelId
     * @param contextPath
     */
    public void deleteModel(Long modelId, String contextPath);

    public void deleteModel(Long modelId, Long creatorId) throws IllegalAccessException;


    /**
     * 获取文档路径
     * @param nodeId
     * @return
     */
    public String getRealPathByNodeId(Long nodeId);

    public List<ModelTree> getChild(Long nodeId, Long creatorId);

    public ModelTree findNodeByModelId(Long modelId);

}
