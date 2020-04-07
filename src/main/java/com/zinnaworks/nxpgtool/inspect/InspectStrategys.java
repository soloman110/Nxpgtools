package com.zinnaworks.nxpgtool.inspect;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zinnaworks.nxpgtool.util.CastUtils;

public class InspectStrategys {
	private static InspectFilter<Map<String, Object>>  EXPS_YN_FILTER = (Map<String, Object> map, boolean inspYn) -> {
		if (inspYn) return "Y".equalsIgnoreCase(CastUtils.getObjectToString(map.get("insp_exps_yn")));
		else return "Y".equalsIgnoreCase(CastUtils.getObjectToString(map.get("exps_yn")));
	};
	private static InspectFilter<Map<String, Object>> DIST_STS_FILTER = (Map<String, Object> gridContent, boolean inspYn) -> {
		if(inspYn) 
			return "65".equalsIgnoreCase(CastUtils.getObjectToString(gridContent.get("insp_dist_sts_cd")));
		else 
			return "65".equalsIgnoreCase(CastUtils.getObjectToString(gridContent.get("dist_sts_cd")));
	};
	
	private static class InspectCallbackReplace implements InspectCallback<Map<String, Object>> {
		String fr, to;
		boolean inspflag = false;
		public InspectCallbackReplace(String fr, String to, boolean inspflag) {
			this.fr = fr;
			this.to = to;
			this.inspflag = inspflag;
		}
		@Override
		public void call(Map<String, Object> map, boolean yn) {
			if(inspflag == true) {
				if(yn) replace(fr, to, map);
			} else {
				replace(fr, to, map);
			}
		}
	}
	
	private static  abstract class IterableBase<T> extends InspectStrategy<T, Map<String,Object>> {
		public IterableBase(String field, String[] toDelFields, InspectCallback<Map<String, Object>> cb, InspectFilter<Map<String,Object>> filter) {
			super(field, toDelFields, cb, filter);
		}
		public void loopHandle(List<Map<String,Object>> list, boolean inspYn, InspectFilter<Map<String,Object>> filter) {
			for (Iterator<Map<String, Object>> iterator = list.iterator(); iterator.hasNext();) {
				Map<String, Object> map = CastUtils.getObjectToMap(iterator.next());
				if(map == null || !isInspectData(map)) {
					continue;
				}
				if(!filter.filter(map, inspYn)) { 
					iterator.remove(); 
					continue;
				}
				if(cb != null)
					cb.call(map, inspYn);
				delFileds(map, toDelFields);
			}
		}
		@Override
		protected boolean isInspectData(Map<String, Object> data) {
			return any(data, toDelFields);
		}
	}
	//List타입을 처리하는 template
	private static class ListIterableTemplate extends IterableBase<List<Map<String, Object>>> {
		public ListIterableTemplate(String field, String[] toDelFields, InspectCallback<Map<String, Object>> cb, InspectFilter<Map<String, Object>> filter) {
			super(field, toDelFields, cb, filter);
		}
		@Override
		public void handle(List<Map<String, Object>> list, boolean inspYn) {
			if(cb == null) cb = new InspectCallbackReplace("insp_svc_fr_dt", "svc_fr_dt",true); 
			loopHandle(list, inspYn, filter);
		}
	}
	//Map에서 List를 추출하여 처리하는 template
	private static class MapIterableTemplate extends IterableBase<Map<String, Object>>{
		public MapIterableTemplate(String field, String[] toDelFields, InspectCallback<Map<String, Object>> cb, InspectFilter<Map<String, Object>> filter) {
			super(field, toDelFields, cb, filter);
		}
		@Override
		public void handle(Map<String, Object> data, boolean inspYn) {
			List<Map<String, Object>> list = extractList(data, field);
			loopHandle(list, inspYn, filter);
		}
	}
	// 일반 Map 타입을 처리하는 template
	private static abstract class GeneralMapTemplate extends InspectStrategy<Map<String, Object>, Map<String, Object>> {
		String[] inspectFiedls;
		public GeneralMapTemplate(InspectFilter<Map<String, Object>> filter, String... inspectFiedls) {
			super(filter);
			this.inspectFiedls = inspectFiedls;
		}
		public GeneralMapTemplate(String... inspectFiedls) {
			super();
			this.inspectFiedls = inspectFiedls;
		}
		public abstract void handleInspectData(Map<String, Object> t, boolean inspYn);
		
		@Override
		public void handle(Map<String, Object> data, boolean inspYn) {
			if(isInspectData(data)) {
				handleInspectData(data, inspYn);
			}
		}
		@Override
		protected boolean isInspectData(Map<String, Object> data) {
			if(inspectFiedls == null || inspectFiedls.length == 0) {
				return true;
			}
			return any(data, inspectFiedls);
		}
	}
	
	/** INFNXPG003-빅배너 big_banner 
	 	검수여부 Y인경우 - insp_dist_fr_dt 값을 dist_fr_dt로 대체.대체된 값을 통해 현재 시간 내 제공할 배너 정보 사용
	 */
	public static final MapIterableTemplate INFNXPG003 = new MapIterableTemplate("banners",toDelFields("insp_dist_fr_dt"), new InspectCallbackReplace("insp_dist_fr_dt", "dist_fr_dt",true), (Map<String, Object> t, boolean inspYn) ->{ return true;});
	/**
	 INFNXPG004-블록정보 block_block 
	 	검수여부 Y인경우
	 		- insp_exps_yn이 Y인 블록만 노출하며, -
	 		- insp_dist_fr_dt 값을 dist_fr_dt로 대체. 대체된 값을 통해 현재 시간 내 제공할 블록 정보 사용 
	 	검수여부 N인경우 
	 		- exps_yn이 Y인 블록만 노출
	 */
	public static final MapIterableTemplate INFNXPG004_005 = new MapIterableTemplate("blocks",toDelFields("exps_yn", "insp_dist_fr_dt","insp_exps_yn"),new InspectCallbackReplace("insp_dist_fr_dt", "dist_fr_dt",true), EXPS_YN_FILTER);
	/** INFNXPG005-그리드배너 grid_banner과 동일 */
	public static final MapIterableTemplate INFNXPG005 = new MapIterableTemplate("banners",toDelFields("exps_yn", "insp_dist_fr_dt", "insp_exps_yn"),new InspectCallbackReplace("insp_dist_fr_dt", "dist_fr_dt",true), EXPS_YN_FILTER);
	/**
	 INFNXPG006-그리드정보-grid_contents 
	 	검수여부 Y 
	 		- insp_dist_sts_cd 65인 값만 노출하며 -
			- insp_svc_fr_dt 값을 svc_fr_dt 값으로 대체(대체된 값을 통해 현재 시간 내 제공할 그리드 정보 사용) 
		검수여부 N - dist_sts_cd 65 인 값만 노출
	 */
	public static final ListIterableTemplate INFNXPG006 = new ListIterableTemplate("banners",toDelFields("dist_sts_cd", "insp_dist_sts_cd","insp_svc_fr_dt"),null, DIST_STS_FILTER);
	/**
	 INFNXPG006_CW -그리드정보-grid_contents_item 
	 	검수여부 구분없이 필드 insp_svc_fr_dt, insp_dist_sts_cd, dist_sts_cd 삭제 
	 */
	public static final GeneralMapTemplate INFNXPG006_CW = new GeneralMapTemplate() {
		@Override
		public void handleInspectData(Map<String, Object> content, boolean inspYn) {
			delFileds(content, "insp_svc_fr_dt", "insp_dist_sts_cd", "dist_sts_cd");
		}
	};
	/**
	 INFNXPG009-->synopsis_gateway(게이트웨이 시놉시스)
		검수여부Y인경우 - insp_svc_fr_dt 값을 svc_fr_dt로 대체
	 */
	public static final GeneralMapTemplate INFNXPG009 = new GeneralMapTemplate("insp_svc_fr_dt") {
		@Override
		public void handleInspectData(Map<String, Object> synopsisGw, boolean inspYn) {
			if(inspYn) {
				replace("insp_svc_fr_dt", "svc_fr_dt", synopsisGw);
			}
			delFileds(synopsisGw, "insp_svc_fr_dt");
		}
	};
	/**
	INFNXPG020-->menu_kidsCharacter 키즈존 캐릭터 메뉴(전체보기 포함)
		검수여부Y인경우
			- insp_exps_yn이 Y인 메뉴만 노출하며,
			- insp_dist_fr_dt 값을 dist_fr_dt로 대체. 대체된 값을 통해 현재 시간 내 제공할 메뉴 정보 사용
		검수여부 N인경우 - exps_yn이 Y인 메뉴만 노출
	*/
	public static final ListIterableTemplate INFNXPG020 = new ListIterableTemplate(null,toDelFields("exps_yn", "insp_exps_yn", "insp_dist_fr_dt"),new InspectCallbackReplace("insp_dist_fr_dt", "dist_fr_dt",true), EXPS_YN_FILTER);
	/**
	 INFNXPG015-구매정보(구매버튼 정보) contents_purchares
		검수여부Y인경우 -모든정보 제공
		검수여부N인경우 -matl_sts_cd 50,92만제공
	 */
	public static final ListIterableTemplate INFNXPG015 = new ListIterableTemplate(null,toDelFields("matl_sts_cd"),(kidsCharacter, inspYn)-> {}, 
		(Map<String, Object> contentsPurchares, boolean inspYn)->{
			if (inspYn) return true;
			else {
				String matl_sts_cd = CastUtils.getObjectToString(contentsPurchares.get("matl_sts_cd"));
				return "50".equals(matl_sts_cd) || "92".equals(matl_sts_cd);
		}
	});
	/**
	INFNXPG010-->synopsis_commerce(VOD+관련상품 시놉시스)
		검수여부Y인경우 - insp_svc_fr_dt 값을 svc_fr_dt로 대체
	 */
	public static final GeneralMapTemplate INFNXPG010 = new GeneralMapTemplate("insp_svc_fr_dt") {
		@Override
		public void handleInspectData(Map<String, Object> synopsisCommerce, boolean inspYn) {
			if(inspYn) replace("insp_svc_fr_dt", "svc_fr_dt", synopsisCommerce);
			delFileds(synopsisCommerce, "insp_svc_fr_dt");
		}
	};
	/**
	INFNXPG008-->synopsis_srisInfo (단편/회차 시놉시스(타이틀/시즌)) 
		검수여부 Y인경우
    		- insp_dist_sts_cd 값이 65면 insp_svc_fr_dt 값을 svc_fr_dt로 대체
    		- products 안에 matl_sts_cd 모두 제공
    		- epsd_rslu_info 모든정보 제공
  		검수여부 N인경우 - 무시
    		products 안에 matl_sts_cd 50, 92만 제공
    		epsd_rslu_info 안에 matl_sts_cd 50,92만제공
	 * */
	public static final GeneralMapTemplate INFNXPG008 = new GeneralMapTemplate("insp_svc_fr_dt", "insp_dist_sts_cd") //데이터 중에 insp_svc_fr_dt 또 insp_dist_sts_cd 필드 없으면 처리안함..
	{
		@Override
		public void handleInspectData(Map<String, Object> synopsisSrisInfo, boolean inspYn) {
			if(inspYn && "65".equals(CastUtils.getObjectToString(synopsisSrisInfo.get("insp_dist_sts_cd")))) 
				replace("insp_svc_fr_dt", "svc_fr_dt", synopsisSrisInfo);
			delFileds(synopsisSrisInfo, "insp_svc_fr_dt", "insp_dist_sts_cd");
			//Product 처리  
			List<Map<String, Object>> products = extractList(synopsisSrisInfo, "products");
			for (Iterator<Map<String, Object>> iterator = products.iterator(); iterator.hasNext();) {
					Map<String, Object> product = CastUtils.getObjectToMap(iterator.next());
					if(product == null || !product.containsKey("matl_sts_cd")) //해당 태그 없으면,그냥 넘어간다..
						continue;
					if (!inspYn) {
						String matl_sts_cd = CastUtils.getObjectToString(product.get("matl_sts_cd"));
						if(!"50".equals(matl_sts_cd) && !"92".equals(matl_sts_cd)) {
							iterator.remove();
							continue;
						}
					}
					delFileds(product, "matl_sts_cd");
			}
			//epsd_rslu_info 처리 
			List<Map<String, Object>> epsd_rslu_info = extractList(synopsisSrisInfo, "epsd_rslu_info");
			for (Iterator<Map<String, Object>> iterator = epsd_rslu_info.iterator(); iterator.hasNext();) {
					Map<String, Object> info = CastUtils.getObjectToMap(iterator.next());
					if(info == null || !info.containsKey("matl_sts_cd")) //해당 태그 없으면,그냥 넘어간다..
						continue;
					if(!inspYn) {
						String matl_sts_cd = CastUtils.getObjectToString(info.get("matl_sts_cd"));
						if(!"50".equals(matl_sts_cd) && !"92".equals(matl_sts_cd)) {
							iterator.remove();
						}
					}
				}
			}
	};
	/**
	 * 
	 * 주의 특별히 처리한다..
	 * 
	INFNXPG012-예고편영역-contents_preview
		검수여부Y인경우 -모든정보 제공
 		검수여부N인경우 -matl_sts_cd 50만제공
	 */
	public static final GeneralMapTemplate INFNXPG012 = new GeneralMapTemplate(
		(Map<String, Object> content, boolean inspYn) -> {
			if (inspYn) return true;
			else return "50".equals(CastUtils.getObjectToString(content.get("matl_sts_cd")));
	}) {
		@Override
		public void handleInspectData(Map<String, Object> contentsPreview, boolean inspYn) {
			List<Map<String, Object>> previewList = CastUtils.getObjectToMapList(contentsPreview.get("preview"));
			List<Map<String, Object>> specialList = CastUtils.getObjectToMapList(contentsPreview.get("special"));
			if(previewList != null) 
				filterAndDelete(inspYn, previewList);
			if(specialList != null) 
				filterAndDelete(inspYn, specialList);
		}

		private void filterAndDelete(boolean inspYn, List<Map<String, Object>> list) {
			for (Iterator<Map<String, Object>> iterator = list.iterator(); iterator.hasNext();) {
				Map<String, Object> preview = CastUtils.getObjectToMap(iterator.next());
				if(preview == null || !preview.containsKey("matl_sts_cd")) //해당 태그 없으면,그냥 넘어간다..
					continue;
				if(!filter.filter(preview, inspYn)) {
					iterator.remove();
					continue;
				}
				delFileds(preview, "matl_sts_cd");
			}
		}
	};
	/**
	INFNXPG025-시리즈 회차 기본정보
		검수여부Y인경우
   			- insp_dist_sts_cd 65인 값만 노출
   			- insp_svc_fr_dt값은 svc_fr_dt로 대체
 		검수여부N인경우 - dist_sts_cd 65,80인 값만 노출
	 */
	public static final ListIterableTemplate INFNXPG025 = new ListIterableTemplate(null,toDelFields("insp_svc_fr_dt", "insp_dist_sts_cd"), null,
		(Map<String, Object> content, boolean inspYn)->{ //filter
			if (inspYn) {
				return "65".equals(CastUtils.getObjectToString(content.get("insp_dist_sts_cd")));
			}
			else {
				String dist_sts_cd = CastUtils.getObjectToString(content.get("dist_sts_cd"));
				return "65".equals(dist_sts_cd) || "80".equals(dist_sts_cd);
			}
		}
	);
	/**
	 INFNXPG007-->synopsis_contents(공통 시놉시스(타이틀/시즌))
		검수여부 Y인경우 - insp_dist_sts_cd 값이 65면 insp_svc_fr_dt 값을 svc_fr_dt로 대체
		검수여부 N인경우 - 무시
	 * */
	public static final GeneralMapTemplate INFNXPG007 = new GeneralMapTemplate("insp_dist_sts_cd") {
		@Override
		public void handleInspectData(Map<String, Object> content, boolean inspYn) {
			if(inspYn && "65".equals(CastUtils.getObjectToString(content.get("insp_dist_sts_cd")))) 
				replace("insp_svc_fr_dt", "svc_fr_dt", content);
			delFileds(content, "insp_svc_fr_dt", "insp_dist_sts_cd");
		}
	};
	/**
	 INFNXPG021--> synopsis_liveChildStory(살아있는 동화 시놉시스) 
	 	- insp_dist_sts_cd 필드 삭제.
	 * */
	public static final GeneralMapTemplate INFNXPG021 = new GeneralMapTemplate("insp_dist_sts_cd") {
		@Override
		public void handleInspectData(Map<String, Object> content, boolean inspYn) {
			delFileds(content, "insp_dist_sts_cd");
		}
	};
	
	private static void replace(String fieldFrom, String fieldTo, Map<String, Object> data) {
		if (data != null) data.put(fieldTo, data.get(fieldFrom));
	}
	private static void delFileds(Map<String, Object> data, String... fields) {
		if(fields == null || fields.length == 0) return;
		if (data != null) {
			for (String field : fields) {
				data.remove(field);
			}
		}
	}
	private static List<Map<String, Object>> extractList(Map<String, Object> map, String key) {
		List<Map<String, Object>> list  = Collections.emptyList();;
		if (map != null && map.get(key) != null) 
			list = CastUtils.getObjectToMapList(map.get(key));
		return list;
	}
	private static String[] toDelFields(String... fields) {
		return fields;
	}
	private static boolean any(Map<String, Object> map, String[] fields) {
		for(String f : fields) {
			if(map.containsKey(f))
				return true;
		}
		return false;
	}
}
