package com.tipdm.framework.service.dmserver.impl;

import com.tipdm.framework.model.dmserver.Template;
import com.tipdm.framework.repository.dmserver.TemplateRepository;
import com.tipdm.framework.service.dmserver.TemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Created by TipDM on 2017/2/22.
 * E-mail:devp@tipdm.com
 */
@Service("templateService")
public class TemplateServiceImpl implements TemplateService{

    private final static Logger logger = LoggerFactory.getLogger(TemplateServiceImpl.class);

    @Autowired
    private TemplateRepository templateRepository;

    @Override
    public Page<Template> findTemplateList(Pageable pageable) {

        return templateRepository.findAll(pageable);
    }

    @Override
    public void delete(Long templateId) {
        templateRepository.delete(templateId);
    }
}
