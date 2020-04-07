package com.zinnaworks.nxpgtool.inspect;

public interface InspectFilter<T> {
	public boolean filter(T t, boolean inspYn);
}