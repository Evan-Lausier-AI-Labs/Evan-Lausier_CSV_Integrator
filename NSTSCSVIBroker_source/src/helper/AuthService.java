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

import model.OAuthServiceHelper;
import model.nsURLDiscoveryMap;
import model.Constants;
import helper.webRequestResponseHelper;
import com.netsuite.tools.imex.file.PropertyFile;

import java.net.URI;
import java.net.URL;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

/**
 *
 * @author rilagan
 */
public class AuthService {
	public String consumerKey;
	public String consumerSecret;
	public String tokenId;
	public String tokenSecret;
	public String restURL;
	public String realm;
	public String error = "";
	private OAuthService service;
	private Token accessToken;
	public PropertyFile configNs = new PropertyFile("netsuite");
	nsURLDiscoveryMap _dataCenter = new nsURLDiscoveryMap();

	public AuthService(String LoginProcess, nsURLDiscoveryMap dataCenter) throws Exception {
		consumerKey = configNs.getValue(Constants.NS_CONSUMER_KEY);
		consumerSecret = configNs.getValue(Constants.NS_CONSUMER_SECRET);
		tokenId = configNs.getValue(Constants.NS_TOKEN_ID);
		tokenSecret = configNs.getValue(Constants.NS_TOKEN_SECRET);
		restURL = configNs.getValue(Constants.NS_RESLET_URL);
		realm = configNs.getValue(Constants.NS_ACCOUNT);
		if (LoginProcess.equals(Constants.LoginPreference.OAUTH.toString())) {
			service = getService();
			accessToken = getToken();
		}

		_dataCenter = dataCenter;
		
		
		if(restURL.isEmpty()){
			throw new Exception("netsuite.tsg.Integration.restlet.url is Empty!");
		}
	}

	public String getError() {
		System.out.println("ERROR:  " + this.error);
		return this.error;
	}

	public AuthService(String _tokenId, String _tokenSecret, nsURLDiscoveryMap dataCenter) throws Exception {
		restURL = configNs.getValue(Constants.NS_RESLET_URL);
		consumerKey = configNs.getValue(Constants.NS_CONSUMER_KEY);
		consumerSecret = configNs.getValue(Constants.NS_CONSUMER_SECRET);
		tokenId = _tokenId;
		tokenSecret = _tokenSecret;
		realm = configNs.getValue(Constants.NS_ACCOUNT);
		
		service = getService();
		accessToken = getToken();

		_dataCenter = dataCenter;
	}

	public Response sendRequest(String contentType, String json, String appReturn, Verb action) {
		try {
			restURL = configNs.getValue(Constants.NS_RESLET_URL);
			String Url = "";
			URI uri = new URI(restURL);
			if(uri.isAbsolute()){
				Url = restURL;
			}else{
				Url = _dataCenter.dataCenterURLs.restDomain + restURL;
			}		
			
			OAuthRequest request = new OAuthRequest(action, Url);
			request.setRealm(realm);
			request.addHeader(contentType, appReturn);
			request.addPayload(json);

			service.signRequest(accessToken, request);
			return request.send();
		} catch (Exception error) {
			System.out.println("OAUTH ERROR: " + error.getMessage());
			this.error = error.getMessage();
			return null;
		}
	}
	
	public Response sendRequest(String url,String contentType, String json, String appReturn, Verb action) {
		try {
			OAuthRequest request = new OAuthRequest(action, url);
			request.setRealm(realm);
			request.addHeader(contentType, appReturn);
			request.addPayload(json);

			service.signRequest(accessToken, request);
			return request.send();
		} catch (Exception error) {
			System.out.println("OAUTH ERROR: " + error.getMessage());
			this.error = error.getMessage();
			return null;
		}
	}

	public String sendRequest(String email, String pass, String json, String action) {
		try {
			String headerAuth = "NLAuth nlauth_account=%s, nlauth_email=%s, nlauth_signature=%s, nlauth_role=%s";
			headerAuth = String.format(headerAuth, configNs.getValue(Constants.NS_ACCOUNT), email, pass, configNs.getValue(Constants.NS_ROLE));
			String Url = "";

			URI uri = new URI(restURL);
			if(uri.isAbsolute()){
				Url = restURL;
			}else{
				Url = _dataCenter.dataCenterURLs.restDomain + restURL;
			}			
			System.out.println("dataCenterURLs.restDomain:" + _dataCenter.dataCenterURLs.restDomain);
			
			webRequestResponseHelper client = new webRequestResponseHelper();
			client.setHeader("Content-type", "application/json");
			client.setHeader("Authorization", headerAuth);
			return client.sendRequest(action, Url, json);
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			this.error = ex.getMessage();
			return null;
		}
	}

	public String sendURLRequest(String url, String email, String pass, String json, String action) {
		try {

			String headerAuth = "NLAuth nlauth_email=%s, nlauth_signature=%s";
			headerAuth = String.format(headerAuth, email, pass);

			webRequestResponseHelper client = new webRequestResponseHelper();
			client.setHeader("Content-type", "application/json");
			client.setHeader("Authorization", headerAuth);
			return client.sendRequest(action, url, json);
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			this.error = ex.getMessage();
			return null;
		}
	}

	private OAuthService getService() {
		return new ServiceBuilder().provider(OAuthServiceHelper.class).apiKey(consumerKey.trim()).apiSecret(consumerSecret.trim())
				.signatureType(SignatureType.Header).build();
	}

	private Token getToken() {
		return new Token(tokenId.trim(), tokenSecret.trim());
	}

}


