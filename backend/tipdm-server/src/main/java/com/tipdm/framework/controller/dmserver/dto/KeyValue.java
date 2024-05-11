package com.tipdm.framework.controller.dmserver.dto;

import java.io.Serializable;

/**
 * Created by TipDM on 2017/8/23.
 * E-mail:devp@tipdm.com
 */
public class KeyValue implements Serializable{

    private Object key;

    private Object value;

    public KeyValue(Object key, Object value) {
        this.key = key;
        this.value = value;
    }

    public Object getKey() {
        return key;
    }

    public void setKey(Object key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
