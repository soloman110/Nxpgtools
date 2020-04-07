package com.zinnaworks.nxpgtool.util;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Jay Lee on 27/12/2016.
 */
public class StrUtil {
	
	public static String getValue(String value) {
		if (value == null) return "";
		return value;
	}
	
    public static <T extends CharSequence> T isNull(final T str, final T defaultStr) {
        return isEmpty(str) ? defaultStr : str;
    }

    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static String upperCase(final String str) {
        if (str == null) {
            return null;
        }
        return str.toUpperCase();
    }
	
	// 문자열을 받아서 리스트로 만들어준다.
	public static List<String> getList(String value, String pattern) {
		List<String> result = new ArrayList<String>();
		if (value != null) {
			if (!value.isEmpty()) {
				int conIndex = value.indexOf(pattern);
				while (conIndex > -1) {
					result.add(value.substring(0, conIndex));
					value = value.substring(conIndex + 1, value.length());
					conIndex = value.indexOf(pattern);
				}
				result.add(value);
			}
		}
		return result;
	}
	
	public static String getRegexString(String regex, String replaceStr) {
		
		String result = "";
		Pattern ptn = null;
		Matcher matcher = null;
		ptn = Pattern.compile(regex); 
		if (ptn != null) {
			matcher = ptn.matcher(replaceStr); 
			if (matcher != null) {
				while(matcher.find()){ 
					result=matcher.group(1);
					break;
				}
			}
		}
		
		return result;
		
	}
	
	public static boolean checkField(String json, String jsonPath) {
		
		if (json == null || json.isEmpty()) {
			return false;
		}
		
		if (!assertThatCustom(json, hasJsonPath(jsonPath))) {
			return false;
		}
		
		return true;
			
	}
	
	public static <T> Boolean assertThatCustom( T actual, org.hamcrest.Matcher<? super T> matcher) {
		if (!matcher.matches(actual)) {
			return false;
		} else {
			return true;
		}
	}
}
