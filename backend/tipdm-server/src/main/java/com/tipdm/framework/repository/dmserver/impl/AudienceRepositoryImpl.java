package com.tipdm.framework.repository.dmserver.impl;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by TipDM on 2017/3/16.
 * E-mail:devp@tipdm.com
 */
public class AudienceRepositoryImpl {

    @PersistenceContext
    private EntityManager em;

    public void setEm(EntityManager em) {
        this.em = em;
    }


}
