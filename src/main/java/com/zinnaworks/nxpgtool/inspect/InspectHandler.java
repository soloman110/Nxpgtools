package com.zinnaworks.nxpgtool.inspect;

import java.util.Map;

public class InspectHandler {
	public static <T1, T2> void handle(T1 object, InspectStrategy<T1, T2> inspector, Map<String, String> param) {
		if(object == null) {
			return;
		}
		inspector.handle(object, getInspYn(param));
	}

	private static boolean getInspYn(Map<String, String> param) {
		return "y".equalsIgnoreCase((param.get("inspect_yn")));
	}
}

