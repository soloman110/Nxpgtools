package com.zinnaworks.nxpgtool.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.zinnaworks.nxpgtool.common.NXPGCommon;
import com.zinnaworks.nxpgtool.redis.RedisClient;
import com.zinnaworks.nxpgtool.service.GridService;
import com.zinnaworks.nxpgtool.util.CastUtil;
import com.zinnaworks.nxpgtool.util.JsonUtil;

@Service
public class GridServiceImpl implements GridService {

	@Autowired
	private RedisClient redisClient;

	@Autowired
	private ThreadPoolTaskExecutor gridServiceExecutor;

	@Override
	public List<String> searchVodPkg() {
		Cursor<Entry<String, Object>> cursor = redisClient.hscan(NXPGCommon.GRID_CONTENTS);
		final List<String> menuIds = Collections.synchronizedList(new ArrayList<>());
		try {
			if (cursor != null) {
				while (cursor.hasNext()) {
					Entry<String, Object> e = cursor.next();
					String menuId = e.getKey();
					final Object obj = e.getValue();

					gridServiceExecutor.execute(new Runnable() {
						@Override
						public void run() {
							if(!Objects.isNull(obj)) {
								try {
									Map<String, Object> gridMap = JsonUtil.jsonToObjectHashMap(obj.toString(), String.class, Object.class);
									if (gridMap != null) {
										List<Map<String, Object>> list = CastUtil
												.getObjectToMapList(gridMap.get("contents"));
										if (containVodOrPackageProduct(list)) {
											menuIds.add(menuId);
										}
									} 
								} catch (Exception e2) {
									e2.printStackTrace();
								}
							}
						}
					});
				}
			}
		} finally {
			try {
				cursor.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return menuIds;
	}

	@Override
	public Map<String, Object> getGridByMenuId(String menuId) {
		String json = (String) redisClient.hget(NXPGCommon.GRID_CONTENTS, menuId);
		Map<String, Object> gridMap = JsonUtil.jsonToObjectHashMap(json, String.class, Object.class);
		return gridMap;
	}

	/**
	 * 
	 * List 중에 Predicate 조건을 만족하는 Element(Map)가 하나만 있으면 true로 리턴한
	 * 
	 * @param list
	 * @param p
	 * @return
	 */
	private boolean listChecker(List<Map<String, Object>> list, Predicate<Map<String, Object>> p) {
		ListIterator<Map<String, Object>> it = list.listIterator();
		while (it.hasNext()) {
			Map<String, Object> map = it.next();
			if (p.test(map)) {
				return true;
			}
		}
		return false;
	}
	
	/***
	 * 
	 * List(Grid List)중에 패키지상품 또는 VOD +관련상품 콘텐츠를 포함하는지 확인. 
	 * 
	 * @param list Grid List
	 * @return
	 */
	private boolean containVodOrPackageProduct(List<Map<String, Object>> list) {
		if (list == null || list.size() == 0) {
			return false;
		}
		return listChecker(list, new Predicate<Map<String, Object>>() {
			@Override
			public boolean test(Map<String, Object> map) {
				String synon_typ_cd = (String) map.get("synon_typ_cd");
				return "03".equals(synon_typ_cd) || "04".equals(synon_typ_cd);
			}
		});
	}
}