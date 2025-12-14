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
package Views;

import java.awt.EventQueue;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import helper.nsUrlDiscoveryHelper;

import java.net.URL;

/******************************************************************************************************************
 * CLASS STARTUP
 * This is the MAIN class. Provides user option to run through user interface or console for scheduled processes
 *****************************************************************************************************************/
public class startup {
	private static Boolean isUi = false;
	private static Boolean isDefault = false;
	private static Boolean isConsole = false;
	
	public static void main(String[] args) throws IOException {		
        for (String arg : args) {
			switch (arg.toLowerCase()) {
			case "-ui":
				isUi = true;
			case "-?":
				isDefault = true;
				break;
			case "-ci":
				isConsole = true;
				break;
			default:
				StringBuilder argInfo = new StringBuilder();
				argInfo.append("##############################################\n");
				argInfo.append("# Invalid Argument parameter                 #\n");
				argInfo.append("# Use the Argument Bellow                    #\n");
				argInfo.append("#____________________________________________#\n");
				System.out.println(argInfo.toString());
				
				isDefault = true;
				break;
			}
		}
		if (args.length <=0){
			isUi = true;
		}

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					if (isUi) {
						
						
						mainview frame = new mainview();
						frame.setVisible(true);
						return;
					}

					if (isDefault) {
						StringBuilder argInfo = new StringBuilder();
						argInfo.append("##############################################\n");
						argInfo.append("#           Argument Information             #\n");
						argInfo.append("#--------------------------------------------#\n");
						argInfo.append("# -ui\t: Run the User Interface             #\n");
						argInfo.append("# -ci\t: Run the Console Interface          #\n");
						argInfo.append("#--------------------------------------------#\n");
						argInfo.append("# GET CONFIGURATION                          #\n");
						argInfo.append("# -config=mall\t: Get all Map Config         #\n");
						argInfo.append("##############################################\n");
						System.out.println(argInfo.toString());
						// Scanner scan=new Scanner(System.in);
					}
					
					if(isConsole){
						StringBuilder argInfo = new StringBuilder();
						
						String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
						argInfo.append("*** Start Time (Timestamp) : ");
						argInfo.append(timeStamp);
						argInfo.append("\n\n");
						
						argInfo.append("##############################################\n");
						argInfo.append("#           Running on Console               #\n");
						argInfo.append("#--------------------------------------------#\n");
						argInfo.append("#                                            #\n");
						System.out.println(argInfo.toString());
						
						mainconsole console = new mainconsole();
						
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
