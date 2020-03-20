package com.zinnaworks.nxpgtool.service;

import java.util.Map;

public interface FilterService {
	public Map<String, Object> filterINFNXPG004(Map<String, String> param, boolean LocFilterYn, Map<String, Object> blockblock);
	public void filterINFNXPG004Month(Map<String, String> param, Map<String, Object> blockblock);
	public Map<String, Object> filterINFNXPG005(Map<String, Object> event);
	public Map<String, Object> filterINFNXPG006(Map<String, Object> grid);
	public void filterINFNXPG014(Map<String, String> param, Map<String, Object> review);
}
