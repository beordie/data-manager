package com.tipdm.framework.service.dmserver;

import com.tipdm.framework.model.dmserver.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Created by TipDM on 2017/2/22.
 * E-mail:devp@tipdm.com
 */
public interface TemplateService {

    Page<Template> findTemplateList(Pageable pageable);

    void delete(Long templateId);
}
