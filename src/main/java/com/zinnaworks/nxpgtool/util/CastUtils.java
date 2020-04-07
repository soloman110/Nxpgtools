package com.zinnaworks.nxpgtool.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.json.GsonJsonParser;
import org.springframework.boot.json.JacksonJsonParser;

import com.google.gson.Gson;

public class CastUtils {
	@SuppressWarnings("unchecked")
	public static Map<String, Object> getObjectToMap(Object obj) {
		if (obj != null && obj instanceof Map<?, ?>) {
			return (Map<String, Object>) obj;
		} else return null;
	}
	
	public static String getObjectToString(Object obj) {
		if (obj != null && obj instanceof String) {
			String str = (String) obj;
			return str;
		} else return null;
	}
	
	public static String getListToJsonArrayString(Object obj) {
		String rtn = "";
		if (obj != null && obj instanceof List) {
			try {
				Gson gson = new Gson();
				rtn = gson.toJson(obj);
				
			} catch (Exception e) {
				System.out.println(e.toString());
			}
		}
		return rtn;
	}
	
	public static List<Object> StringToJsonList(String json) {
		if (json != null) {	
			try {
				JacksonJsonParser parser = new JacksonJsonParser();
				List<Object> list = parser.parseList(json);
				return list;	
			} catch (Exception e) {
				return null;
			}
		} else return null;
	}
	
	public static int getStrToInt(String str) {
		if (str == null) return 0;
		if (str.equals("")) return 0;
		
		int rtn = 0;
		String temp = "";
		try {
			for(int i = 0; i < str.length(); i++) {
				if (str.charAt(i) == 46) break;
				if ((str.charAt(i) >= 48) && (str.charAt(i) <= 57)) {
					temp += str.charAt(i);
				}
			}
			rtn = Integer.parseInt(temp);
		} catch (Exception e) {
			return 0;
		}
		return rtn;
	}
	
	public static Map<String, Object> StringToJsonMap(String json) {
		try {
			if (json != null) {
				GsonJsonParser parser = new GsonJsonParser();
				Map<String, Object> map = parser.parseMap(json);
				return map;
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}
	
	public static List<Map<String, Object>> getJSONArrayToMapList(JSONArray jarr) {
		if (jarr == null) return null;
		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
		
		for (int i = 0; i < jarr.length(); i++) {
			resultList.add(StringToJsonMap(jarr.getJSONObject(i).toString()));
		}
		
		return resultList;
	}
	
	public static String getStringToJSONObject(JSONObject obj, String key) {
		if (obj == null) return null;
		if (obj.has(key) == false) return null;
		Object value = obj.get(key);
		
		if (value instanceof Integer) {
			return Integer.toString(obj.getInt(key));
		} else if (obj.get(key) instanceof Double) {
			return Long.toString(Math.round(obj.getDouble(key)));
		} else if (obj.get(key) instanceof String) {
			return obj.getString(key);
		} else {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> getObjectToMapList(Object obj) {
		if (obj != null && obj instanceof List<?>) {
			return (List<Map<String, Object>>) obj;
		} else return null;
	}
	
	public static JSONObject getObjectToJSONObject(Object obj) {
		if (obj != null && obj instanceof JSONObject) {
			return (JSONObject) obj;
		} else return null;
	}
}
