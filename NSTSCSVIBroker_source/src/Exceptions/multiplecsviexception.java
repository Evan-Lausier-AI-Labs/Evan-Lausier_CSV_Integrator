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
package Exceptions;

public class multiplecsviexception extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String message  = "Multiple CSV Integration Mapping IDs found.";
	
    public multiplecsviexception(String message){
		this.message = message;
	}
	
    @Override
    public String toString() {
        return message;
    }
 
    @Override
    public String getMessage() {
        return message;
    }
}
