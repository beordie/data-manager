package com.tipdm.framework.controller.dmserver.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by TipDM on 2016/12/22.
 * E-mail:devp@tipdm.com
 */
public class SearchParam {

    private Map<String, Object> searchParams = new HashMap<>();

    public Map<String, Object> getSearchParams() {
        return searchParams;
    }

    public void setSearchParams(Map<String, Object> searchParams) {
        this.searchParams = searchParams;
    }
}
