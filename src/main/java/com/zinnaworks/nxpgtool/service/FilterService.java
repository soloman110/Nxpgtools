package com.zinnaworks.nxpgtool.service;

import java.util.List;
import java.util.Map;

public interface FilterService {
	public Map<String, Object> filterINFNXPG004(Map<String, String> param, boolean LocFilterYn, Map<String, Object> blockblock);
	public void filterINFNXPG004Month(Map<String, String> param, Map<String, Object> blockblock);
	public Map<String, Object> filterINFNXPG005(Map<String, Object> event);
	public Map<String, Object> filterINFNXPG006(Map<String, Object> grid);
	public void filterINFNXPG014(Map<String, String> param, Map<String, Object> review);
	public void filterINFNXPG007(Map<String, String> param, Map<String, Object> contents);
	public void filterINFNXPG015(Map<String, String> param, Map<String, Object> purchares);
	public void filterINFNXPG009(Map<String, String> param, Map<String, Object> gwsynop);
	public void filterINFNXPG010(Map<String, String> param, Map<String, Object> commerce);
	public void filterINFNXPG032(Map<String, String> param, Map<String, Object> ppmCommerce);
	public void filterINFNXPG033(Map<String, String> param, List<Map<String, Object>> menus);
	public void filterINFNXPG020(Map<String, String> param, List<Map<String, Object>> kzgnb);
	public  Map<String, Object> filterINFNXPG019(Map<String, String> param, List<Map<String, Object>> kids);
	public void filterINFNXPG021(Map<String, String> param, Map<String, Object> contents);
}
