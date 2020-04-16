package com.zinnaworks.nxpgtool.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.zinnaworks.nxpgtool.inspect.InspectHandler;
import com.zinnaworks.nxpgtool.inspect.InspectStrategys;
import com.zinnaworks.nxpgtool.service.FilterService;
import com.zinnaworks.nxpgtool.service.IFService;
import com.zinnaworks.nxpgtool.util.CastUtils;
import com.zinnaworks.nxpgtool.util.DateUtils;
import com.zinnaworks.nxpgtool.util.GridComparator;
import com.zinnaworks.nxpgtool.util.JsonUtil;

@Service
public class FilterServiceImpl implements FilterService {

	private static String commerceProduct = "42";
	private static String ppmProduct = "36";
	
	@Autowired
	IFService ifService;

//	@Autowired
//	private CollectionProperties collectionProperties;

	private Map<String, Object> getSynopsisSrisInfo(Map<String, String> param, String epsd_id) {
		Map<String, Object> result = null;
		Map<String, String> ncmsParam = new HashMap<String, String>(param);
		ncmsParam.put("ifname", "INFNXPG008");
		ncmsParam.put("epsd_id", epsd_id);
		String ncmsResult = ifService.getNcmsData(ncmsParam);
		
		result = CastUtils.StringToJsonMap(ncmsResult);

		InspectHandler.handle(result, InspectStrategys.INFNXPG008, param);
		
		return result;
	}
	
	private List<Map<String, Object>> getContentsSeries(Map<String, String> param) {
		Map<String, String> ncmsParam = new HashMap<String, String>(param);
		ncmsParam.put("ifname", "INFNXPG025");
		String result = ifService.getNcmsData(ncmsParam);
		
		Map<String, Object> temp = CastUtils.StringToJsonMap(result);
		if (temp != null && temp.get("episodes") != null) {
			
			List<Map<String, Object>> seriesList = new ArrayList<Map<String, Object>>();
			seriesList = CastUtils.getObjectToMapList(temp.get("episodes"));
			InspectHandler.handle(seriesList, InspectStrategys.INFNXPG025, param);
			//필터링
			DateUtils.getCompare(seriesList, "svc_fr_dt", "svc_to_dt", false);
			
			return seriesList;
		}
		return null;
	}

	// 인물정보 가지고 오기.
	public void getContentsPeople(Map<String, Object> contents, String epsd_id, Map<String, String> param) {
		Map<String, String> ncmsParam = new HashMap<String, String>(param);
		ncmsParam.put("ifname", "INFNXPG011");
		ncmsParam.put("epsd_id", epsd_id);
		String result = ifService.getNcmsData(ncmsParam);
		
		if (result != null && !"".equals(result)) {
			contents.putAll(CastUtils.StringToJsonMap(result));
		} else {
			contents.put("peoples", null);
		}
	}

	// 코너 정보 가지고 오기.
	public void getContentsCorner(Map<String, Object> contents, String epsd_id, Map<String, String> param) {
		Map<String, String> ncmsParam = new HashMap<String, String>(param);
		ncmsParam.put("ifname", "INFNXPG013");
		ncmsParam.put("epsd_id", epsd_id);
		String result = ifService.getNcmsData(ncmsParam);
		
		if(result != null && !"".equals(result)) {
			Map<String, Object> corners = CastUtils.StringToJsonMap(result);
			contents.put("corners", corners.get("corners"));
		}else {
			contents.put("corners", null);
		}
	}	

	// 예고편/스틸컷/스페셜영상 정보					
	public Map<String, Object> getContentsPreview(Map<String, Object> contents, String sris_id, Map<String, String> param) {
		Map<String, String> ncmsParam = new HashMap<String, String>(param);
		ncmsParam.put("ifname", "INFNXPG012");
		String result = ifService.getNcmsData(ncmsParam);
		
		if (result != null && !"".equals(result)) {
			Map<String, Object> preview = CastUtils.StringToJsonMap(result);
			InspectHandler.handle(preview, InspectStrategys.INFNXPG012, param);
			contents.putAll(preview);
		} else {
			contents.put("preview", null);
		}
		
		return contents;
	}	
	
	// 평점/리뷰 정보
	public void getContentsReview(Map<String, Object> contents, String sris_id, Map<String, String> param) {
		Map<String, String> ncmsParam = new HashMap<String, String>(param);
		ncmsParam.put("ifname", "INFNXPG014");
		String result = ifService.getNcmsData(ncmsParam);
		
		Map<String, Object> root = null;
		if (result != null && !"".equals(result)) {
			root = CastUtils.StringToJsonMap(result);

			Map<String, Object> temp = null;
			List<Map<String, Object>> sites = CastUtils.getObjectToMapList(root.get("sites"));

			for (Map<String, Object> site : sites) {
				temp = null;
				List<Map<String, Object>> reviews = CastUtils.getObjectToMapList(site.get("reviews"));
				if (reviews != null && reviews.size() > 0) {
					temp = reviews.get(0);
				}
				List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
				if (temp != null) {
					list.add(temp);
				}
				site.put("reviews", list);
			}

			contents.put("site_review", root);
		} else {
			contents.put("site_review", null);
		}
	}
	

	@Override
	// 게이트웨이 시놉시스
	public void filterINFNXPG009(Map<String, String> param, Map<String, Object> gwsynop) {
		InspectHandler.handle(gwsynop, InspectStrategys.INFNXPG009, param);
	}
	
	private Map<String, Object> getData(List<Map<String, Object>> list, String key, String value) {
		if (key == null) return null;
		if (value == null) return null;
		if (list == null) return null;
		
		for(int i = 0; i < list.size(); i++) {
			Map<String, Object> temp = list.get(i);
			if (temp.containsKey(key)) {
				if (value.equals(temp.get(key).toString())) {
					return temp;
				}
			}
		}
		
		return null;
	}
	
	@Override
	// VOD+관련상품 시놉시스
	public void filterINFNXPG010(Map<String, String> param, Map<String, Object> commerce) {
		InspectHandler.handle(commerce, InspectStrategys.INFNXPG010, param);

		Map<String, String> ncmsParam = new HashMap<String, String>(param);
		ncmsParam.put("ifname", "INFNXPG023");
		String result = ifService.getNcmsData(ncmsParam);
		List<Map<String, Object>> phraseList = CastUtils.getJSONArrayToMapList(new JSONArray(result));
		
		if (commerce.containsKey("info_id"))
			commerce.put("info_id", getData(phraseList, "stb_exps_phrs_id", commerce.get("info_id").toString()));
		if (commerce.containsKey("delivery_info_id"))
			commerce.put("delivery_info_id", getData(phraseList, "stb_exps_phrs_id", commerce.get("delivery_info_id").toString()));
		if (commerce.containsKey("refund_info_id"))
			commerce.put("refund_info_id", getData(phraseList, "stb_exps_phrs_id", commerce.get("refund_info_id").toString()));
		if (commerce.containsKey("clause_info_id"))
			commerce.put("clause_info_id", getData(phraseList, "stb_exps_phrs_id", commerce.get("clause_info_id").toString()));
	}
	
	@Override
	// PPM+관련상품 시놉시스
	public void filterINFNXPG032(Map<String, String> param, Map<String, Object> ppmCommerce) {
		Map<String, String> ncmsParam = new HashMap<String, String>(param);
		ncmsParam.put("ifname", "INFNXPG023");
		String result = ifService.getNcmsData(ncmsParam);
		List<Map<String, Object>> phraseList = CastUtils.getJSONArrayToMapList(new JSONArray(result));
		
		if (ppmCommerce.containsKey("info_id"))
			ppmCommerce.put("info_id", getData(phraseList, "stb_exps_phrs_id", ppmCommerce.get("info_id").toString()));
		if (ppmCommerce.containsKey("delivery_info_id"))
			ppmCommerce.put("delivery_info_id", getData(phraseList, "stb_exps_phrs_id", ppmCommerce.get("delivery_info_id").toString()));
		if (ppmCommerce.containsKey("refund_info_id"))
			ppmCommerce.put("refund_info_id", getData(phraseList, "stb_exps_phrs_id", ppmCommerce.get("refund_info_id").toString()));
		if (ppmCommerce.containsKey("clause_info_id"))
			ppmCommerce.put("clause_info_id", getData(phraseList, "stb_exps_phrs_id", ppmCommerce.get("clause_info_id").toString()));
	}

	@Override
	// Light 홈 GNB 메뉴 정보
	public void filterINFNXPG033(Map<String, String> param, List<Map<String, Object>> menus) {
		/*
		if ("Y".equals(param.get("is_all"))) {
			// 하위 block 가지고 오기.
			for (int i = 0; i < menus.size(); i++) {
				Map<String, Object> item = menus.get(i);
				Map<String, String> blockParam = new HashMap<String, String>(param);
				Map<String, String> gridParam = new HashMap<String, String>(param);
				
				// 메뉴 아이디 가지고 와서 설정 
				blockParam.put("menu_id", CastUtils.getObjectToString(item.get("menu_id")));
				
				// grid count 세팅
				gridParam.put("page_no", "1");

				if (StrUtil.isEmpty(gridParam.get("page_cnt"))) {
					gridParam.put("page_cnt", "6");
				}
				
				// 기존 003에서 사용하는 함수 사용해서 block정보 가지고 오기.
				Map<String, Object> block = getBlockBlock(ver, blockParam, false);
				if (block != null) {
					// 가지고 온 block 정보에서 필요한 blocks정보 가지고 오기 
					List<Map<String, Object>> blockList = CastUtils.getObjectToMapList(block.get("blocks"));
					if (blockList != null) {
						// 하위 grid 정보 가지고 오기.
						for(int blockIndex = 0; blockIndex < blockList.size(); blockIndex++) {
							Map<String, Object> blockItem = blockList.get(blockIndex);
							// grid 조회용 menu_id가지고 오기.
							gridParam.put("menu_id", CastUtils.getObjectToString(blockItem.get("menu_id")));
							Map<String, Object> gridReturn = new HashMap<String, Object>();
							
							// grid 정보 조회 
							gridService.getGrid(gridReturn, gridParam);
							
							// grid 정보 가공
							List<Map<String, Object>> gridList = CastUtils.getObjectToMapList(gridReturn.get("contents"));
							if (gridList == null) {
								blockItem.put("grid", null);
								blockItem.put("total_count", 0);
							} else {
								blockItem.put("grid", gridList);
								blockItem.put("total_count", CastUtils.getStrToInt(CastUtils.getObjectToString((gridReturn.get("total_count")))));
							}
						}
					}
					item.put("menus", blockList);
				} else {
					item.put("menus", null);
				}
			}
		}
		*/
	}
	

	@Override
	// 키즈존 캐릭터메뉴 정보
	public void filterINFNXPG020(Map<String, String> param, List<Map<String, Object>> kzgnb) {
		
		InspectHandler.handle(kzgnb, InspectStrategys.INFNXPG020, param);

		// 그리드 타이틀 정렬 처리
		List<Map<String, Object>> mCopyGrids = null;
		try {
			String order_type = CastUtils.getObjectToString(param.get("order_type"));
			
			if(!"".equals(order_type)) {
				mCopyGrids = new ArrayList<Map<String, Object>>(kzgnb);
				
				Collections.sort(mCopyGrids, new GridComparator(order_type, true));
				kzgnb = mCopyGrids;
			}
		}
		catch(Exception e){
			
		}
	}

	@Override
	// 살아있는 동화 시놉시스
	public void filterINFNXPG021(Map<String, String> param, Map<String, Object> contents) {
		InspectHandler.handle(contents, InspectStrategys.INFNXPG021, param);
		getContentsCorner(contents, param.get("epsd_id").toString(), param);
	}

	@Override
	// 메뉴 맵핑정보
	public  Map<String, Object> filterINFNXPG019(Map<String, String> param, List<Map<String, Object>> kids) {
		
		Map<String, Object> result = new HashMap<String, Object>();
		
		for(Map<String, Object> kid : kids) {
			// 살아있는 동화 최상위 메뉴 ID 가져오기
			if(("70").equals(kid.get("kidsz_gnb_cd"))) {
				result.put("menu_id", CastUtils.getObjectToString(kid.get("menu_id")));
			}
			// 뽀로로 Talk 최상위 메뉴 ID 가져오기
			if(("80").equals(kid.get("kidsz_gnb_cd"))) {
				result.put("po_menu_id", CastUtils.getObjectToString(kid.get("menu_id")));
			}
		}
		
		return result;
	}
	
	@Override
	// 구매버튼 관련
	public void filterINFNXPG015(Map<String, String> param, Map<String, Object> purchares) {
		if (purchares == null) return;
		
		String rslu_type = CastUtils.getObjectToString(param.get("rslu_typ_cd"));
//		Map<String, String> checkdate = collectionProperties.getCheckdate();
		
		List<Map<String, Object>> products = CastUtils.getObjectToMapList(purchares.get("products"));
		InspectHandler.handle(products, InspectStrategys.INFNXPG015, param);
		DateUtils.getCompare(products, "prd_prc_fr_dt", "purc_wat_to_dt", false);
		
		// ppm_yn, cmc_yn 필터링 
		checkPPMList(products, param);
		
		products = CastUtils.getObjectToMapList(purchares.get("products"));
		
		// purchares 날짜 체크
		for(int i = products.size()-1; i >= 0; i--) {
			Map<String, Object> product = products.get(i);

			// 2019-04-09
			// 해상도 필터링 추가 
			String rslu_typ_cd = CastUtils.getObjectToString(product.get("rslu_typ_cd"));
			
			//진입한 STB의 화질이 콘텐츠의 화질보다 낮을 경우 필터링.(상위 화질은 안보이게 한다.)
			if (rslu_type != null && !rslu_type.isEmpty()
					&& CastUtils.getStrToInt(rslu_typ_cd) > CastUtils.getStrToInt(rslu_type)) {
				products.remove(i);
				continue;
			}
			//////////

			// 여기 필터링을 어떻게 할까?
			/*
			String strTemp = null;

			String brcast_avl_perd_yn = "";
			if (product.containsKey("brcast_avl_perd_yn")) {
				brcast_avl_perd_yn = product.get("brcast_avl_perd_yn") + "";
			}

			// prd_prc_to_dt
			if (product.get("prd_prc_to_dt") != null)
				strTemp = product.get("prd_prc_to_dt").toString();
			if (!"Y".equals(brcast_avl_perd_yn) && DateUtils.checkDate(strTemp, CastUtils.getStrToInt(checkdate.get("prdprctodt"))*-1))
				product.put("expire_prd_prc_dt", strTemp);
			else 
				product.put("expire_prd_prc_dt", "");

			// next_prd_prc_fr_dt
			if (product.get("next_prd_prc_fr_dt") != null)
				strTemp = product.get("next_prd_prc_fr_dt").toString();
			if (!DateUtils.checkDate(strTemp, CastUtils.getStrToInt(checkdate.get("nextprdprcfrdt"))))
				product.put("next_prd_prc_fr_dt", "");
			
			// ppm_orgnz_fr_dt
			if (product.get("ppm_orgnz_fr_dt") != null)
				strTemp = product.get("ppm_orgnz_fr_dt").toString();
			if (!DateUtils.checkDate(strTemp, CastUtils.getStrToInt(checkdate.get("ppmorgnzfrdt"))))
				product.put("ppm_orgnz_fr_dt", "");

			// ppm_orgnz_to_dt
			if (product.get("ppm_orgnz_to_dt") != null)
				strTemp = product.get("ppm_orgnz_to_dt").toString();
			if (!DateUtils.checkDate(strTemp, CastUtils.getStrToInt(checkdate.get("ppmorgnzfrdt"))))
				product.put("ppm_orgnz_to_dt", "");
				*/
		}
		
		purchares.put("purchares", products);
		///////////////////////
	}
	
	@Override
	// 시놉시스 관련(여러 인터페이스를 사용하는데 하나로 다 만들어서 처리했다고 한다.
	public void filterINFNXPG007(Map<String, String> param, Map<String, Object> sris) {
		if (sris == null) return;
		
		String sris_id = "", epsd_id = "";
		Map<String, Object> epsd = null;
		String rslu_type = CastUtils.getObjectToString(param.get("rslu_typ_cd"));
//		Map<String, String> checkdate = collectionProperties.getCheckdate();
		
		epsd_id = param.get("epsd_id");
		
		epsd = getSynopsisSrisInfo(param, epsd_id);
		
		// 처음에 가지고 오는 데이터.
		// sris = getSynopContent(param);
		
		if (sris != null) {
			if ("Y".equals(param.get("yn_recent")) && sris != null && "01".equals(sris.get("sris_typ_cd"))) {
				
				String series = CastUtils.getListToJsonArrayString(getContentsSeries(param));
				if (series != null && !series.isEmpty()) {
					String last_epsd_id = "";
					Pattern p = Pattern.compile(".*epsd_id\":\"([^\"]+)\"");
					Matcher m = p.matcher(series);
					
					if (m.find()) {
						last_epsd_id = m.group(1);
					}
					
					if (!last_epsd_id.isEmpty()) {
						epsd_id = last_epsd_id;
						epsd = getSynopsisSrisInfo(param, epsd_id);
					}
				}
			}
			List<Map<String, Object>> epsd_rslu_info = CastUtils.getObjectToMapList(epsd.get("epsd_rslu_info"));
			
			for (int i = epsd_rslu_info.size()-1; i >= 0; i--) {
				Map<String, Object> map = epsd_rslu_info.get(i);
				String rslu_typ_cd = CastUtils.getObjectToString(map.get("rslu_typ_cd"));
				
				//진입한 STB의 화질이 콘텐츠의 화질보다 낮을 경우 필터링.(상위 화질은 안보이게 한다)
				if (rslu_type != null && !rslu_type.isEmpty()
						&& CastUtils.getStrToInt(rslu_typ_cd) > CastUtils.getStrToInt(rslu_type)) {
					epsd_rslu_info.remove(i);
				}
			}
			
		}
		
		if (epsd == null || sris == null) return;
		
		// ppm_yn, cmc_yn 필터링 
		checkPPMList(epsd.get("products"), param);
		
		sris.putAll(epsd);
		
		sris.remove("relation_contents");
		// 성공
		getContentsPeople(sris, epsd_id, param);
		getContentsCorner(sris, epsd_id, param);
		getContentsPreview(sris, sris_id, param);
		getContentsReview(sris, sris_id, param);

		sris.put("combine_product_yn", "N");
		if (param.containsKey("ukey_prd_id")) {
			List<Map<String, Object>> ukeyList = CastUtils.getObjectToMapList(sris.get("ukey_products"));
			for (Map<String, Object> ukey : ukeyList) {
				if (ukey.containsKey("ukey_prd_id")) {
					if (ukey.get("ukey_prd_id").equals(param.get("ukey_prd_id"))) {
						sris.put("combine_product_yn", "Y");
					}
				}
			}
		}

		sris.put("series_info", getContentsSeries(param)); //검수 추가 INFNXPG025 시리즈 회차 기본 정보.
		sris.put("session_id",  CastUtils.getObjectToString(param.get("session_id")));
		sris.put("track_id", CastUtils.getObjectToString(param.get("track_id")));
		sris.put("menu_id", CastUtils.getObjectToString(param.get("cur_menu")));
		sris.put("cw_call_id", CastUtils.getObjectToString(param.get("cw_call_id")));
		
		// String tempEnding = redisClient.hget("ending_cwcallidval", "ending_cwcallidval", param);
		Map<String, String> ncmsParam = new HashMap<String, String>(param);
		ncmsParam.put("ifname", "INFNXPG029");
		String tempEnding = ifService.getNcmsData(ncmsParam);
		
		Map<String, Object> tempEndingMap = CastUtils.StringToJsonMap(tempEnding);
		sris.put("ending_cw_call_id_val", null);
		if (tempEndingMap != null && tempEndingMap.get("cw_call_id_val") != null) {
			sris.put("ending_cw_call_id_val", tempEndingMap.get("cw_call_id_val"));
		}
		
		// products 날짜 체크
		if (sris != null) {
			List<Map<String, Object>> products = CastUtils.getObjectToMapList(sris.get("products"));
			DateUtils.getCompare(products, "prd_prc_fr_dt", "purc_wat_to_dt", false);
			if (products != null) {
				
				for (int i = products.size()-1; i >= 0; i--) {
					Map<String, Object> product = products.get(i);	
					
					// 2019-04-09
					// 해상도 필터링 추가 
					String rslu_typ_cd = CastUtils.getObjectToString(product.get("rslu_typ_cd"));
					
					//진입한 STB의 화질이 콘텐츠의 화질보다 낮을 경우 필터링.(상위 화질은 안보이게 한다)
					if (rslu_type != null && !rslu_type.isEmpty()
							&& CastUtils.getStrToInt(rslu_typ_cd) > CastUtils.getStrToInt(rslu_type)) {
						products.remove(i);
						continue;
					}
					//////////
					
					// 여기 필터링 어떻게 할까?
					/*
					String brcast_avl_perd_yn = "";
					if (product.containsKey("brcast_avl_perd_yn")) {
						brcast_avl_perd_yn = product.get("brcast_avl_perd_yn") + "";
					}
					
					String strTemp = null;
					// prd_prc_to_dt
					if (product.get("prd_prc_to_dt") != null)
						strTemp = product.get("prd_prc_to_dt").toString();
					if (!"Y".equals(brcast_avl_perd_yn) && DateUtils.checkDate(strTemp, CastUtils.getStrToInt(checkdate.get("prdprctodt"))*-1))
						product.put("expire_prd_prc_dt", strTemp);
					else 
						product.put("expire_prd_prc_dt", "");
					
					// next_prd_prc_fr_dt
					if (product.get("next_prd_prc_fr_dt") != null)
						strTemp = product.get("next_prd_prc_fr_dt").toString();
					if (!DateUtils.checkDate(strTemp, CastUtils.getStrToInt(checkdate.get("nextprdprcfrdt"))))
						product.put("next_prd_prc_fr_dt", "");
					*/
				}
			}
		}
	}
	
	@Override
	public Map<String, Object> filterINFNXPG004(Map<String, String> param, boolean LocFilterYn,
			Map<String, Object> blockblock) {

		boolean chInfoFilterYn = !LocFilterYn;

		if (blockblock != null && blockblock.get("blocks") != null) {

			List<Map<String, Object>> blocks = CastUtils.getObjectToMapList(blockblock.get("blocks"));
//			List<Map<String, Object>>cwResult = new ArrayList<Map<String,Object>>();
			// 날짜 및 seg는 공통으로 처리하는 곳이 있음.
//			DateUtils.getCompare(blocks, "dist_fr_dt", "dist_to_dt", true);
//			doSegment(blocks, param.get("seg_id"), "cmpgn_id");

			List<Map<String, Object>> deleteList = new ArrayList<Map<String, Object>>();

			// cw 안함.
//			Map<String, Object> cw = properties.getCw();

//			boolean firstCW = false;
//			int j = 0; //502가 편성된 위치 찾기 용
//			int k = 0; //cw추가된 메뉴 계산용

			Map<Integer, List<Map<String, Object>>> whichMap = new HashMap<Integer, List<Map<String, Object>>>();

			// exps_yn가 Y인것만 노출
			valueFilter(blocks, "exps_yn", "Y");

			for (Iterator<Map<String, Object>> iterator = blocks.iterator(); iterator.hasNext();) {
//				cwResult = new ArrayList<Map<String,Object>>();
				Map<String, Object> map = CastUtils.getObjectToMap(iterator.next());
				if (LocFilterYn) {
					locCdFilter(map, param);
				}
				if (chInfoFilterYn) {
					chInfoDelete(map);
				}

				/*
				 * //CW menu로 대체한다. if(cw.get("scnmthdcd").equals(map.get("scn_mthd_cd"))) {
				 * 
				 * 
				 * iterator.remove(); //CW를 호출하는 맵파일은 삭제한다. k++;
				 * 
				 * if(!firstCW) { List<String> menuData = cwCall(param); Map<String, Object>
				 * keyAndValue; if (menuData != null) { for(String temp:menuData) { try {
				 * keyAndValue = CastUtil.getObjectToMap(cw.get("block")); Map<String, Object>
				 * cwRtn = new HashMap<String, Object>(); cwRtn.putAll(keyAndValue);
				 * 
				 * temp=temp.replaceAll("\\|", "\\@");
				 * 
				 * String [] menuNtitle = temp.split("\\@");
				 * 
				 * // MenuId, Title이 없으면 그냥 넘어간다. if (StrUtil.isEmpty(menuNtitle[0])) break; if
				 * (StrUtil.isEmpty(menuNtitle[1])) break;
				 * 
				 * //그리드 콘텐츠에 존재하면서 만료일이 넘지 않은 경우 메뉴를 노출시킨다. 이외에는 노출시키지 않음 String cwPerGridStr =
				 * (String)redisClient.hget(NXPGCommon.GRID_CONTENTS, menuNtitle[0], param);
				 * 
				 * if(cwPerGridStr != null && !cwPerGridStr.isEmpty()) {
				 * List<Map<String,Object>> cwPerGrid = null; Map<String ,Object> cwPerMap =
				 * null; cwPerMap = CastUtil.StringToJsonMap(cwPerGridStr); // id_package 처리 //
				 * CastUtil.checkPackAgeList(cwPerMap.get("contents"), param);
				 * 
				 * cwPerGrid = CastUtil.getObjectToMapList(cwPerMap.get("contents"));
				 * List<Map<String, Object>> data = (List<Map<String, Object>>)
				 * CastUtil.getObjectToMapList(cwPerGrid);
				 * 
				 * //주의 .....검수 정보를 여기서 제거 ..... InspectHandler.handle(data,
				 * InspectStrategys.INFNXPG006, param);
				 * 
				 * DateUtil.getCompare(data, "dist_fr_dt", "dist_to_dt", true); if(data != null
				 * && !data.isEmpty()) { cwRtn.put("menu_id", menuNtitle[0]);
				 * cwRtn.put("menu_nm", menuNtitle[1]); cwRtn.put("dist_to_dt",
				 * map.get("dist_to_dt")); cwRtn.put("gnb_typ_cd", map.get("gnb_typ_cd"));
				 * cwRtn.put("dist_fr_dt", map.get("dist_fr_dt")); cwRtn.put("menu_nm_exps_yn",
				 * map.get("menu_nm_exps_yn")); cwRtn.put("scn_mthd_cd", "10");
				 * cwRtn.put("menus", null);
				 * 
				 * cwResult.add(cwRtn); }
				 * 
				 * whichMap.put(j, cwResult); } } catch (Exception e) { break; } } } firstCW =
				 * true; } }
				 */
//				j++;	//502 위치 찾기 용

				map.put("menus", null);

				if ("20".equals(map.get("blk_typ_cd")) && "N".equals(map.get("is_leaf"))) {

					Map<String, Object> gridbanner = getGridBanner(param.get("menu_stb_svc_id"), map.get("menu_id").toString());

					if (gridbanner != null) {
						if (gridbanner.containsKey("banners")) {
							DateUtils.getCompare(CastUtils.getObjectToMapList(gridbanner.get("banners")), "dist_fr_dt",
									"dist_to_dt", true);
							JsonUtil.doSegment(CastUtils.getObjectToMapList(gridbanner.get("banners")),
									param.get("seg_id"), "cmpgn_id");

							// exps_yn가 Y인것만 노출
							valueFilter(CastUtils.getObjectToMapList(gridbanner.get("banners")), "exps_yn", "Y");

							map.put("menus", CastUtils.getObjectToMapList(gridbanner.get("banners")));
						}
					} else {
						deleteList.add(map);
					}
				}
				map.put("evt_use_yn", eventYn(map) ? "Y" : "N");
			}

			for (int o = 0; o < blocks.size(); o++) {
				if (whichMap.containsKey(o)) {
					blocks.addAll(o, whichMap.get(o));

//					totalCount += whichMap.get(o).size();
				}
			}

			// 005에서 데이터 없으면 삭제.
			for (Map<String, Object> del : deleteList) {
				if (blocks.contains(del)) {
					blocks.remove(del);
				}
			}
			int totalCount = blocks.size();

			blockblock.put("block_count", totalCount);
			blockblock.remove("total_count");
		} else {
			blockblock.put("block_count", 0);
			blockblock.remove("total_count");
		}

		return blockblock;
	}

	// INFNXPG004 + IF-NXPG-005
	// 월정액은 block에 따로 추가 기능이 있어서 이렇게 해둔다.
	@Override
	public void filterINFNXPG004Month(Map<String, String> param, Map<String, Object> blockblock) {

		List<Map<String, Object>> newBlocks = new ArrayList<Map<String, Object>>();
		Map<String, Object> shcutblockblock = null;
		List<Map<String, Object>> blocks = CastUtils.getObjectToMapList(blockblock.get("blocks"));
		// 여기서 안해도 되는데. 아래 로직 최대한 덜 타도록 하기 위해 넣어둠.
		DateUtils.getCompare(blocks, "dist_fr_dt", "dist_to_dt", true);

		Map<String, Boolean> exceptionPid = new HashMap<String, Boolean>();

		String[] tempArr = null;
		List<String> lstUserInputMonth = new ArrayList<String>();
		if (param.containsKey("prd_prc_id_lst") && param.get("prd_prc_id_lst") != null
				&& !param.get("prd_prc_id_lst").isEmpty()) {
			tempArr = param.get("prd_prc_id_lst").split(",");
		}
		if (tempArr != null) {
			for (String pid : tempArr) {
				if (!lstUserInputMonth.contains(pid)) {
					lstUserInputMonth.add(pid);
				}
				exceptionPid.put(pid, true);
			}
		}

		// 월정액 정보를 가지고 온다.
		String ncmsResult = null;
		Map<String, String> ncmsParam = new HashMap<String, String>();
		ncmsParam.put("ifname", "INFNXPG024");
		ncmsParam.put("menu_stb_svc_id", param.get("menu_stb_svc_id"));
		ncmsResult = ifService.getNcmsData(ncmsParam);
		////////////////
		
		List<Object> monthList = CastUtils.StringToJsonList(ncmsResult);
		List<Map<String, Object>> user_month = new ArrayList<Map<String, Object>>();
		if (tempArr != null && monthList != null) {

			for (String userInputMonth : lstUserInputMonth) {

				for (Object month : monthList) {

					Map<String, Object> tempMonth = CastUtils.getObjectToMap(month);

					if (tempMonth.get("prd_prc_id") != null) {

						String prd_prc_id = CastUtils.getObjectToString(tempMonth.get("prd_prc_id"));
						if (userInputMonth.equals(prd_prc_id)) {
							exceptionPid.put(prd_prc_id, true);

							List<Map<String, Object>> lowrank = CastUtils
									.getObjectToMapList(tempMonth.get("low_rank_products"));

							// low_rank_products_type == 02 경우 상위상품을 노출
							// low_rank_products_type : 01 - 하위상품의 총 합이 곧 상위상품이므로 하위를 노출
							// low_rank_products_type : 02 - 부모자식관계이므로 상위상품을 노출
							if (lowrank != null && lowrank.size() > 0) {

								if (tempMonth.containsKey("low_rank_products_type")
										&& "02".equals(tempMonth.get("low_rank_products_type"))) {

									Map<String, Object> tempMonthNoLowRank = tempMonth;
									tempMonthNoLowRank.put("low_rank_products", "");
									if (!user_month.contains(tempMonthNoLowRank)) {
										user_month.add(tempMonthNoLowRank);
									}

								}

								for (Map<String, Object> low : lowrank) {
									if (!tempMonth.containsKey("low_rank_products_type")
											|| (tempMonth.containsKey("low_rank_products_type")
													&& "01".equals(tempMonth.get("low_rank_products_type")))) {
										if (!user_month.contains(low)) {
											user_month.add(low);
										}
									}
									String low_prd_prc_id = CastUtils.getObjectToString(low.get("prd_prc_id"));
									exceptionPid.put(low_prd_prc_id, true);
								}

							} else {
								if (!user_month.contains(tempMonth)) {
									user_month.add(tempMonth);
								}
							}
							break;
							// blockblock.put("block_count", blockblock.get("total_count"));
							// blockblock.remove("total_count");
						} else {
							continue;
						}

					}
				}
			}

			// 가입 월정액 리스트 필터 추가
			DateUtils.getCompareMonth(user_month, "dist_fr_dt", "dist_to_dt", "purc_wat_to_dt", true);
			/////////

			for (Map<String, Object> month_item : user_month) {
				// 2019-07-09 하위 리스트 삭제 리스트에 추가.
				if (month_item.containsKey("drv_prd_prc_id")) {
					List<Map<String, Object>> prdPrcIdList = CastUtils
							.getObjectToMapList(month_item.get("drv_prd_prc_id"));
					if (prdPrcIdList != null) {
						for (Map<String, Object> prdPrcId : prdPrcIdList) {
							String strPrdPrcId = CastUtils.getObjectToString(prdPrcId.get("prd_prc_id"));
							if (strPrdPrcId != null)
								exceptionPid.put(strPrdPrcId, true);
						}
					}
					// 삭제 리스트에 추가 후 필드 삭제
					month_item.remove("drv_prd_prc_id");
				}
				//////////////

				// 벳지 달기(reserve)
				String strPrdTypCd = CastUtils.getObjectToString(month_item.get("prd_typ_cd"));
				Date purcFrDt = DateUtils
						.getStringToDateTime(CastUtils.getObjectToString(month_item.get("purc_wat_fr_dt")));
				Date now = DateUtils.getStringToDateTime(DateUtils.getYYYYMMDDhhmmss2());
				// 테스트 데이터...
				// purcFrDt = DateUtil.getStringToDateTime("20190709110000");
				// 기간권 여부 확인
				if ("32".equals(strPrdTypCd)) {
					// 시청 시간 확인
					if (now.before(purcFrDt)) {
						month_item.put("i_img_cd", "reserve");
					}
				}
				//////////////

				shcutblockblock = getGridBanner(param.get("menu_stb_svc_id"), month_item.get("shcut_menu_id").toString());
				if (shcutblockblock != null && !shcutblockblock.isEmpty()) {
					List<Map<String, Object>> shcutblocks = CastUtils
							.getObjectToMapList(shcutblockblock.get("banners"));
					DateUtils.getCompare(shcutblocks, "dist_fr_dt", "dist_to_dt", true);
					if (shcutblocks != null && shcutblocks.size() > 0) {
						// mmtf_home_exps_yn (홈 노출여부)가 Y인 데이터만 노출한다.
						for (Map<String, Object> temp : shcutblocks) {
							String homeShowYn = CastUtils.getObjectToString(temp.get("mmtf_home_exps_yn"));
							if ("Y".equalsIgnoreCase(homeShowYn)) {
								newBlocks.add(temp);
							}
						}
					}
				}
			}
		}
		Map<Integer, List<Map<String, Object>>> whichMap = new HashMap<Integer, List<Map<String, Object>>>();

		int j = 0;
		for (Iterator<Map<String, Object>> iterator = blocks.iterator(); iterator.hasNext();) {

			Map<String, Object> map = CastUtils.getObjectToMap(iterator.next());
			chInfoDelete(map);
			map.put("menus", null);

			if ("btv020".equals(map.get("call_url"))) {
				map.put("menus", user_month);
			}
			if ("505".equals(map.get("scn_mthd_cd"))) {
				iterator.remove();

				whichMap.put(j, newBlocks);
			}

			j++;

			if ("20".equals(map.get("blk_typ_cd")) && "N".equals(map.get("is_leaf"))) {

				Map<String, Object> gridbanner = getGridBanner(param.get("menu_stb_svc_id"), map.get("menu_id").toString());
				if (gridbanner != null) {
					List<Map<String, Object>> menubanners = CastUtils.getObjectToMapList(gridbanner.get("banners"));

					List<Map<String, Object>> removeTemp = new ArrayList<Map<String, Object>>();
					for (int i = 0; i < menubanners.size(); i++) {
						Map<String, Object> temp = menubanners.get(i);

						if (temp.get("prd_prc_id") != null && !"".equals(temp.get("prd_prc_id"))) {
							if (exceptionPid.containsKey(temp.get("prd_prc_id"))) {
//									menubanners.remove(i);
								removeTemp.add(temp);
							}
						}
					}

					for (Map<String, Object> rt : removeTemp) {
						menubanners.remove(rt);
					}

					DateUtils.getCompare(menubanners, "dist_fr_dt", "dist_to_dt", true);
					map.put("menus", menubanners);
				}
			}
		}

		for (int o = 0; o < blocks.size(); o++) {
			if (whichMap.containsKey(o)) {
				blocks.addAll(o, whichMap.get(o));
			}
		}
	}

	@Override
	public Map<String, Object> filterINFNXPG005(Map<String, Object> event) {
		// exps_yn이 Y 인것만 노출
		valueFilter(CastUtils.getObjectToMapList(event.get("banners")), "exps_yn", "Y");
		return event;
	}

	@Override
	public Map<String, Object> filterINFNXPG006(Map<String, Object> grid) {
		// dist_sts_cd가 65 인것만 노출
		valueFilter(CastUtils.getObjectToMapList(grid.get("contents")), "dist_sts_cd", "65");
		return grid;
	}

	@Override
	public void filterINFNXPG014(Map<String, String> param, Map<String, Object> review) {
		List<Map<String, Object>> sites = CastUtils.getObjectToMapList(review.get("sites"));
		valueFilter(sites, "site_cd", param.get("site_cd"));
		// site를 하나 가지고와서 하나만 넣어준다.
		if (sites != null && sites.size() >= 1)
			review.put("sites", CastUtils.getObjectToMapList(review.get("sites")).get(0));
	}

	private void locCdFilter(Map<String, Object> map, Map<String, String> param) {
		// chnl_grp(List) -> channels(List) -> loc_cd
		if (!param.containsKey("loc_cd") || StringUtils.isBlank(param.get("loc_cd"))) {
			return;
		}

		if (!Pattern.matches("([a-zA-Z]+=\\d+(\\.\\d+)?\\^?)+", param.get("loc_cd"))) {
			throw new IllegalArgumentException("loc_cd format is invalid");
		}
		;

		List<Map<String, Object>> chGroupList = CastUtils.getObjectToMapList(map.get("chnl_grp"));
		if (chGroupList == null || chGroupList.size() == 0) {
			return;
		}
		// 전달받은 지역 코드 정보 추출 format: mbc=1^kbs=52^sbs=61^uhdmbc=231^uhdkbs=200^uhdsbs=261
		Set<Integer> reqLcd = Arrays.asList(param.get("loc_cd").split("\\^")).stream().filter((v) -> {
			return !v.trim().equals("");
		}).map((cd) -> {
			String[] cds = cd.split("=");
			return CastUtils.getStrToInt(cds[1]);
		}).collect(Collectors.toSet());

		chGroupList.forEach((chGroupInfo) -> {
			List<Map<String, Object>> channelsList = CastUtils.getObjectToMapList(chGroupInfo.get("channels"));
			if (channelsList != null && channelsList.size() != 0) {
				channelsList = channelsList.stream().filter((chInfo) -> {
					int lcd = CastUtils.getStrToInt(chInfo.get("loc_cd").toString());
					if (lcd == 0 || reqLcd.contains(lcd))
						return true;
					else
						return false;
				}).sorted(Comparator.comparing((chMap) -> {
					return CastUtils.getStrToInt(chMap.get("sort_seq").toString());
				})).collect(Collectors.toList());

				if (channelsList == null)
					channelsList = Collections.emptyList();
				chGroupInfo.put("channels", channelsList);
			}
		});
	}

	private void chInfoDelete(Map<String, Object> map) {
		if (map.containsKey("chnl_grp")) {
			map.remove("chnl_grp");
		}
	}

	private boolean eventYn(Map<String, Object> block) {
		if (block.get("evt_use_yn") == null) {
			return false;
		}
		String evtUseYn = block.get("evt_use_yn").toString();
		if (StringUtils.isBlank(evtUseYn) || "N".equals(evtUseYn.toUpperCase())
				|| !"30".equals(block.get("blk_typ_cd"))) {
			return false;
		}
		Date lastEvtDt = DateUtils.getStringToDateTime(block.get("last_evt_dist_to_dt").toString());
		Date now = DateUtils.getStringToDateTime(DateUtils.getYYYYMMDDhhmmss2());
		return now.before(lastEvtDt);
	}

	// key값이 value가 아니면 삭제처리 한다.
	private void valueFilter(List<Map<String, Object>> list, String key, String value) {
		if (list == null)
			return;
		List<Map<String, Object>> deleteList = new ArrayList<Map<String, Object>>();

		for (int i = 0; i < list.size(); i++) {
			Map<String, Object> map = list.get(i);

			if (map.containsKey(key)) {
				if (!value.equals(map.get(key).toString()))
					deleteList.add(map);
			}
		}

		for (int i = 0; i < deleteList.size(); i++) {
			list.remove(deleteList.get(i));
		}
	}

	private Map<String, Object> getGridBanner(String menu_stb_svc_id, String menu_id) {
		Map<String, Object> result = null;
		String ncmsResult = null;
		Map<String, String> param = new HashMap<String, String>();

		param.put("ifname", "INFNXPG005");
		param.put("menu_stb_svc_id", menu_stb_svc_id);
		param.put("menu_id", menu_id);
		
		ncmsResult = ifService.getNcmsData(param);
		
		if (ncmsResult != null) {
			result = CastUtils.StringToJsonMap(ncmsResult);
		}

		return result;
	}

	// ppm_yn으로 처리한다.
	public static void checkPPMList(Object list, Map<String, String> param) {
		String ppmYn = "N";
		if (param.containsKey("ppm_yn")) {
			ppmYn = param.get("ppm_yn");
		}
		
		// Y가 아니면 ppm_yn 확인한다.
		if (!"Y".equals(ppmYn)) {
			List<Map<String, Object>> mapList = CastUtils.getObjectToMapList(list);
			// ppm 상품 삭제.
			for (int i = mapList.size()-1; i >= 0; i--) {
				try {
					Map<String, Object> map = mapList.get(i);
					
					if (!map.containsKey("prd_typ_cd")) {
						continue;
					}
					String prdTydCd = (map.get("prd_typ_cd") + "").trim();
					
					if (ppmProduct.equals(prdTydCd)) {
						mapList.remove(i);
						continue;
					}
					
					// 2019-07-09 박문수 
					// 기간권 추가 
					if ("32".equals(prdTydCd)) {
						mapList.remove(i);
						continue;
					}
					
				} catch (Exception e) {
					System.out.println(e.toString());
				}
			}
			
			checkPackBizList(list, param);
		}
	}
	
	// id_package 값을 확인하여서 price arr를 obj로 변환시켜준다.
	public static void checkPackBizList(Object list, Map<String, String> param) {
		String cmcYn = "N";
		if (param.containsKey("cmc_yn")) {
			cmcYn = param.get("cmc_yn");
		}
		
		List<Map<String, Object>> mapList = CastUtils.getObjectToMapList(list);
		if ("Y".equals(cmcYn) || mapList == null) return;
		
		for (int i = mapList.size()-1; i >= 0; i--) {
			try {
				Map<String, Object> map = mapList.get(i);
				
				if (!map.containsKey("prd_typ_cd")) {
					continue;
				}
				String prdTydCd = (map.get("prd_typ_cd") + "").trim();
				
				if (commerceProduct.equals(prdTydCd)) {
					mapList.remove(i);
					continue;
				}
				
			} catch (Exception e) {
				System.out.println(e.toString());
			}

			// use_yn 추가.
			try {
				Map<String, Object> map = mapList.get(i);
				
				if (!map.containsKey("use_yn")) {
					continue;
				}
				String useYn = (map.get("use_yn") + "").trim();
				
				if (!"Y".equals(useYn)) {
					mapList.remove(i);
				}
			} catch (Exception e) {
				System.out.println(e.toString());
			}
		}
	}
	
}
