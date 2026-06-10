package com.pcm.kms.core.service;

import java.util.LinkedHashMap;
import java.util.Map;

public class RuntimeInfoService {

    public Map<String, Object> summary() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("project", "pcm-kms");
        result.put("stage", "phase-1");
        result.put("status", "bootstrap-ready");
        return result;
    }
}
