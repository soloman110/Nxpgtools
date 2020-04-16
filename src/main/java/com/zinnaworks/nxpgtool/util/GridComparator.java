package com.zinnaworks.nxpgtool.util;

import java.util.Comparator;
import java.util.Map;

public class GridComparator implements Comparator<Map<String, Object>> {
	
	public final static int DATA_TYPE_STRING = 0;
	public final static int DATA_TYPE_INTEGER = 1;
	
	private String target = null;	// 정렬대상 
	private boolean flag;		// Order 방식 
	
	public GridComparator(String target, boolean flag) {
		this.target = target;
		this.flag = flag;
	}

	@Override
	public int compare(Map<String, Object> grid0, Map<String, Object> grid1) {
		String v1 = null;
		String v2 = null;
		
		try {
				v1 = (String) grid0.get(target);
				v2 = (String) grid1.get(target);
		}
		catch(Exception e) {
			v1 = "";
			v2 = "";
			// LogUtil.error("", "", "", "", "", e.getStackTrace()[0].toString());
		}
		
		return (flag ? v1.compareTo(v2) : v2.compareTo(v1));
	}

}