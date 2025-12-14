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
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import com.netsuite.tools.imex.file.PropertyFile;

import model.Constants;

public class webRequestResponseHelper {
	
	Map<String, String> header = new HashMap<String, String>();
	private fileOutputHelper appLog;
	private PropertyFile configNs;
	private String _baseLogfile;

	public webRequestResponseHelper() {

		Date dtNow = new Date();

		configNs = new PropertyFile("netsuite");
		String stDate = new SimpleDateFormat("MM-dd-yyyy").format(dtNow);
		_baseLogfile = configNs.getValue(Constants.FILE_BASELOGDIR);
		_baseLogfile = String.format("%sLOGS %s.log", _baseLogfile, stDate);
		appLog = new fileOutputHelper(_baseLogfile);
	}
	
	public String sendRequest(String webMethod, String url,String data) throws Exception{
		
		System.out.println("#   SENDING REQUEST...");
		
		Date today = new Date();
		SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss a");
		String date = DATE_FORMAT.format(today);
		
		try{
			
			int responseCode = 0;
			String responseCodeMsg;
			URL webUrl = new URL(url);
			HttpsURLConnection urlConn = (HttpsURLConnection) webUrl.openConnection();
			urlConn.setRequestMethod(webMethod);
			urlConn.setAllowUserInteraction(false);
			urlConn.setDoInput(true);
			urlConn.setDoOutput(true);

			//Add Headers
			for ( Map.Entry<String, String> entry : header.entrySet() ) {
			    String key = entry.getKey();
			    String value = entry.getValue();
				urlConn.setRequestProperty(key,value);
			}
			
			//write the data
			if (!data.isEmpty()){
				try{
					urlConn.getOutputStream().write(data.getBytes("UTF-8"));
				}finally{
					urlConn.getOutputStream().close();
				}
			}
			
			responseCode = urlConn.getResponseCode();
			responseCodeMsg = urlConn.getResponseMessage();
			System.out.println("#   SENDING REQUEST Response Code\t: " + responseCode);
			System.out.println("#   SENDING REQUEST Response Message\t: " + responseCodeMsg);	
			
			if (responseCode == 200) {
				BufferedReader br;
				StringBuilder response_sb = new StringBuilder();
				br = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
				try{
					String responseLine;
					do {
						responseLine = br.readLine();
						if (responseLine != null)
							response_sb.append(responseLine);
					} while (responseLine != null);
				}finally{
					br.close();
				}
				return response_sb.toString();
			}else{
				System.out.println(responseCodeMsg);
				throw new Exception(responseCodeMsg);
			}
		}catch(Exception ex){
			appLog.writeLine(String.format( "{ERROR @%s}",date));
			appLog.writeLine(String.format( " -message: %s",ex.toString()));
			for(StackTraceElement ste : ex.getStackTrace()){
				appLog.writeLine(String.format("\t%6d : %s.%s\t#%s",ste.getLineNumber(), ste.getClassName(),ste.getMethodName(),ste.getClass())); 
			}
			throw ex;
		}

	}
	
	public void setHeader(String key, String value){
		header.put(key, value);
	}
	
}
