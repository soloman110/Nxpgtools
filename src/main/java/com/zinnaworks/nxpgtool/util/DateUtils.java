package com.zinnaworks.nxpgtool.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DateUtils {

	public static Date getStringToDateTime(String value) {
		try {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
			return formatter.parse(value);
		} catch (Exception e) {
			// LogUtil.error("", "", "", "", "", e.toString());
			return null;
		}
	}

	public static String getYYYYMMDDhhmmss2() {
		Locale currLocale = new Locale("KOREAN","KOREA");  
		String pattern = "yyyyMMddHHmmss";
		SimpleDateFormat formatter = new SimpleDateFormat(pattern, currLocale);

		return formatter.format(new Date());
	}

	public static String getYYYYMMDD() {
		Locale currLocale = new Locale("KOREAN","KOREA");  
		String pattern = "yyyyMMdd";
		SimpleDateFormat formatter = new SimpleDateFormat(pattern, currLocale);

		return formatter.format(new Date());
	}

	public static Date getStringToDate(String value) {
		try {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
			return formatter.parse(value);
		} catch (Exception e) {
			// LogUtil.error("", "", "", "", "", e.toString());
			return null;
		}
	}
	
	// 입력 일자 기준으로 며칠 이내인지 확인
	public static boolean checkDate(String strDate, int value) {
		if (strDate == null) return false;
		if (value == 0) return false;
		
		try {
			// 현재시간 년월일만 가지고 오기
			Date now = getStringToDate(getYYYYMMDD());
			
			// 입력받은값 년월일만 가지고 오기
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
			Date chkDate = formatter.parse(strDate);
			Calendar cal = Calendar.getInstance();
			cal.setTime(chkDate);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			chkDate = cal.getTime();
			//////////////////////////
			
			int compare = now.compareTo(chkDate);
			// 지금시간이랑 같으면 
			if (compare == 0)
				return true;
			
			// 이전일 확인  (prd_prc_to_dt)
			if (value < 0) {
				// 지금 시간보다 더 작으면
				if (compare > 0) return true;
			}
			// 며칠 이내 확인 
			else {
				// 지금 시간보다 더 작으면 
				if (compare > 0) return false;
			}
			
			long diffDay = (chkDate.getTime() - now.getTime()) / (24*60*60*1000);
			return Math.abs(diffDay) <= Math.abs(value);
		} catch (Exception e) {
			// LogUtil.error("", "", "", "", "", e.toString());
			return false;
		}
	}

	// 005에서 Grid_Banner 필터링 할 떄 32번은 다른 필드값을 처리해야 되어서 추가.
	public static void getCompareMonth(List<Map<String, Object>> list, String fromDt, String toDt, String toDt2, boolean isMenuAndGrid) {

		if (list != null) {
			List<Map<String, Object>> newList = new ArrayList<Map<String, Object>>();
			for (Map<String, Object> object : list) {
				String prdTypCd = CastUtils.getObjectToString(object.get("prd_typ_cd"));

				if ("32".equals(prdTypCd))  
					doCompare(newList, object, fromDt, toDt2, isMenuAndGrid);
				else 
					doCompare(newList, object, fromDt, toDt, isMenuAndGrid);
			}
			//list = new ArrayList<Map<String, Object>>();
			//list.addAll(newList);
			for (Map<String, Object> del : newList) {
				list.remove(del);
			}
		}
	}
	
	public static void getCompare(List<Map<String, Object>> list, String fromDt, String toDt, boolean isMenuAndGrid) {
		
		//if (isMenuAndGrid && !expiryDate) {
			//return;
		//}
		if (list != null) {
			List<Map<String, Object>> newList = new ArrayList<Map<String, Object>>();
			for (Map<String, Object> object : list) {
				doCompare(newList, object, fromDt, toDt, isMenuAndGrid);
			}
			//list = new ArrayList<Map<String, Object>>();
			//list.addAll(newList);
			for (Map<String, Object> del : newList) {
				list.remove(del);
			}
		}
	}

	public static void doCompare(List<Map<String, Object>> newList, Map<String, Object> object, String fromDt, String toDt, boolean isMenuAndGrid) {
		// menu_cd 제거
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		Date date = new Date();
		Date strTime = null;
		Date endTime = null;
		
		String tempFromDt = String.valueOf(object.get(fromDt));
		String temptoDt = String.valueOf(object.get(toDt));
		
		try {
			if (tempFromDt != null && tempFromDt.matches("[0-9]{14}")) {
				strTime = dateFormat.parse(tempFromDt);
			}
			if (temptoDt != null && temptoDt.matches("[0-9]{14}")) {
				endTime = dateFormat.parse(temptoDt);
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
		}

		if ((strTime == null || strTime.before(date)) && (endTime == null || endTime.after(date))) {
			
		} else {
        	newList.add(object);
        }

		if (object.containsKey("menus")) {
			getCompare(CastUtils.getObjectToMapList(object.get("menus")), fromDt, toDt, isMenuAndGrid);
		}
	}
}
