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

import java.net.URL;

import javax.xml.namespace.QName;

import org.scribe.model.Response;
import org.scribe.model.Verb;

import com.google.gson.Gson;
import com.netsuite.tools.imex.file.PropertyFile;
import com.netsuite.webservices.platform.core_2015_1.DataCenterUrls;
import com.netsuite.webservices.platform_2015_1.NetSuitePortType;
import com.netsuite.webservices.platform_2015_1.NetSuiteServiceLocator;

import model.Constants;
import model.nsURLDiscoveryMap;

public class nsUrlDiscoveryHelper {

	private PropertyFile configNs;

	private String nlAccount = "";
	private String role = "";
	

	public nsUrlDiscoveryHelper() throws Exception 
	{
		configNs = new PropertyFile("netsuite");
        nlAccount = configNs.getValue(Constants.NS_ACCOUNT).trim();     
        role = configNs.getValue(Constants.NS_ROLE);
	}
	
	public nsURLDiscoveryMap getDomain(){
        nsURLDiscoveryMap retval = new nsURLDiscoveryMap();
        
		try{
			
            NetSuitePortType _service;
            // Locate the NetSuite web service.
            NetSuiteServiceLocator serviceLocator =  new NetSuiteServiceLocator();
            String stWSDLUrl = configNs.getValue(Constants.NS_URL);
            
            if(stWSDLUrl.isEmpty()){
                //_service = serviceLocator.getNetSuitePort();
                throw new Exception(Constants.NS_URL + " is Required in netsuite.properties");
            }else{
                _service = serviceLocator.getNetSuitePort(new URL(stWSDLUrl));
            }
            
            // Get the service port
            //Get RestDomain
            String resDomain = _service.getDataCenterUrls(nlAccount).getDataCenterUrls().getRestDomain();
            String wsDomain = _service.getDataCenterUrls(nlAccount).getDataCenterUrls().getWebservicesDomain();
            String sysDomain = _service.getDataCenterUrls(nlAccount).getDataCenterUrls().getSystemDomain();
            
            retval.role.internalId = role;
            retval.account.internalId = nlAccount;
            retval.dataCenterURLs.restDomain = resDomain;
            retval.dataCenterURLs.webservicesDomain = wsDomain;
            retval.dataCenterURLs.systemDomain = sysDomain;
            
			System.out.println("##############################################");
            System.out.println("RESLET DOMAIN URL \t:" + retval.dataCenterURLs.restDomain);
            System.out.println("SYS DOMAIN URL \t:" + retval.dataCenterURLs.systemDomain);
            System.out.println("WS DOMAIN URL \t:" + retval.dataCenterURLs.webservicesDomain);
            System.out.println("##############################################\n");
            
            
	        return retval;
		}catch(Exception e){
			return retval;
		}

	}

}
