package com.zinnaworks.nxpgtool.inspect;

import java.util.Map;

/**
 * 
 * @author sikongming
 *
 * @param <T> NCMS에서 받아온 데이터, 우리가 처리할 데이터의 타입 Map<String, Object>, List<Map<String, Object>> 
 * @param <S> Filter에서 사용하는 데이터 타입 .
 * 
 * @param field 데이터를 추출시 사용하는 key. 예: List<Map<String, Object>> list = CastUtil.getObjectToMapList(map.get(field)); 
 */
public abstract class InspectStrategy <T, S> {
	InspectFilter<S> filter;
	String field;
	String[] toDelFields;
	InspectCallback<Map<String, Object>> cb;

	public InspectStrategy(String field, String[] toDelFields, InspectCallback<Map<String, Object>> cb, InspectFilter<S> filter) {
		this.filter = filter;
		this.field = field;
		this.cb = cb;
		this.toDelFields = toDelFields;
	}
	public InspectStrategy(InspectFilter<S> filter) {
		this.filter = filter;
	}
	public InspectStrategy() {
	}
	public abstract void handle(T data, boolean inspYn);
	protected abstract boolean isInspectData(S data);
}