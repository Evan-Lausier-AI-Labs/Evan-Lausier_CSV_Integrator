/**
* Copyright (c) 1998-2015 NetSuite, Inc.
* 2955 Campus Drive, Suite 100, San Mateo, CA, USA 94403-2511
* All Rights Reserved.
* 
* This software is the confidential and proprietary information of
* NetSuite, Inc. ("Confidential Information"). You shall not
* disclose such Confidential Information and shall use it only in
* accordance with the terms of the license agreement you entered into
* with NetSuite.
* 
* The purpose of this solution is to develop integration processes for uploading records that are supported via NetSuite’s CSV Import process. 
* The process requires files of type (CSV) originating from a 3rd Party application. 
* 
* Version Type    Date            	Author                  Remarks
* 1.00    Create  8/26/2014 		Dennis Geronimo         Initial Version
* 1.00    Create  7/3/2015  		Rose Ann Ilagan			Add enhancements
*                                                           1. Enable Token based authentication
*                                                           2. Read files from FTP Server
*                                                           3. Support Multi Queue for SuiteCloud Licensed Account
*/
package helper;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.print.attribute.HashAttributeSet;

import model.GroupedData;

public class csvFileHelper {
	private String csvFilename_;
	private Map<String, Integer> csvHeader;


	private ArrayList<ArrayList<String>> csvData = new ArrayList<>();

	public csvFileHelper(String filename) {
		csvFilename_ = filename;
	}

	public csvFileHelper() {
	}

	public String getFilename() {
		return csvFilename_;
	}

	public void SetFilename(String value) {
		csvFilename_ = value;
	}

	public ArrayList<ArrayList<String>> loadCsv() {
		BufferedReader csvFile = null;
		csvHeader = new Hashtable<String, Integer>();
		try {
			String sCurrentLine;
			csvFile = new BufferedReader(new FileReader(getFilename()));
			while ((sCurrentLine = csvFile.readLine()) != null) {
				if (csvHeader.isEmpty()) {
					String[] temp_header = sCurrentLine.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
					for (int i = 0; i < temp_header.length; i++) {
						csvHeader.put(temp_header[i].trim(), i);
					}
				} else {
					ArrayList<String> temp_csvColDataList = new ArrayList<String>();
					String temp_csvColData[] = sCurrentLine.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
					for (String data : temp_csvColData) {
						temp_csvColDataList.add(data);
					}
					csvData.add(temp_csvColDataList);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();

		} finally {
			try {
				if (csvFile != null)
					csvFile.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return csvData;
	}

	public ArrayList<ArrayList<String>> loadCsvToString(String rawData, String delimiter) {
		String[] bufcsvData = rawData.split("\n");
		csvHeader = new HashMap();
		
		for (String sCurrentLine : bufcsvData) {
			if (csvHeader.isEmpty()) {
				String[] temp_header = sCurrentLine.split(delimiter + "(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
				for (int i = 0; i < temp_header.length; i++) {
					csvHeader.put(temp_header[i].trim(), i);
				}
			} else {
				ArrayList<String> temp_csvColDataList = new ArrayList<String>();
				String temp_csvColData[] = sCurrentLine.split(delimiter + "(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
				for (String data : temp_csvColData) {
					temp_csvColDataList.add(data);
				}
				csvData.add(temp_csvColDataList);

			}
		}
		return csvData;
	}
	
	public Map<String, GroupedData> groupDataByColumn(String csvCol, String delimiter){
		String csvDelimiter = delimiter;
		
		if(delimiter.equals("\\|")){
			csvDelimiter = "|";
		}else if(delimiter.equals("\\s")){
			csvDelimiter = " ";
		}else if(delimiter.equals("\t")){
			csvDelimiter = "	";
		}
		
		Map<String, GroupedData> groupedDataMap = new HashMap<String, GroupedData>();
		
		int colindex = -1;
		
		try{
			colindex = csvHeader.get(csvCol);
		}catch(Exception error) {
			throw new InvalidParameterException("The Primary ID defined in the CSV Integration Mapping is not contained in the CSV Header Format. Kindly check the CSV Integration Mapping and/or the properties file.");
		}
		
		for(ArrayList<String> rows : csvData){
			String key = rows.get(colindex);
			String curLine = "";
			for(String colData : rows){
				curLine += (curLine.isEmpty())? colData : csvDelimiter + colData;
			}
			
			if(groupedDataMap.get(key) == null){
				GroupedData groupData = new GroupedData();
				groupData.setKey(key);
				groupData.setValues(new StringBuilder(curLine));
				groupData.setLineCount(1);
				groupedDataMap.put(key, groupData);
			}else{
				groupedDataMap.get(key).getValues().append("\n").append(curLine);
				int lineCount = groupedDataMap.get(key).getLineCount();
				groupedDataMap.get(key).setLineCount(++lineCount);
			}
		}
				
		return groupedDataMap;
	}
    
};
