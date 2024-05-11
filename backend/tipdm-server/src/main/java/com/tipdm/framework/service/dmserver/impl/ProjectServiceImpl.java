package com.tipdm.framework.service.dmserver.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.ValueFilter;
import com.tipdm.framework.common.token.model.TokenModel;
import com.tipdm.framework.common.utils.FileKit;
import com.tipdm.framework.common.utils.RedisUtils;
import com.tipdm.framework.common.utils.StringKit;
import com.tipdm.framework.controller.dmserver.dto.ProjectShareInfo;
import com.tipdm.framework.dmserver.core.scheduling.model.IO;
import com.tipdm.framework.dmserver.core.scheduling.model.Link;
import com.tipdm.framework.dmserver.core.scheduling.model.Node;
import com.tipdm.framework.dmserver.exception.ElementNotFoundException;
import com.tipdm.framework.dmserver.exception.IllegalOperationException;
import com.tipdm.framework.dmserver.utils.Constants;
import com.tipdm.framework.dmserver.utils.GlobalSeqGenerator;
import com.tipdm.framework.model.dmserver.*;
import com.tipdm.framework.repository.dmserver.*;
import com.tipdm.framework.service.AbstractBaseServiceImpl;
import com.tipdm.framework.service.dmserver.ProjectService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by TipDM on 2016/12/15.
 * E-mail:devp@tipdm.com
 */
@SuppressWarnings("all")
@Transactional
@Service("projectService")
public class ProjectServiceImpl extends AbstractBaseServiceImpl<Project, Long> implements ProjectService {

    private final static Logger LOG = LoggerFactory.getLogger(ProjectServiceImpl.class);

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private DataTableRepository tableRepository;

    @Autowired
    private AudienceRepository audienceRepository;

    @Autowired
    private DataSchemaRepository dataSchemaRepository;

    @Override
    public List find(Long... ids) {
        return null;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Project> findTableByCondition(final Map<String, Object> params, final Pageable pageable) {

        Specification<Project> specification = new Specification<Project>() {

            @Override
            public Predicate toPredicate(Root<Project> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {

                Predicate predicates = null;

                String creatorName = (String) params.get("creatorName");

                if (StringKit.isNotBlank(creatorName)) {
                    Predicate condition = criteriaBuilder.equal(root.get("creatorName").as(String.class), creatorName);

                    if (null == predicates) {
                        predicates = criteriaBuilder.and(condition);
                    } else {
                        predicates = criteriaBuilder.and(predicates, condition);
                    }
                }

                String name = (String) params.get("name");

                if (StringKit.isNotBlank(creatorName)) {
                    Predicate condition = criteriaBuilder.like(root.get("name").as(String.class), "%" + creatorName + "%");

                    if (null == predicates) {
                        predicates = criteriaBuilder.and(condition);
                    } else {
                        predicates = criteriaBuilder.and(predicates, condition);
                    }
                }

                Date beginTime = null;
                Date endTime = null;
                try {
                    beginTime = (Date) params.get("beginTime");
                    endTime = (Date) params.get("endTime");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (beginTime != null && endTime != null) {
                    Predicate condition = criteriaBuilder.between(root.get("createTime").as(Date.class), beginTime, endTime);

                    if (null == predicates) {
                        predicates = criteriaBuilder.and(condition);
                    } else {
                        predicates = criteriaBuilder.and(predicates, condition);
                    }
                }

                return predicates;
            }
        };

        return projectRepository.findAll(specification, pageable);
    }

    @Transactional
    @Override
    public Long saveDocument(Document document) {

        List<Document> list = documentRepository.findByParentId(document.getParentId(), document.getCreatorId());
        Optional<Document> optional = list.stream().filter(x -> document.getName().equals(x.getName())).findFirst();
        if (optional.isPresent()) {
            throw new DataIntegrityViolationException("保存的位置已存在同名文件夹");
        }
        List<String> paths = new ArrayList<>();
        getVirtualPath(document.getParentId(), paths);
        document.setSequence(list.size());
        documentRepository.save(document);
        paths.add(document.getId().toString());
        paths.add(0, "0");
        document.setPath(StringKit.join(paths, "/") + "/");
        return document.getId();
    }

    @Transactional(readOnly = true)
    @Override
    public Document findProjectId(Long projectId) {
        Document doc = documentRepository.findByProjectId(projectId);
        return doc;
    }

    @Override
    public String generateSharedUrl(String creator, Long projectId) {
        Project project = projectRepository.findOne(projectId);

        if (null == project) {
            throw new ElementNotFoundException("挖掘工程不存在");
        }
        if (!creator.equals(project.getCreatorName())) {
            throw new IllegalOperationException("当前用户与工程的创建者不一致");
        }
        ProjectShareInfo shardInfo = new ProjectShareInfo(project.getCreatorName(), project.getId());
        return StringKit.toBase64String(shardInfo);
    }

    @Override
    public void deleteDocument(Long documentId, String contextPath) throws IOException {

        Document doc = documentRepository.findOne(documentId);
        File parentDir = new File(contextPath + "/" + getRealPathByDocumentId(doc.getParentId()));

        if (doc.getLeaf()) {
            File file = new File(parentDir, doc.getName() + ".json");
            documentRepository.delete(documentId);
            file.delete();
        } else {
            File documentDir = new File(parentDir, doc.getName());
            documentRepository.deleteByPath(doc.getPath() + "%");
            FileUtils.deleteDirectory(documentDir);
        }
    }

    @Override
    public void deleteProject(Long projectId, String contextPath) {

        //删除工程前检测是否是模版工程
        Template template = templateRepository.findByProjectId(projectId);
        if (null != template) {
            throw new DataIntegrityViolationException("删除失败，请先从模版列表移除对应的工程再继续该操作");
        }
        Document doc = findDocumentByProjectId(projectId);
        //删除流程文件
        File parentDir = new File(contextPath + "/" + getRealPathByDocumentId(doc.getParentId()));
        File file = new File(parentDir, doc.getProject().getName() + ".json");
        documentRepository.delete(doc.getId());
        file.delete();
        //从Redis移除信息
        RedisUtils.remove("workFlow-" + projectId);
    }

    @Transactional(readOnly = true)
    @Override
    public String getRealPathByDocumentId(Long documentId) {

        List<String> paths = new ArrayList<>();

        getPath(documentId, paths);

        return StringKit.join(paths, "/");
    }

    @Transactional(readOnly = true)
    @Override
    public List<Document> getChild(Long documentId, Long creatorId) {
        return documentRepository.findByParentId(documentId, creatorId);
    }

    private void getPath(Long documentId, List<String> path) {

        Document doc = documentRepository.findOne(documentId);

        if (doc != null) {
            path.add(0, doc.getName());
            getPath(doc.getParentId(), path);
        }
    }

    private void getVirtualPath(Long documentId, List<String> path) {

        Document doc = documentRepository.findOne(documentId);

        if (doc != null) {
            path.add(0, doc.getId().toString());
            getVirtualPath(doc.getParentId(), path);
        }
    }


    @Transactional
    @Override
    public void save(Project project) {

        projectRepository.save(project);

        Document document = new Document();
        List<Document> list = documentRepository.findByParentId(project.getParentId(), project.getCreatorId());
        Optional<Document> optional = list.stream().filter(x -> project.getName().equals(x.getName())).findFirst();
        if (optional.isPresent()) {
            throw new DataIntegrityViolationException("保存的位置已存在同名工程");
        }
        document.setSequence(list.size());//排序
        document.setName(project.getName());
        document.setLeaf(Boolean.TRUE);
        document.setCreatorId(project.getCreatorId());
        document.setCreatorName(project.getCreatorName());
        document.setParentId(project.getParentId());
        document.setProject(project);

        documentRepository.save(document);
    }

    @Transactional(readOnly = true)
    @Override
    public Document findDocumentByProjectId(Long projectId) {
        return documentRepository.findByProjectId(projectId);
    }

    @Override
    public Long saveAsTemplate(Long projectId, String[] tags) {

        Project project = projectRepository.findOne(projectId);

        if (project == null) {
            throw new NullPointerException("工程不存在或已删除");
        }
        Template template = templateRepository.findByProjectId(project.getId());
        if (null != template) {
            template.setUpdateTime(Calendar.getInstance().getTime());
            return template.getId();
        }

        template = new Template();
        template.setCreatorId(project.getCreatorId());
        template.setCreatorName(project.getCreatorName());
        template.setProject(project);
        template.setTags(ArrayUtils.isEmpty(tags) ? "" : StringKit.join(tags, ","));
        templateRepository.save(template);
        return template.getId();
    }

    @Override
    public void save(TokenModel tokenModel, Long parentId, boolean supportParalleled, File... files) throws IOException {
        File destFile = null;
        try {
            String docDir = RedisUtils.get(Constants.DOCUMENT_DIR, String.class);
            for(File file : files) {
                String projectName = FilenameUtils.getBaseName(file.getName());
                //新建工程
                Project newProject = new Project();
                newProject.setName(projectName);
                newProject.setCreatorName(tokenModel.getUsername());
                newProject.setCreatorId(tokenModel.getUserId());
                newProject.setParalleled(supportParalleled);
                newProject.setParentId(parentId);
                save(newProject);
                //解密
                destFile = new File(StringKit.getBase64FromUUID());
                destFile = FileKit.decryptFile(file, destFile);
                String content = FileUtils.readFileToString(destFile, com.tipdm.framework.common.Constants.CHARACTER);

                JSONObject jsonObject = JSONObject.parseObject(content, Feature.OrderedField);
                List<Node> nodes = JSON.parseObject(JSON.toJSONString(jsonObject.get("nodes"), SerializerFeature.SortField), new TypeReference<List<Node>>() {
                }, Feature.OrderedField);
                List<Link> links = JSON.parseObject(jsonObject.getString("links"), new TypeReference<List<Link>>() {
                }, Feature.OrderedField);

                for (Node node : nodes) {
                    String nodeId = node.getId();
                    final String seqId = GlobalSeqGenerator.getNextId().toString();
                    node.getInputs().stream().forEach(new Consumer<IO>() {
                        @Override
                        public void accept(IO io) {
                            boolean compare = io.getId().equals(io.getValue());
                            String[] tmp = StringKit.split(io.getId(), "_");
                            tmp[0] = seqId;
                            io.setId(StringKit.join(tmp, "_"));
                            //如果没有修改前id的值等于value
                            if (compare) {
                                io.setValue(io.getId());
                            }
                        }
                    });

                    node.getOutputs().stream().forEach(new Consumer<IO>() {
                        @Override
                        public void accept(IO io) {
                            boolean compare = io.getId().equals(io.getValue());
                            String[] tmp = StringKit.split(io.getId(), "_");
                            tmp[0] = seqId;
                            io.setId(StringKit.join(tmp, "_"));
                            //如果没有修改前id的值等于value
                            if (compare) {
                                io.setValue(io.getId());
                            }
                        }
                    });

                    links.stream().filter(x -> nodeId.equals(x.getTarget())).forEach(x -> x.setTarget(seqId));
                    links.stream().filter(x -> nodeId.equals(x.getSource())).forEach(x -> x.setSource(seqId));
                    node.setId(seqId);
                }

                links.stream().forEach(new Consumer<Link>() {
                    @Override
                    public void accept(Link link) {
                        String[] tmp = StringKit.split(link.getInputPortId(), "_");
                        tmp[0] = link.getTarget();
                        link.setInputPortId(StringKit.join(tmp, "_"));

                        tmp = StringKit.split(link.getOutputPortId(), "_");
                        tmp[0] = link.getSource();
                        link.setOutputPortId(StringKit.join(tmp, "_"));
                        link.setId(link.getOutputPortId() + "_" + link.getInputPortId());
                    }
                });

                //迭代输入节点，将数据源/模型共享给当前用户
                nodes.stream().forEach(new Consumer<Node>() {
                    @Override
                    public void accept(Node node) {
                        if (CollectionUtils.isEmpty(node.getInputs())) {

                            if ("Input".equals(node.getTargetAlgorithm())) {
                                node.getOutputs().forEach(new Consumer<IO>() {
                                    @Override
                                    public void accept(IO io) {
                                        try {
                                            Long tableId = Long.parseLong(io.getValue());
                                        } catch (ClassCastException e) {

                                        }
                                    }
                                });
                            }

                            if ("ModelLoader".equals(node.getTargetAlgorithm())) {
                                node.getOutputs().forEach(new Consumer<IO>() {
                                    @Override
                                    public void accept(IO io) {
                                        try {
                                            Long modelId = Long.parseLong(io.getValue());
                                        } catch (ClassCastException e) {

                                        }
                                    }
                                });
                            }
                        }
                    }
                });

                JSONObject json = new JSONObject();
                json.put("nodes", nodes);
                json.put("links", links);
                json.put("style", jsonObject.get("style"));
                String jsonString = JSON.toJSONString(json, new ValueFilter() {
                    @Override
                    public Object process(Object source, String name, Object value) {
                        if (name.contains("Items")) {
                            return JSON.parseObject((String) value, new TypeReference<List<Map>>() {
                            });
                        }
                        return value;
                    }
                });
                File newJson = new File(docDir + "/" + tokenModel.getUsername() + "/" + getRealPathByDocumentId(newProject.getParentId()), newProject.getName() + ".json");
                FileUtils.writeStringToFile(newJson, jsonString, com.tipdm.framework.common.Constants.CHARACTER);
            }
        } catch (JSONException e){
            throw new JSONException("文件导入失败，原因：无法解析工程文件");
        } catch (Exception e) {
            throw e;
        } finally {
            if (null != destFile) {
                destFile.delete();
            }
        }
    }

    @Transactional
    @Override
    public Long cloneProject(Long projectId, TokenModel tokenModel, Long parentId, String projectName) throws Exception {
        File destFile = null;
        try {
            Project project = findOne(projectId);
            if (null == project) {
                LOG.error("Can not find Project by Id:{}", projectId);
                return -1L;
            }

            String docDir = RedisUtils.get(Constants.DOCUMENT_DIR, String.class);
            Document doc = findDocumentByProjectId(projectId);
            if (null == doc) {
                LOG.error("Can not find Document by Id:{}", projectId);
                return -1L;
            }
            File parentDir = new File(docDir + "/" + project.getCreatorName() + "/" + getRealPathByDocumentId(doc.getParentId()));
            File file = new File(parentDir, project.getName() + ".json");

            if (!file.exists()) {
                LOG.error("NoSuch File:{}", file.getAbsolutePath());
                return -1L;
            }

            //新建工程
            Project newProject = new Project();
            newProject.setName(projectName);
            newProject.setCreatorName(tokenModel.getUsername());
            newProject.setCreatorId(tokenModel.getUserId());
            newProject.setParalleled(project.getParalleled());
            newProject.setDescription(project.getDescription());
            newProject.setParentId(parentId);
            save(newProject);
            destFile = new File(FileKit.getTempDirectory(), UUID.randomUUID().toString());
            FileKit.copyFile(file, destFile);
            String content = FileUtils.readFileToString(destFile, com.tipdm.framework.common.Constants.CHARACTER);

            JSONObject jsonObject = JSONObject.parseObject(content, Feature.OrderedField);
            List<Node> nodes = JSON.parseObject(JSON.toJSONString(jsonObject.get("nodes"), SerializerFeature.SortField), new TypeReference<List<Node>>() {
            }, Feature.OrderedField);
            List<Link> links = JSON.parseObject(jsonObject.getString("links"), new TypeReference<List<Link>>() {
            }, Feature.OrderedField);

            for (Node node : nodes) {
                String nodeId = node.getId();
                final String seqId = GlobalSeqGenerator.getNextId().toString();
                node.getInputs().stream().forEach(new Consumer<IO>() {
                    @Override
                    public void accept(IO io) {
                        boolean compare = io.getId().equals(io.getValue());
                        String[] tmp = StringKit.split(io.getId(), "_");
                        tmp[0] = seqId;
                        io.setId(StringKit.join(tmp, "_"));
                        //如果没有修改前id的值等于value
                        if (compare) {
                            io.setValue(io.getId());
                        }
                    }
                });

                node.getOutputs().stream().forEach(new Consumer<IO>() {
                    @Override
                    public void accept(IO io) {
                        boolean compare = io.getId().equals(io.getValue());
                        String[] tmp = StringKit.split(io.getId(), "_");
                        tmp[0] = seqId;
                        io.setId(StringKit.join(tmp, "_"));
                        //如果没有修改前id的值等于value
                        if (compare) {
                            io.setValue(io.getId());
                        }
                    }
                });

                links.stream().filter(x -> nodeId.equals(x.getTarget())).forEach(x -> x.setTarget(seqId));
                links.stream().filter(x -> nodeId.equals(x.getSource())).forEach(x -> x.setSource(seqId));
                node.setId(seqId);
            }

            links.stream().forEach(new Consumer<Link>() {
                @Override
                public void accept(Link link) {
                    String[] tmp = StringKit.split(link.getInputPortId(), "_");
                    tmp[0] = link.getTarget();
                    link.setInputPortId(StringKit.join(tmp, "_"));

                    tmp = StringKit.split(link.getOutputPortId(), "_");
                    tmp[0] = link.getSource();
                    link.setOutputPortId(StringKit.join(tmp, "_"));
                    link.setId(link.getOutputPortId() + "_" + link.getInputPortId());
                }
            });

            //迭代输入节点，将数据源/模型共享给当前用户
            nodes.stream().forEach(new Consumer<Node>() {
                @Override
                public void accept(Node node) {
                    if (CollectionUtils.isEmpty(node.getInputs())) {

                        if ("com.tipdm.framework.dmserver.core.algo.unparallel.io.Input".equals(node.getTargetAlgorithm())) {
                            node.getOutputs().forEach(new Consumer<IO>() {
                                @Override
                                public void accept(IO io) {
                                    try {
                                        Long dataTableId = Long.parseLong(io.getValue());
                                        DataTable table = tableRepository.findOne(dataTableId);
                                        Assert.notNull(table, "数据表不存在，Id:" + dataTableId);
                                        Audience audience = new Audience();
                                        audience.setUserId(tokenModel.getUserId());
                                        audience.setUserName(tokenModel.getUsername());
                                        audience.setSharedObjectId(dataTableId);
                                        audience.setObjectType(ShareType.DATASOURCE);
                                        DataSchema dataSchema = dataSchemaRepository.findByName(audience.getUserName());
                                        if (null == dataSchema) {
                                            dataSchema = new DataSchema();
                                            dataSchema.setName(audience.getUserName());
                                            String password = RandomStringUtils.randomAlphabetic(8);
                                            dataSchema.setPassword(password);
                                            dataSchemaRepository.createSchema(audience.getUserName(), password);
                                            dataSchemaRepository.save(dataSchema);
                                        }
                                        // 授予被分享表的查询权限
                                        tableRepository.grantPrivilege(table.getCreatorName(), table.getTableName(), audience.getUserName());
                                        audienceRepository.save(audience);
                                    } catch (Exception e) {
                                        LOG.error(ExceptionUtils.getStackTrace(e));
                                    }
                                }
                            });
                        }

                        if ("ModelLoader".equals(node.getTargetAlgorithm())) {
                            node.getOutputs().forEach(new Consumer<IO>() {
                                @Override
                                public void accept(IO io) {
                                    try {
                                        Long modelId = Long.parseLong(io.getValue());
                                    } catch (ClassCastException e) {

                                    }
                                }
                            });
                        }
                    }
                }
            });

            JSONObject json = new JSONObject();
            json.put("nodes", nodes);
            json.put("links", links);
            json.put("style", jsonObject.get("style"));
            String jsonString = JSON.toJSONString(json, new ValueFilter() {
                @Override
                public Object process(Object source, String name, Object value) {
                    if (name.contains("Items")) {
                        return JSON.parseObject((String) value, new TypeReference<List<Map>>() {
                        });
                    }
                    return value;
                }
            });
            File newJson = new File(docDir + "/" + tokenModel.getUsername() + "/" + getRealPathByDocumentId(newProject.getParentId()), newProject.getName() + ".json");
            FileUtils.writeStringToFile(newJson, jsonString, com.tipdm.framework.common.Constants.CHARACTER);
            return newProject.getId();
        } catch (Exception e) {
            throw e;
        } finally {
            if (null != destFile) {
                destFile.delete();
            }
        }
    }

    @Transactional
    @Override
    public Long saveAs(Long projectId, TokenModel tokenModel, Long parentId, String projectName) throws Exception {
        File destFile = null;
        try {
            Project project = findOne(projectId);
            if (null == project) {
                LOG.error("Can not find Project by Id:{}", projectId);
                return -1L;
            }

            if((!project.getCreatorId().equals(tokenModel.getUserId()))){
                LOG.error("Project's creatorId not match currentUser with projectId:{}", projectId);
                return -1L;
            }

            String docDir = RedisUtils.get(Constants.DOCUMENT_DIR, String.class);
            Document doc = findDocumentByProjectId(projectId);
            if (null == doc) {
                LOG.error("Can not find Document by Id:{}", projectId);
                return -1L;
            }
            File parentDir = new File(docDir + "/" + project.getCreatorName() + "/" + getRealPathByDocumentId(doc.getParentId()));
            File file = new File(parentDir, project.getName() + ".json");

            if (!file.exists()) {
                LOG.error("NoSuch File:{}", file.getAbsolutePath());
                return -1L;
            }

            //新建工程
            Project newProject = new Project();
            newProject.setName(projectName);
            newProject.setCreatorName(tokenModel.getUsername());
            newProject.setCreatorId(tokenModel.getUserId());
            newProject.setParalleled(project.getParalleled());
            newProject.setDescription(project.getDescription());
            newProject.setParentId(parentId);
            save(newProject);

            destFile = new File(FileKit.getTempDirectory(), UUID.randomUUID().toString());
            FileKit.copyFile(file, destFile);
            String content = FileUtils.readFileToString(destFile, com.tipdm.framework.common.Constants.CHARACTER);

            JSONObject jsonObject = JSONObject.parseObject(content);
            List<Node> nodes = JSON.parseArray(jsonObject.getString("nodes"), Node.class);
            List<Link> links = JSON.parseArray(jsonObject.getString("links"), Link.class);

            for (Node node : nodes) {
                String nodeId = node.getId();
                final String seqId = GlobalSeqGenerator.getNextId().toString();
                node.getInputs().stream().forEach(new Consumer<IO>() {
                    @Override
                    public void accept(IO io) {
                        boolean compare = io.getId().equals(io.getValue());
                        String[] tmp = StringKit.split(io.getId(), "_");
                        tmp[0] = seqId;
                        io.setId(StringKit.join(tmp, "_"));
                        //如果没有修改前id的值等于value
                        if (compare) {
                            io.setValue(io.getId());
                        }
                    }
                });

                node.getOutputs().stream().forEach(new Consumer<IO>() {
                    @Override
                    public void accept(IO io) {
                        boolean compare = io.getId().equals(io.getValue());
                        String[] tmp = StringKit.split(io.getId(), "_");
                        tmp[0] = seqId;
                        io.setId(StringKit.join(tmp, "_"));
                        //如果没有修改前id的值等于value
                        if (compare) {
                            io.setValue(io.getId());
                        }
                    }
                });

                links.stream().filter(x -> nodeId.equals(x.getTarget())).forEach(x -> x.setTarget(seqId));
                links.stream().filter(x -> nodeId.equals(x.getSource())).forEach(x -> x.setSource(seqId));
                node.setId(seqId);
            }

            links.stream().forEach(new Consumer<Link>() {
                @Override
                public void accept(Link link) {
                    String[] tmp = StringKit.split(link.getInputPortId(), "_");
                    tmp[0] = link.getTarget();
                    link.setInputPortId(StringKit.join(tmp, "_"));

                    tmp = StringKit.split(link.getOutputPortId(), "_");
                    tmp[0] = link.getSource();
                    link.setOutputPortId(StringKit.join(tmp, "_"));
                    link.setId(link.getOutputPortId() + "_" + link.getInputPortId());
                }
            });

            JSONObject json = new JSONObject();
            json.put("nodes", nodes);
            json.put("links", links);
            json.put("style", jsonObject.get("style"));
            String jsonString = JSON.toJSONString(json, new ValueFilter() {
                @Override
                public Object process(Object source, String name, Object value) {
                    if (name.contains("Items")) {
                        List<Map> items = JSON.parseObject((String) value, new TypeReference<List<Map>>() {
                        });
                        return items;
                    }
                    return value;
                }
            });
            File newJson = new File(docDir + "/" + tokenModel.getUsername() + "/" + getRealPathByDocumentId(newProject.getParentId()), newProject.getName() + ".json");
            FileUtils.writeStringToFile(newJson, jsonString, com.tipdm.framework.common.Constants.CHARACTER);
            return newProject.getId();
        } catch (Exception e) {
            throw e;
        } finally {
            if (destFile != null) {
                destFile.delete();
            }
        }
    }

    @Override
    public void modifyDesc(Long projectId, String desc) throws NoSuchElementException {
        if (null == projectId || projectId == 0) {
            throw new NoSuchElementException("挖掘工程不存在");
        }
        Project project = projectRepository.findOne(projectId);
        if (null == project) {
            throw new NoSuchElementException("挖掘工程不存在");
        }
        project.setDescription(desc);
    }

    @Override
    public File export(Long[] ids) throws IOException {
        if (RedisUtils.exists(Constants.DOCUMENT_DIR)) {
            //从Redis获取挖掘工程对应流程保存的路径
            String docDir = RedisUtils.get(Constants.DOCUMENT_DIR, String.class);
            //查询要导出的工程
            Iterable<Project> iterable = projectRepository.findAll(Arrays.asList(ids));
            //新建一个临时目录
            File tmp = new File(StringKit.getBase64FromUUID());
            tmp.mkdir();
            try {
                iterable.forEach(x -> {
                    Document document = documentRepository.findByProjectId(x.getId());
                    //构建文件路径
                    File parentDir = new File(docDir + "/" + x.getCreatorName() + "/" + getRealPathByDocumentId(document.getParentId()));
                    File file = new File(parentDir, x.getName() + ".json");
                    try {
                        if (file.exists()) {
                            //读取文件内容
                            String content = FileUtils.readFileToString(file, com.tipdm.framework.common.Constants.CHARACTER);
                            JSONObject jsonObject = JSONObject.parseObject(content);
                            //追加工程描述
                            jsonObject.put("description", x.getDescription());
                            FileKit.writeStringToFile(file, jsonObject.toJSONString(), false);
                            File destFile = new File(tmp, file.getName());
                            //目录下是否已经存在同名文件
                            if(FileKit.directoryContains(tmp, destFile)){
                                String destName = String.format("%1$s-%2$s.json", StringKit.substringBeforeLast(file.getName(), "."), StringKit.getBase64FromUUID());
                                destFile = new File(tmp,  destName);
                                FileKit.copyFile(file, destFile);
                            } else {
                                FileKit.copyFileToDirectory(file, tmp);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                File[] files = tmp.listFiles();
                //单个工程不进行压缩
                if (files.length == 1) {
                    return FileKit.encryptFile(files[0], new File(StringKit.substringBeforeLast(files[0].getName(), ".") + ".tpf"));
                } else {
                    //生成zip格式的压缩包
                    File zipFile = new File(tmp.getName() + ".zip");
                    try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile))) {
                        for (int i = 0; i < files.length; ++i) {
                            //加密挖掘工程的流程文件
                            File tpf = FileKit.encryptFile(files[i], new File(StringKit.substringBeforeLast(files[i].getName(), ".") + ".tpf"));
                            try (InputStream input = new FileInputStream(tpf)) {
                                zipOut.putNextEntry(new ZipEntry(tmp.getName() + File.separator + tpf.getName()));
                                int len = 0;
                                while ((len = input.read()) != -1) {
                                    zipOut.write(len);
                                }
                            }
                        }
                    }
                    return zipFile;
                }
            } catch (Exception ex) {
                throw new IOException("挖掘工程导出失败，错误信息：" + ex.getMessage());
            } finally {
                FileKit.deleteDirectory(tmp);
            }
        }
        return null;
    }
}