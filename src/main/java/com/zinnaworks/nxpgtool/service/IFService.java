package com.zinnaworks.nxpgtool.service;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import com.zinnaworks.nxpgtool.exception.DataNotValidException;

public interface IFService {

	Map<String, Object> toTree(String ifName) throws FileNotFoundException, DataNotValidException;
	public String compare(Map<String, Object> remote, Map<String, Object> apiExcel);
	public Map<String, Object> getFieldTree(String ifname) throws FileNotFoundException;
	
	public String compareInterface(Map<String, String> param);
	public List<Map<String, String>> descInterface(Map<String, String> param);
	public String getNcmsData(Map<String, String> param);
}