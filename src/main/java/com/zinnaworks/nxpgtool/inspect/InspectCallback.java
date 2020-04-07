package com.zinnaworks.nxpgtool.inspect;

public interface InspectCallback<T> {
	public void call(T t, boolean yn);
}
