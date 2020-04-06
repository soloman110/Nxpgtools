package com.zinnaworks.nxpgtool.util;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.zinnaworks.nxpgtool.config.CollectionProperties;

//提供将java对象转化成json结构
public final class JsonUtil {
	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final JsonUtil INSTANCE = new JsonUtil();

	private JsonUtil() {
	}

	public static JsonUtil getInstance() {
		return INSTANCE;
	}

	static {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期输出格式
		objectMapper.setDateFormat(dateFormat);
		// 设置将对象转换成JSON字符串时候:包含的属性不能为空或"";
		// Include.Include.ALWAYS 默认
		// Include.NON_DEFAULT 属性为默认值不序列化
		// Include.NON_EMPTY 属性为 空（""） 或者为NULL都不序列化
		// Include.NON_NULL 属性为NULL 不序列化
		// objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);//序列化的时候序列化全部的属性
		objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// 反序列化的时候如果多了其他属性,不抛出异常
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);// 转化成格式化的json
		objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		// 是否允许一个类型没有注解表明打算被序列化。默认true，抛出一个异常；否则序列化一个空对象，比如没有任何属性。
		objectMapper.disable(SerializationFeature.FLUSH_AFTER_WRITE_VALUE);
		// 该特性决定是否在writeValue()方法之后就调用JsonGenerator.flush()方法。
		// 当我们需要先压缩，然后再flush，则可能需要false。
	}

	/**
	 * @ 将对象序列化 @ param obj @ return byte[] @ create by ostreamBaba on 上午12:35
	 * 18-9-18
	 */

	public static <T> byte[] serialize(T obj) {
		byte[] bytes;
		try {
			bytes = objectMapper.writeValueAsBytes(obj);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		return bytes;
	}

	/**
	 * @ 反序列成对象 @ param data @ param clazz @ return T @ create by ostreamBaba on
	 * 上午12:35 18-9-18
	 */

	public static <T> T deserialize(byte[] data, Class<T> clazz) {
		T obj;
		try {
			obj = objectMapper.readValue(data, clazz);
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		return obj;
	}

	/**
	 * @ 将json转化成对象 @ param json @ param clazz @ return T @ create by ostreamBaba on
	 * 上午12:37 18-9-18
	 */

	public static <T> T jsonToObject(String json, Class<?> clazz) {
		T obj;
		JavaType javaType = objectMapper.getTypeFactory().constructType(clazz);
		try {
			obj = objectMapper.readValue(json, javaType);
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		return obj;
	}

	/**
	 * @ 将json转化成list @ param json @ param collectionClass @ param elementClass @
	 * return T @ create by ostreamBaba on 上午12:38 18-9-18
	 */

	public static <T> T jsonToObjectList(String json, Class<?> collectionClass, Class<?>... elementClass) {
		T obj;
		JavaType javaType = objectMapper.getTypeFactory().constructParametricType(collectionClass, elementClass);
		try {
			obj = objectMapper.readValue(json, javaType);
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		return obj;
	}

	/**
	 * @ 将json转化成Map @ param json @ param keyClass @ param valueClass @ return T @
	 * create by ostreamBaba on 上午12:38 18-9-18
	 */

	public static <T> T jsonToObjectHashMap(String json, Class<?> keyClass, Class<?> valueClass) {
		T obj;
		JavaType javaType = objectMapper.getTypeFactory().constructParametricType(HashMap.class, keyClass, valueClass);
		try {
			obj = objectMapper.readValue(json, javaType);
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		return obj;
	}

	/**
	 * @ 将对象转化成json @ param obj @ return java.lang.String @ create by ostreamBaba on
	 * 上午12:39 18-9-18
	 */

	public static String objectToJson(Object obj) {
		String json;
		try {
			json = objectMapper.writeValueAsString(obj);
		} catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		return json;
	}

	private static Map<String, String> setDesc(String nxpg, String tag, String nxpgValue, String ncms, String ncmsValue,
			boolean sync, boolean byPass) {
		Map<String, String> result = new HashMap<String, String>();
		result.put("nxpg", StrUtil.getValue(nxpg));
		result.put("tag", StrUtil.getValue(tag));
		result.put("nxpgValue", StrUtil.getValue(nxpgValue));
		result.put("ncms", StrUtil.getValue(ncms));
		result.put("ncmsValue", StrUtil.getValue(ncmsValue));

		if (sync)
			result.put("sync", "Y");
		else
			result.put("sync", "N");

		if (byPass)
			result.put("byPass", "Y");
		else
			result.put("byPass", "N");

		return result;
	}

	// json Array 데이터 상세 조회
	// nxpg, ncms는 두곳의 if 명이 들어간다.
	public static void descJSONArray(CollectionProperties properties, String nxpgPreKey, Map<String, String> param,
			String ncms, List<Map<String, String>> resultList, JSONArray jArr1, JSONArray jArr2) {
		if (jArr1 == null)
			return;
		if (jArr2 == null)
			return;

		JSONArray jArrNcms = jArr2;
		List<Map<String, Object>> list = CastUtils.getJSONArrayToMapList(jArrNcms);

		// 날짜 필터링
		// sample : dist_fr_dt|dist_to_dt
		try {
			String[] datefilter = CastUtils.getObjectToMap(properties.getCollections().get(ncms)).get("datefilter").toString().split(",");
			for (int i = 0; i < datefilter.length; i++) {
				String[] filter = datefilter[i].split("\\|");
				if (filter.length == 2)
					DateUtils.getCompare(list, filter[0], filter[1], true);
			}
		} catch (Exception e) {
			System.out.println("Date Filter Error : " + e.toString());
		}
		//////////////////

		// segment 필터링
		// sample : cmpgn_id|seg_id
		try {
			String[] segment = CastUtils.getObjectToMap(properties.getCollections().get(ncms)).get("segment").toString().split("\\|");
			
			// IF-NXPG-005는 segment 필터링을 뺸다
			if ("IF-NXPG-005".equals(param.get("ifname"))) segment = null;
			
			if (segment != null && segment.length == 2) {
				doSegment(list, param.get(segment[1]), segment[0]);
			}
		} catch (Exception e) {
			System.out.println("Segment Filter Error : " + e.toString());
		}
		///////////////////

		jArrNcms = new JSONArray(list);
		try {
			for (int i = 0; i < jArr1.length(); i++) {
				if (jArr1.get(i) instanceof JSONObject) {
					resultList.add(setDesc(nxpgPreKey + "-" + jArr1.length()+"("+(i+1)+")", "", "", "", "", true, true));
					descJSONObject(properties, nxpgPreKey, param, ncms, resultList, jArr1.getJSONObject(i),
							jArrNcms.getJSONObject(i));
				} else {
					boolean sync = jArr1.get(i).equals(jArrNcms.get(i));
					// array안에 키없이 들어가있는 애들은..? 있는지 확인 필요.
					resultList.add(setDesc(param.get("ifname").toString(), "array", jArr1.get(i).toString(), ncms,
							jArrNcms.get(i).toString(), sync, true));
				}
			}
		} catch (Exception e) {
			System.out.println("descJSONArray Error : " + e.toString());
		}
	}

	// json Object 데이터 상세 조회
	public static void descJSONObject(CollectionProperties properties, String nxpgPreKey, Map<String, String> param,
			String ncms, List<Map<String, String>> resultList, JSONObject jObj1, JSONObject jObj2) {
		if (jObj1 == null)
			return;
		if (jObj2 == null)
			return;
		String key = null;
		try {
			Iterator<String> keyList = jObj1.keys();
			while (keyList.hasNext()) {
				key = keyList.next();

				if (jObj1.get(key) instanceof JSONObject) {
					descJSONObject(properties, nxpgPreKey + "." + key, param, ncms, resultList,
							jObj1.getJSONObject(key), jObj2.getJSONObject(key));
				} else if (jObj1.get(key) instanceof JSONArray) {
					// 만약 ncms에 값이 없으면 nxpg에 있는 menu의 크기를 찍어준다.
					if (jObj2.has(key)) {
						if (jObj2.get(key) instanceof JSONArray) {
							descJSONArray(properties, nxpgPreKey + "." + key, param, ncms, resultList, jObj1.getJSONArray(key), jObj2.getJSONArray(key));
						} else {
							resultList.add(setDesc(key + " count :" + jObj1.getJSONArray(key).length(), "", "", "", "", true, true));
						}
					} else {
						resultList.add(setDesc(key + " count :" + jObj1.getJSONArray(key).length(), "", "", "", "", true, true));
					}
				} else {
					boolean sync = true;
					String nxpgValue = null;
					String ncmsValue = null;
										
					nxpgValue = CastUtils.getStringToJSONObject(jObj1, key);
					ncmsValue = CastUtils.getStringToJSONObject(jObj2, key);

					if (nxpgValue == null) {
						if (ncmsValue != null) {
							sync = false;
						}
					} else {
						sync = nxpgValue.equals(ncmsValue);
					}
					if (sync == false)
						System.out.println("key : " + key + " nxpg : " + nxpgValue + " ncms : " + ncmsValue);

					boolean byPass = properties.checkByPass(param.get("ifname").toString(), key);
					resultList.add(setDesc(param.get("ifname").toString(), nxpgPreKey + "." + key,
							nxpgValue, ncms, ncmsValue, sync, byPass));
				}
			}
		} catch (Exception e) {
			System.out.println("descJSONObject Error : " + key + " - " + e.toString());
		}
	}

	// json Array 데이터 비교
	public static boolean compareJSONArray(JSONArray jArr1, JSONArray jArr2) {
		boolean result = true;
		if (jArr1 == null)
			return false;
		if (jArr2 == null)
			return false;

		try {
			for (int i = 0; i < jArr1.length(); i++) {
				if (jArr1.get(i) instanceof JSONObject) {
					if (compareJSONObject(jArr1.getJSONObject(i), jArr2.getJSONObject(i)) == false)
						return false;
				} else {
					if (jArr1.get(i).equals(jArr2.get(i)) == false)
						return false;
				}
			}
		} catch (Exception e) {
			return false;
		}
		return result;
	}

	// json Object 데이터 비교
	public static boolean compareJSONObject(JSONObject jObj1, JSONObject jObj2) {
		boolean result = true;
		if (jObj1 == null)
			return false;
		if (jObj2 == null)
			return false;

		try {
			Iterator<String> keyList = jObj1.keys();
			while (keyList.hasNext()) {
				String key = keyList.next();
				// 버전이 들어가면 넘어간다.
				if (key.indexOf("version") >= 0)
					continue;

				if (jObj1.get(key) instanceof JSONObject) {
					if (compareJSONObject(jObj1.getJSONObject(key), jObj2.getJSONObject(key)) == false)
						return false;
				} else if (jObj1.get(key) instanceof JSONArray) {
					if (compareJSONArray(jObj1.getJSONArray(key), jObj2.getJSONArray(key)) == false)
						return false;
				} else {
					if (jObj1.get(key).equals(jObj2.get(key)) == false)
						return false;
				}

			}
		} catch (Exception e) {
			return false;
		}

		return result;
	}
	
	public static void doSegment(List<Map<String, Object>> menuList, String segmentId, String field_campaign) {
		// menu_cd 제거
		List<Map<String, Object>> deleteListInner = new ArrayList<Map<String, Object>>();

		if (menuList != null) {
			if (segmentId == null || segmentId.isEmpty()) {
				segmentId = "_";
			}

			// segmentId 10개까지만 가지고 오게 끔 처리
			String[] segmentIdList = segmentId.split(",");

			if (segmentIdList == null) {
				return;
			}

			for (int i = 0; i < segmentIdList.length; i++) {
				if (i == 10)
					break;
				segmentId += "," + segmentIdList[i];
			}
			if (segmentId != null && segmentId.length() > 0) {
				segmentId = segmentId.substring(1);
			}

			for (Map<String, Object> menu : menuList) {
				if (menu.containsKey("menu_cd")) {
					menu.remove("menu_cd");
				}

				if (menu.containsKey(field_campaign) && menu.get(field_campaign) != null) {
					if (!StrUtil.isEmpty(menu.get(field_campaign) + "")
							&& !segmentId.contains(menu.get(field_campaign) + "")) {
						deleteListInner.add(menu);
					}
				}
			}

			for (Map<String, Object> del : deleteListInner) {
				menuList.remove(del);
			}
		}
	}
}
