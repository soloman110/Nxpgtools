package com.zinnaworks.nxpgtool.controller;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.zinnaworks.nxpgtool.common.ResponseCommon;
import com.zinnaworks.nxpgtool.exception.DataNotValidException;
import com.zinnaworks.nxpgtool.rest.RestClient_BadCode;
import com.zinnaworks.nxpgtool.service.IFService;
import com.zinnaworks.nxpgtool.util.JsonUtil;

@RequestMapping("/nxpgtool")
@Controller
public class IFController {
	private static final Logger logger = LoggerFactory.getLogger(IFController.class);
		
	@Autowired
	IFService ifService;
	
	@Autowired
	RestClient_BadCode client;
	
	@RequestMapping("/if")
	public String hello(Model model, @RequestParam(defaultValue = "Ryan") String name) throws FileNotFoundException {
		model.addAttribute("name", name);
		return "tiles/thymeleaf/if";
	}
	
	@ResponseBody
	@RequestMapping(value="/if/checkfield", method = {RequestMethod.GET,RequestMethod.POST})
	public Map<String, String> checkField(@RequestParam String url, @RequestParam String ifname) throws FileNotFoundException {
		
		Map<String, Object> ifTree = null;
		try {
			ifTree = ifService.toTree(url);
		} catch (DataNotValidException e) {
			return ResponseCommon.IFResponse("해당 데이터가 없음..Interface 호출하여 데이터 확인하세요.");
		}
		Map<String, Object> apiTree = ifService.getFieldTree(ifname);
		logger.info(JsonUtil.objectToJson(apiTree));
		
		String diff = ifService.compare(ifTree, apiTree);
		return ResponseCommon.IFResponse(diff);
	}
	
	@RequestMapping("/jiraissue")
	public String jiraIssue(Model model, @RequestParam(defaultValue = "Ryan") String name) throws FileNotFoundException {
		model.addAttribute("name", name);
		return "tiles/thymeleaf/jiraIssue";
	}

	@RequestMapping("/ifcompare")
	public String ifcompare(Model model) {
		return "tiles/thymeleaf/ifCompare";
	}

	@ResponseBody
	@RequestMapping("/ifcomparecheck")
	public Object ifcomparecheck(Model model, @RequestParam Map<String, String> param) {
		return ifService.compareInterface(param);
	}
		
	@RequestMapping("/ifdesc")
	public String ifdesc(Model model) {
		return "tiles/thymeleaf/ifDesc";
	}

	@ResponseBody
	@RequestMapping("/ifdesccheck")
	public void ifdesccheck(@RequestParam Map<String, String> param, HttpServletResponse response) {

	    SXSSFWorkbook wb = new SXSSFWorkbook();
	    Sheet sheet = wb.createSheet();

	    List<Map<String, String>> resultList = ifService.descInterface(param);
	    try {
	    	// 제목 만들기
	    	int cellIndex = 0;
  	        Row row = sheet.createRow(0);
  	        Cell cell = row.createCell(cellIndex++); cell.setCellValue("DATA(XPG)");
  	        cell = row.createCell(cellIndex++); cell.setCellValue("태그");
  	        cell = row.createCell(cellIndex++); cell.setCellValue("XPGValue");
  	        cell = row.createCell(cellIndex++); cell.setCellValue("DATA(CMS)");
  	        cell = row.createCell(cellIndex++); cell.setCellValue("CMSValue");
  	        cell = row.createCell(cellIndex++); cell.setCellValue("Sync");
  	        cell = row.createCell(cellIndex++); cell.setCellValue("ByPass");
  	      	
  	        // 데이터 넣기
	    	for (int i = 0; i < resultList.size(); i++) {
	    		cellIndex = 0;
	  	        row = sheet.createRow(i+1);
	  	        Map<String, String> result = resultList.get(i);
	  	        
	  	        cell = row.createCell(cellIndex++); cell.setCellValue(result.get("nxpg"));
	  	        cell = row.createCell(cellIndex++); cell.setCellValue(result.get("tag"));
	  	        cell = row.createCell(cellIndex++); cell.setCellValue(result.get("nxpgValue"));
	  	        cell = row.createCell(cellIndex++); cell.setCellValue(result.get("ncms"));
	  			cell = row.createCell(cellIndex++); cell.setCellValue(result.get("ncmsValue"));
	  			cell = row.createCell(cellIndex++); cell.setCellValue(result.get("sync"));
	  			cell = row.createCell(cellIndex++); cell.setCellValue(result.get("byPass"));
	    	}

		response.setHeader("Set-Cookie", "fileDownload=true; path=/");
		response.setHeader("Content-Disposition", String.format("attachment; filename=\""+ param.get("ifname").toString() +".xlsx\""));
		wb.write(response.getOutputStream());

	    } catch(Exception e) {
	        response.setHeader("Set-Cookie", "fileDownload=false; path=/");
	        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
	        response.setHeader("Content-Type","text/html; charset=utf-8");

	        OutputStream out = null;
	        try {
	            out = response.getOutputStream();
	            byte[] data = new String("fail..").getBytes();
	            out.write(data, 0, data.length);
	        } catch(Exception ignore) {
	            ignore.printStackTrace();
	        } finally {
	            if(out != null) try { out.close(); } catch(Exception ignore) {}
	        }

	    } finally {
	        // 디스크 적었던 임시파일을 제거합니다.
	        wb.dispose();
	        try { wb.close(); } catch(Exception ignore) {}
	    }
	}
}
