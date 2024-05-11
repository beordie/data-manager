package com.tipdm.framework.dmserver.core.scheduling;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class JobContext {

    private static ThreadLocal<Map<String, Object>> local = new ThreadLocal<Map<String, Object>>() {
        @Override
        protected Map<String, Object> initialValue() {
            return new HashMap<>();
        }
    };

    static
    public void addEntry(String key, Object value) {
        local.get().put(key, value);
    }

    static
    public Map<String, Object> get() {
        return local.get();
    }

    static
    public void clean() {
        local.remove();
    }
}
