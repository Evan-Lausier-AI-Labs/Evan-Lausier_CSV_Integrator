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
* 2.00    Create  7/3/2015  		Rose Ann Ilagan			Add enhancements
*                                                           1. Enable Token based authentication
*                                                           2. Read files from FTP Server
*                                                           3. Support Multi Queue for SuiteCloud Licensed Account
*/
package helper;

import java.text.SimpleDateFormat;
import java.util.Date;

import model.Constants;

import com.netsuite.tools.imex.file.PropertyFile;

public abstract class LoggerHelper {
	
	private static fileOutputHelper appLog;
	
	public static void instantiate() {
		if (appLog == null) {
			PropertyFile configNs = new PropertyFile("netsuite");
			String _dateformat = configNs.getValue(Constants.NS_DATEFORMAT);
			String _sDate = new SimpleDateFormat(_dateformat).format(new Date());
			String _baseLogfile = configNs.getValue(Constants.FILE_BASELOGDIR);
			_baseLogfile = String.format("%sLOGS_%s.log", _baseLogfile, _sDate);
			appLog = new fileOutputHelper(_baseLogfile,false);
		}
	}

	public static fileOutputHelper getAppLog() {
		if (appLog == null) {
			instantiate();
		}
		return appLog;
	}
    
    public static String getDate(){
        Date today = new Date();
        PropertyFile configNs = new PropertyFile("netsuite");
		String _datetimeformat = configNs.getValue(Constants.NS_DATETIMEFORMAT);
		SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(_datetimeformat);//("MM-dd-yyyy hh:mm:ss a");
		String date = DATE_FORMAT.format(today);
        return date;
    }    
}
