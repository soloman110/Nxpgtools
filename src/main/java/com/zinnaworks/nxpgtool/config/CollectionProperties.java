package com.zinnaworks.nxpgtool.config;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.zinnaworks.nxpgtool.util.CastUtils;

@Component
@ConfigurationProperties(value = "user")
public class CollectionProperties {
	
    private final Map<String, Object> collections = new TreeMap<>();
    private final Map<String, Object> xpg = new TreeMap<>();
    private final Map<String, String> checkdate = new TreeMap<>();
    
    public Map<String, Object> getCollections() {
        return collections;
    }

    public Map<String, Object> getXpg() {
        return xpg;
    }
    
    public boolean checkByPass(String ifname, String key) {
    	String[] processing = CastUtils.getObjectToMap(xpg.get(ifname)).get("processing").toString().split(",");
    	for(int i = 0; i < processing.length; i++) {
    		if (processing[i].equals(key)) {
    			return false;
    		}
    	}
    	return true;
    }

    public Map<String, String> getCheckdate() {
    	return checkdate;
    }
}