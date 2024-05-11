package com.tipdm.framework.repository.dmserver.impl;

import com.tipdm.framework.model.dmserver.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

/**
 * Created by TipDM on 2017/1/3.
 * E-mail:devp@tipdm.com
 */
public class TemplateRepositoryImpl {

    @PersistenceContext
    private EntityManager em;

    public void setEm(EntityManager em) {
        this.em = em;
    }

    public Page<Template> findTemplate(Pageable pageable){

        Query query = em.createQuery("from Template order by updateTime desc");
        query.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());
        query.setMaxResults(pageable.getPageSize());

        List list = query.getResultList();
        Page<Template> page = new PageImpl<Template>(list, pageable, list.size());

        return page;
    }

}
