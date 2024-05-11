package com.tipdm.framework.controller.dmserver.dto;

import java.io.Serializable;

/**
 * Created by TipDM on 2017/7/27.
 * E-mail:devp@tipdm.com
 */
public class UploadInfo implements Serializable{

    private Long id;

    private String uploadId;

    private Category category;

    public UploadInfo(Long id, String uploadId, Category category){
        this.id = id;
        this.uploadId = uploadId;
        this.category = category;
    }

    public enum Category{
        FLAT,
        UDC
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }
}
