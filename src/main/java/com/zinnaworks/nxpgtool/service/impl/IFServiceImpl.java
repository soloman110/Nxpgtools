package com.zinnaworks.nxpgtool.service.impl;

import static com.zinnaworks.nxpgtool.util.CommonUtils.loadData;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.zinnaworks.nxpgtool.api.IFParser;
import com.zinnaworks.nxpgtool.common.FileStorageProperties;
import com.zinnaworks.nxpgtool.config.CollectionProperties;
import com.zinnaworks.nxpgtool.exception.DataNotValidException;
import com.zinnaworks.nxpgtool.exception.FileStorageException;
import com.zinnaworks.nxpgtool.rest.RestClient_BadCode;
import com.zinnaworks.nxpgtool.service.FilterService;
import com.zinnaworks.nxpgtool.service.IFService;
import com.zinnaworks.nxpgtool.util.CastUtils;
import com.zinnaworks.nxpgtool.util.JsonUtil;

@Service
public class IFServiceImpl implements IFService {
	private final Path fileStorageLocation;
	
	@Autowired
	private FileStorageProperties fileStorageProperties;

	@Autowired
	private CollectionProperties collectionProperties;
	
	@Autowired
	RestClient_BadCode client;
	
	@Autowired
	FilterService filterService;
	
	@Value("${task.partial.url}")
	private String ncmsPartialUrl;
	
	@Value("${task.redis.url}")
	private String redisUrl;
	
	@Value("${task.nxpg.url}")
	private String nxpgUrl;
	
	@Autowired
	public IFServiceImpl(FileStorageProperties fileStorageProperties) {
		this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
	}
	
	@Override
	public Map<String, Object> toTree(String ifName) throws FileNotFoundException, DataNotValidException {
		IFParser ifparser = new IFParser();
		Map<String, Object> jsonIf = ifparser.parse(ifName);
		return jsonIf;
	}

	@Override
	@SuppressWarnings("unchecked")
	public String compare(Map<String, Object> remote, Map<String, Object> apiExcel) {
		for(Map.Entry<String, Object> entry : remote.entrySet()) {
			String fieldName = entry.getKey();
			Object v = entry.getValue();
			if(!apiExcel.containsKey(fieldName)) {
				return "[필드]" + fieldName + " 존재하지 않음!" + "[Detail]" + entry.getValue().toString()  + apiExcel.get(fieldName);
			}
			if(v instanceof Map) {
				return compare((Map<String, Object>)v, (Map<String, Object>)apiExcel.get(fieldName));
			}
		}
		return null;
	}

	@Override
	public Map<String, Object> getFieldTree(String ifname) throws FileNotFoundException {
		String apiJson = loadData(fileStorageProperties.getUploadDir() + "/" + ifname+".json");
		Map<String, Object> apiTree = JsonUtil.jsonToObjectHashMap(apiJson,String.class, Object.class);
		return apiTree;
	}
	
	@Override
	public List<Map<String, String>> descInterface(Map<String, String> param) {
		List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
		Map<String, Object> keyAndName = CastUtils.getObjectToMap(collectionProperties.getXpg().get(param.get("ifname")));
		String[] api = keyAndName.get("api").toString().split(","); 

		// 1. nxpg 데이터 가지고오기.
		String nxpgResult = getNxpgData(param);
		if (nxpgResult == null || nxpgResult.isEmpty()) return resultList;
		JSONObject jNxpg = new JSONObject(nxpgResult);
		
		// 2. ncms 데이터 가지고 오기.
		for (int i = 0; i < api.length; i++) {
			String[] tempApi = api[i].split("\\|");
			Map<String, String> ncmsParam = new HashMap<String, String>();
			Map<String, Object> ncmsName = CastUtils.getObjectToMap(collectionProperties.getCollections().get(tempApi[0]));
			boolean isArray = (boolean) ncmsName.get("isArray");
			
			String[] ncmsrequestparam = ncmsName.get("requestparam").toString().split(",");
			ncmsParam.put("ifname", tempApi[0]);
			for (int j = 0; j < ncmsrequestparam.length; j++) {
				ncmsParam.put(ncmsrequestparam[j], param.get(ncmsrequestparam[j]));
			}
			
			String ncmsResult = getNcmsData(ncmsParam);
			if (ncmsResult == null || ncmsResult.isEmpty()) return resultList;
			
			// 각종 로직 별 필터링 추가.
			if ("INFNXPG004".equals(tempApi[0])) {
				Map<String, Object> item = CastUtils.StringToJsonMap(ncmsResult);
				if ("IF-NXPG-005".equals(param.get("ifname")))
					filterService.filterINFNXPG004Month(param, item);
				else 
					item = filterService.filterINFNXPG004(ncmsParam, false, item);
				ncmsResult = new JSONObject(item).toString();
			}
			if ("INFNXPG005".equals(tempApi[0])) {
				Map<String, Object> item = CastUtils.StringToJsonMap(ncmsResult);
				item = filterService.filterINFNXPG005(item);
				ncmsResult = new JSONObject(item).toString();
			}
			if ("INFNXPG006".equals(tempApi[0])) {
				Map<String, Object> item = CastUtils.StringToJsonMap(ncmsResult);
				item = filterService.filterINFNXPG006(item);
				ncmsResult = new JSONObject(item).toString();
			}
			if ("INFNXPG014".equals(tempApi[0])) {
				Map<String, Object> item = CastUtils.StringToJsonMap(ncmsResult);
				filterService.filterINFNXPG014(param, item);
				ncmsResult = new JSONObject(item).toString();
			}
			
			// 시놉시스
			if ("INFNXPG007".equals(tempApi[0])) {
				Map<String, Object> item = CastUtils.StringToJsonMap(ncmsResult);
				filterService.filterINFNXPG007(param, item);
				ncmsResult = new JSONObject(item).toString();
			}
			if ("INFNXPG015".equals(tempApi[0])) {
				Map<String, Object> item = CastUtils.StringToJsonMap(ncmsResult);
				filterService.filterINFNXPG015(param, item);
				ncmsResult = new JSONObject(item).toString();
			}
			
			// 게이트웨이 시놉시스
			if ("INFNXPG009".equals(tempApi[0])) {
				Map<String, Object> item = CastUtils.StringToJsonMap(ncmsResult);
				filterService.filterINFNXPG009(param, item);
				ncmsResult = new JSONObject(item).toString();
			}
			
			// VOD+관련상품 시놉시스
			if ("INFNXPG010".equals(tempApi[0])) {
				Map<String, Object> item = CastUtils.StringToJsonMap(ncmsResult);
				filterService.filterINFNXPG010(param, item);
				ncmsResult = new JSONObject(item).toString();
			}
			
			// PPM+관련상품 시놉시스
			if ("INFNXPG032".equals(tempApi[0])) {
				Map<String, Object> item = CastUtils.StringToJsonMap(ncmsResult);
				filterService.filterINFNXPG032(param, item);
				ncmsResult = new JSONObject(item).toString();
			}
			//////////////////////////////////
			
			// 데이터 비교.
			if (jNxpg.has(tempApi[1])) {
				if (isArray) 
					JsonUtil.descJSONArray(collectionProperties, tempApi[1], param, tempApi[0], resultList, jNxpg.getJSONArray(tempApi[1]), new JSONArray(ncmsResult));
				else {
					if (jNxpg.get(tempApi[1]) instanceof JSONArray) {
						JsonUtil.descJSONArray(collectionProperties, tempApi[1], param, tempApi[0], resultList, jNxpg.getJSONArray(tempApi[1]), new JSONObject(ncmsResult).getJSONArray(tempApi[1]));
					} else if (jNxpg.get(tempApi[1]) instanceof JSONObject) {
						JsonUtil.descJSONObject(collectionProperties, tempApi[1], param, tempApi[0], resultList, jNxpg.getJSONObject(tempApi[1]), new JSONObject(ncmsResult));
					} else {
						JsonUtil.descJSONObject(collectionProperties, tempApi[1], param, tempApi[0], resultList, jNxpg, new JSONObject(ncmsResult));
					}
				}
			}	
		}
			
				
		return resultList;
	}
	
	public String getNxpgData(Map<String, String> param) {
		Map<String, Object> keyAndName = CastUtils.getObjectToMap(collectionProperties.getXpg().get(param.get("ifname")));
		String[] requestparam = keyAndName.get("requestparam").toString().split(","); 
		String result = null;
		String url = "";
		String value = "";
		
		try {
			for (int i = 0; i < requestparam.length; i++) {
				if (i == 0) value = requestparam[i]+"="+URLEncoder.encode(param.get(requestparam[i]), "UTF-8");
				else value += "&" + requestparam[i]+"="+URLEncoder.encode(param.get(requestparam[i]), "UTF-8");
			}
			url = nxpgUrl + keyAndName.get("path").toString() + "?" + value;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		result = client.apacheGet(url, null);
		
		return result;
	}
	
	@Override
	public String getNcmsData(Map<String, String> param) {
		Map<String, Object> keyAndName = CastUtils.getObjectToMap(collectionProperties.getCollections().get(param.get("ifname"))); 
		String[] referencekey = keyAndName.get("referencekey").toString().split(",");
		boolean isArray = (boolean) keyAndName.get("isArray");
		boolean itemIsString = (boolean) keyAndName.get("itemIsString");
		String result = "";

		String value = "";
		String url = "";
		
		String[] requestparam = keyAndName.get("requestparam").toString().split(",");
		value = "";
		for (int i = 0; i < requestparam.length; i++) {
			if (i == 0) value = requestparam[i]+"|"+param.get(requestparam[i]);
			else value += "," + requestparam[i]+"|"+param.get(requestparam[i]);
		}
		
		url = ncmsPartialUrl + keyAndName.get("path").toString();
		
		Map<String, String> ncms = client.getRestUri(url, value);
		if (ncms == null) return "";

		Map<String, Object> items = CastUtils.StringToJsonMap(ncms.get("json"));
		if (items == null) return "";
		
		Map<String, Object> item = CastUtils.getObjectToMap(items.get("items"));
		if (item == null) return "";
		if (isArray) {
			//Data : json list to redis
			JSONArray jarr = new JSONArray(CastUtils.getObjectToMapList(item.get("item")));
			result = jarr.toString();
		} else if (itemIsString) {
			JSONObject jsonObject = null;
			
			if (item.get("item") instanceof JSONArray || item.get("item") instanceof List) {
				List<Map<String, Object>> array = CastUtils.getObjectToMapList(item.get("item"));
				if (array != null && array.size() > 0) {
					jsonObject = new JSONObject(CastUtils.getObjectToMap(array.get(0)));
				}
			} else {
				jsonObject = new JSONObject(CastUtils.getObjectToMap(item.get("item")));
			}
			
			if (jsonObject != null) {
				result = jsonObject.toString();
			}
			
		} else {
			//Data : json map to redis
			if (item.get("item") instanceof List) {
				JSONArray jsonArray = new JSONArray(CastUtils.getObjectToMapList(item.get("item")));
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject jsonObject = CastUtils.getObjectToJSONObject(jsonArray.get(i));
					for (int j = 0; j < referencekey.length; j++) {
						if (param.containsKey(referencekey[j])) {
							String paramValue = param.get(referencekey[j]).toString();
							String ncmsValue = jsonObject.getString(referencekey[j]);
							if (paramValue.equals(ncmsValue)) {
								result = jsonObject.toString();
								break;
							}
						}
					}
					if ("".equals(result) == false) {
						break;
					}
				}
				if ("".equals(result)) {
					result = jsonArray.toString();
				}
			} else if(item.get("item") instanceof Map) {
				JSONObject jsonObject = new JSONObject(CastUtils.getObjectToMap(item.get("item")));
				result = jsonObject.toString();
			}
		}
		
		return result;
	}
	
	@Override
	public String compareInterface(Map<String, String> param) {
		JSONObject jResult = new JSONObject();
		jResult.put("result", "9999");
		
		Map<String, Object> keyAndName = CastUtils.getObjectToMap(collectionProperties.getCollections().get(param.get("ifname"))); 
		String[] referencekey = keyAndName.get("referencekey").toString().split(",");
		String value = "";
		
		boolean isArray = (boolean) keyAndName.get("isArray");
		
		for (int i = 0; i < referencekey.length; i++) {
			if (i == 0) value = param.get(referencekey[i]);
			else value += "_" + param.get(referencekey[i]);
		}
		//////////////////////////
		// 1. redis 정보를 확인한다.
		String url = "";
		try {
			url = redisUrl + keyAndName.get("name") + "/" + URLEncoder.encode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return jResult.toString();
		}
		String redisResult = client.apacheGet(url, null);
		if (redisResult == null || redisResult.isEmpty()) return jResult.toString();
		
		//////////////////////////
		// 2. ncms api 정보를 확인한다.
		String ncmsResult = getNcmsData(param);
		if (ncmsResult == null || ncmsResult.isEmpty()) return jResult.toString();
		
		//////////////////////////
		// 3. 데이터 비교 후 결과 알려주기.
		boolean result = false;
		if (isArray) {
			result = JsonUtil.compareJSONArray(new JSONArray(redisResult), new JSONArray(ncmsResult));
			if (result) {
				result = JsonUtil.compareJSONArray(new JSONArray(ncmsResult), new JSONArray(redisResult));
			}
		} else {
			result = JsonUtil.compareJSONObject(new JSONObject(redisResult), new JSONObject(ncmsResult));
			if (result) {
				result = JsonUtil.compareJSONObject(new JSONObject(ncmsResult), new JSONObject(redisResult));
			}
		}
		if (result)
			jResult.put("result", "0000");
		else 
			jResult.put("result", "0002");
		// 결과값 넣기
		jResult.put("redisResult", redisResult);
		jResult.put("ncmsResult", ncmsResult);
		
		return jResult.toString();
	}
}
