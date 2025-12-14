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
* Version Type    Date            	Author                  Remarks
* 1.00    Create  8/26/2014 		Dennis Geronimo         Initial Version
* 1.00    Create  7/3/2015  		Rose Ann Ilagan			Add enhancements
*                                                           1. Enable Token based authentication
*                                                           2. Read files from FTP Server
*                                                           3. Support Multi Queue for SuiteCloud Licensed Account
*/

package controller;

import helper.LoggerHelper;
import helper.csvFileHelper;
import helper.nsUrlDiscoveryHelper;
import helper.webRequestResponseHelper;
import helper.AuthService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.Constants;
import model.GroupedData;
import model.ImportDataRequest;
import model.getMapConfigRequest;
import model.importDataResponse;
import model.errorcsvdata;
import model.mapConfig;
import model.nsURLDiscoveryMap;
import Exceptions.invalidcsvData;
import Exceptions.invalidcsvHeaderException;
import Exceptions.multiplecsviexception;

import com.google.gson.Gson;
import com.netsuite.tools.imex.file.PropertyFile;

import org.scribe.model.Response;
import org.scribe.model.Verb;


/******************************************************************************************************************
 * CLASS WebRequesController Handles request to send csv file for csv import to
 * restlet
 *****************************************************************************************************************/
public class WebRequesController {

	private PropertyFile configNs;
	private mapConfig[] CONFIGS_;
	private AuthService service;
	public String Error = "";
	public int queue = 1;
	public String details = "";
	public String charset = "";
	
	public nsURLDiscoveryMap dataCenter = new nsURLDiscoveryMap();

	public WebRequesController(String LoginProcess) throws Exception {
		configNs = new PropertyFile("netsuite");
		try {

			nsUrlDiscoveryHelper urlDisc = new nsUrlDiscoveryHelper();
			dataCenter = urlDisc.getDomain();
	
		} catch (Exception e) {
			System.out.println("##############################################\n");
			System.out.println("ERROR " + e.getStackTrace());
			System.out.println("##############################################\n");
		}

		charset = configNs.getValue(Constants.FILE_CHARSET);
		if(charset.isEmpty()){
			charset = "ISO-8859-1";
		}

		service = new AuthService(LoginProcess, dataCenter);
		getConfig(LoginProcess);
	}

	public String getError() {
		return this.Error;
	}

	public String getDetails() {
		return this.details;
	}

	public void resetDetails() {
		this.details = "";
	}

	public void setQueue(int selectedQueue) {
		this.queue = selectedQueue;
	}

	/******************************************************************************************************************
	 * FUNCTION createWebRequest Check csv files and send per part for csv
	 * import
	 *****************************************************************************************************************/
	public boolean createWebRequest(String login, int mapId, String email, String pass, String data, String fileName, String delimiter) {
		try {
			this.Error = "";
			int requestQueue = this.queue;
			if (login.equals("OAUTH")) {
				service = new AuthService(email, pass, dataCenter);
			}
			mapConfig config = getMapConfig(mapId);
			if (!pass.isEmpty()) {
				pass = pass.trim();
			}
			if (!email.isEmpty()) {
				email = email.trim();
			}

			if (config == null)
				return false;

			String json;
			String response = "";
			String _mapId;
			String _jobName;

			//String webUrl = configNs.getValue(Constants.NS_RESLET_URL);

			_jobName = configNs.getValue(Constants.NS_REQUEST_JOBNAME);
			Gson gson = new Gson();
			ImportDataRequest importObject = new ImportDataRequest();
			List<String> csvDataArr = processImportData(config, data, delimiter);

			_mapId = config.mapId;
			int partCount = 0;
			Date date = new Date();
			importObject.suiteCloud = configNs.getValue(Constants.NS_SUITECLOUD);

			importObject.date = date.toString();
			importObject.jobName = _jobName;
			importObject.mapId = _mapId;
			importObject.fileName = fileName;

			for (String csv : csvDataArr) {

				partCount++;

				// importObject.rows = rows;
				importObject.csvData = csv;
				importObject.requestPart = partCount;
				importObject.queue = requestQueue;

				System.out.println(this.queue);

				// reset queue
				if (requestQueue == 5) {
					requestQueue = 2;
				} else {
					if (requestQueue > 1)
						requestQueue++;
				}
				json = gson.toJson(importObject);
				String result = "";
				if (login.equals("NLAUTH")) {
					response = service.sendRequest(email, pass, json, "POST");
				} else {
					Response responsePost = service.sendRequest("Content-type", json, "application/json", Verb.POST);
					if (responsePost != null) {
						response = responsePost.getBody();
						System.out.println(responsePost.getBody());
					}
				}
				errorcsvdata errcsv = null;

				importDataResponse impRes = null;
				String errMsg = null;
				if (response != null) {
					impRes = gson.fromJson(response, importDataResponse.class);
					if (errcsv != null && errMsg != null) {
						throw new invalidcsvData(errMsg);
					} else if (impRes.isSuccess.equals("F")) {
						if (impRes.errorDetails == null) {
							errcsv = gson.fromJson(response, errorcsvdata.class);
							errMsg = errcsv.error.message;
							throw new invalidcsvData(errMsg);
						} else
							throw new invalidcsvData(impRes.errorMessage + " : " + impRes.errorDetails);
					} else {
						String stJobIdLog = "#   JOB-ID: " + impRes.importId;
						String stJobNameLog = "#   JOB NAME: " + impRes.jobName;
						String stResponseLog = "#   RESPONSE: " + response;
						String stQueue = "#   QUEUE: " + importObject.queue;
						System.out.println(stJobIdLog);
						System.out.println(stJobNameLog);
						if (importObject.queue > 1) {
							System.out.println(stQueue);
							this.details = this.details + LoggerHelper.getDate() + ":    " + stQueue + ".\n";
						}
						this.details = this.details + LoggerHelper.getDate() + ":    " + stJobIdLog + ".\n";
						this.details = this.details + LoggerHelper.getDate() + ":    " + stJobNameLog + ".\n";
						LoggerHelper.getAppLog().writeLine(stJobNameLog);
						LoggerHelper.getAppLog().writeLine(stResponseLog);
					}
				} else {
					this.Error = this.Error + service.getError() + "\n";
					return false;
				}

			}
			return true;
		} catch (Exception error) {
			error.printStackTrace();
			System.out.println(LoggerHelper.getDate() + ":    REQUEST ERROR:" + error.getMessage());
			this.Error = this.Error + error.getMessage().toString() + ".\n";
			return false;
		}
	}

	/******************************************************************************************************************
	 * FUNCTION processImportData Divide csv files per part
	 *****************************************************************************************************************/
	public List<String> processImportData(mapConfig config, String data, String delimiter) throws Exception {

		if (data.isEmpty())
			return null;

		 config.csvHeader = ( config.csvHeader.isEmpty())? "" :  config.csvHeader.trim().toLowerCase();
		List<String> retVal = new ArrayList<String>();
		
		String strMaxRow = configNs.getValue(Constants.NS_MAX_ROW);
		int maxRow = 24999;
		
		if(!strMaxRow.isEmpty()){
			Integer intMaxRow = Integer.parseInt(strMaxRow);
			
			if(intMaxRow > 0 && intMaxRow < 25000){
				maxRow = intMaxRow;
			}
		}
		
		List<String> arrData = new LinkedList<String>(Arrays.asList(data.split("\n")));
		String csvHeader = arrData.get(0);
		
		String configHeader =  config.csvHeader;
		if (!csvHeader.trim().toLowerCase().equals(configHeader)) {
			throw new invalidcsvHeaderException();
		}

		arrData.remove(0);
		int rowCount = arrData.size();
		System.out.println("#   CSV Row: " + rowCount);

		if (rowCount <= (maxRow)) {
			String concatData = csvHeader;
			for (String csv : arrData) {
				concatData += "\n" + csv;
			}
			retVal.add(concatData);
		} else {
			// NORMAL RECORD TYPE
			csvFileHelper csvHelper = new csvFileHelper();
			csvHelper.loadCsvToString(data, delimiter);
			Map<String, GroupedData> groupedDataMap = csvHelper.groupDataByColumn(config.primaryId, delimiter);

			int bufPos = 0;
			int page = 1;

			StringBuilder concatData = new StringBuilder();
			StringBuilder primaryIdPerPage = new StringBuilder();
			for (String key : groupedDataMap.keySet()) {
				GroupedData dataGroup = groupedDataMap.get(key);

				if (bufPos + dataGroup.getLineCount() > maxRow && concatData.length() > 0) {
					StringBuilder sysoutLog = new StringBuilder();
					sysoutLog.append("####### Page: ").append(page).append("\nPrimary Id: " + primaryIdPerPage);
					// System.out.println(sysoutLog.toString());
					retVal.add(concatData.toString());

					page++;
					primaryIdPerPage = new StringBuilder();
					concatData = new StringBuilder();
					bufPos = 0;
				}

				if (primaryIdPerPage.length() > 0) {
					primaryIdPerPage.append(",");
				}
				primaryIdPerPage.append(key);

				if (concatData.length() == 0) {
					concatData.append(csvHeader);
					//bufPos++;
				}

				concatData.append("\n").append(dataGroup.getValues().toString());
				bufPos += dataGroup.getLineCount();
			}
			StringBuilder log = new StringBuilder();
			log.append("####### Page: ").append(page).append("\nPrimary Id: " + primaryIdPerPage);
			System.out.println(log.toString());
			retVal.add(concatData.toString());
		}
		return retVal;
	}

	/******************************************************************************************************************
	 * FUNCTION processImportData Divide csv files per part
	 *****************************************************************************************************************/
	public List processImportData(String data) throws Exception {
		if (data.isEmpty())
			return null;

		String csvHeader = data.split("\n")[0];

		mapConfig config = getMapConfig(csvHeader);
		return processImportData(config, data, null);

	}

	/******************************************************************************************************************
	 * FUNCTION getConfig Get mapping configuration ids
	 *****************************************************************************************************************/
	public void getConfig(String loginProcess) throws Exception {
		getMapConfigRequest mapConfig = new getMapConfigRequest();
		mapConfig.getConfig = "MAPPING_GETALL";

		Gson gson = new Gson();
		String json = gson.toJson(mapConfig);
		String result = "";

		if (loginProcess.equals("OAUTH")) {
			Response responsePost = service.sendRequest("content-type", json, "application/json", Verb.POST);
			result = responsePost.getBody();
		}
		if (loginProcess.equals("NLAUTH")) {
			result = service.sendRequest(configNs.getValue(Constants.NS_EMAIL),
					configNs.getValue(Constants.NS_PASSWORD), json, "POST");
		}
		if (!loginProcess.isEmpty()) {
			if (result != null) {
				CONFIGS_ = gson.fromJson(result, mapConfig[].class);
				System.out.println("#   GET CONFIG'S");
				for (mapConfig config : CONFIGS_) {
					System.out.println(String.format("#\t- id: %d , name: %s , mapId: %s , header: %s ,primaryId %s",
							config.id, config.name, config.mapId, config.csvHeader, config.primaryId));
				}
			}
		}
	}

	/******************************************************************************************************************
	 * FUNCTION getAllMapConfig Constructor to get map id
	 *****************************************************************************************************************/
	public mapConfig[] getAllMapConfig() {
		return CONFIGS_;
	}

	/******************************************************************************************************************
	 * FUNCTION getMapConfig Constructor to get specific map id
	 *****************************************************************************************************************/
	public mapConfig getMapConfig(int mapId) {
		for (mapConfig config : CONFIGS_) {
			if (config.id == mapId) {
				return config;
			}
		}
		return null;
	}

	/******************************************************************************************************************
	 * FUNCTION getMapConfig Constructor to get specific map id
	 *****************************************************************************************************************/
	public int getMapConfigTotal() {
		int count = 0;
		for (mapConfig config : CONFIGS_) {
			count++;
		}
		return count;
	}

	/******************************************************************************************************************
	 * FUNCTION getMapConfig Constructor to get map id
	 *****************************************************************************************************************/
	public mapConfig getMapConfig(String mapIdOrHeaderFormat) throws Exception {
		int count = 0;

		mapConfig configResult = new mapConfig();
		for (mapConfig config : CONFIGS_) {
			 String configcsvHeader = ( config.csvHeader.isEmpty())? "" :  config.csvHeader.trim().toLowerCase();
			 mapIdOrHeaderFormat = mapIdOrHeaderFormat.trim().toLowerCase();
			 //configcsvHeader = new String(configcsvHeader.getBytes(),charset);
			 
			if (config.mapId == mapIdOrHeaderFormat || configcsvHeader.equals(mapIdOrHeaderFormat)) {
				configResult = config;
				count++;
			}
		}
		if (count == 1)
			return configResult;
		else if (count > 1)
			throw new multiplecsviexception("There are " + Integer.toString(count)
					+ " CSV integration mapping ids found. Only one CSV integration mapping id must be specified.");
		else
			throw new invalidcsvHeaderException();
	}

	/******************************************************************************************************************
	 * FUNCTION encodeJson encode json to a string
	 *****************************************************************************************************************/
	public String encodeJson(String json) {
		String data = json;
		data = data.replaceAll("\"", "\\\""); // - "
		data = data.replaceAll(Pattern.quote("\\"), Matcher.quoteReplacement("\\\\")); // -
																						// \
		data = data.replaceAll(Pattern.quote("\n"), Matcher.quoteReplacement("\\n")); // -
																						// New
																						// line

		return data;
	}

}
