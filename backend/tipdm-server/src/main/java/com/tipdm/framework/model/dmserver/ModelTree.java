package com.tipdm.framework.model.dmserver;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tipdm.framework.model.IdEntity;

import javax.persistence.*;

/**
 * 模型树
 */
@Entity
@Table(name = "dm_model_tree", uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "parent_id"})})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ModelTree extends IdEntity<Long> {

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "is_leaf", nullable = false)
    private Boolean isLeaf = Boolean.FALSE;

    @Column(name = "is_delete", nullable = false)
    private Boolean isDelete = Boolean.FALSE;

    @Column(name = "parent_id")
    private Long parentId = 0L;

    @Column
    private String path = "/";

    @OneToOne(targetEntity = Model.class, cascade = CascadeType.ALL)
    @JoinColumn(name = "model_id", unique = true)
    private Model model;

    public Boolean getDelete() {
        return isDelete;
    }

    public void setDelete(Boolean delete) {
        isDelete = delete;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getLeaf() {
        return isLeaf;
    }

    public void setLeaf(Boolean leaf) {
        isLeaf = leaf;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
