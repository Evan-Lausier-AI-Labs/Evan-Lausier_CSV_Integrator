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
package model;

public class Constants{
	
	public static final String NS_URL = "netsuite.ws.url";
	
	public static final String NS_ACCOUNT = "netsuite.account";
	public static final String NS_ROLE = "netsuite.role";
	
	public static final String NS_EMAIL = "netsuite.email";
	public static final String NS_PASSWORD = "netsuite.password";

	public static final String NS_DATEFORMAT = "netsuite.dateformat";
	public static final String NS_DATETIMEFORMAT = "netsuite.datetimeformat";
	
    public static final String NS_SUITECLOUD = "netsuite.suiteCloudAcct";
    public static final String NS_CSVQUEUE  = "netsuite.csvImportQueue"; 
	public static final String NS_RESLET_URL = "netsuite.tsg.Integration.restlet.url";
	public static final String NS_DATACENTER_URL = "netsuite.datacenter.url";
	
	//public static final String NS_RESLETCONFIG_URL = "netsuite.tsg.Integration.restlet.url.config";
	//public static final String NS_INTEG_MAPID = "netsuite.tsg.Integration.mapid";
	public static final String NS_REQUEST_JOBNAME = "netsuite.tsg.JobName";
	
    //Token Based Authentication       
	public static final String NS_CONSUMER_KEY = "netsuite.consumerKey";
	public static final String NS_CONSUMER_SECRET = "netsuite.consumerSecret";
	public static final String NS_TOKEN_ID = "netsuite.tokenId";
	public static final String NS_TOKEN_SECRET = "netsuite.tokenSecret";
	//File
	public static final String FILE_BASELOGDIR = "baselogdir";
	public static final String FILE_INBOX = "files.path.inbox";
	public static final String FILE_PROCESSED = "files.path.processed";
	public static final String FILE_ERROR = "files.path.error";
        
    //LOGIN PROCESS AND CSV STORAGE
	public static final String NS_CSV_STORAGE = "netsuite.csvStorage";
	public static final String NS_LOGIN_PROCESS = "netsuite.login";
    
	//CSV FILE DELIMITER
	public static final String NS_CSV_FILE_DELIMITER = "netsuite.csvFileDelimiter";
	
	//MAXIMUM ROW
	public static final String NS_MAX_ROW = "netsuite.maxRow";
	
	//FTP
	public static final String FTP_USERNAME = "netsuite.ftp.username";
	public static final String FTP_PASSWORD = "netsuite.ftp.password";
	public static final String FTP_HOST = "netsuite.ftp.host";
	public static final String FTP_PORT = "netsuite.ftp.port";
	public static final String FTP_PATH = "netsuite.ftp.path";
	

	public static final String SFTP_KNOWN_HOST_PATH = "netsuite.sftp.knownhostpath";
	public static final String SFTP_PRIVATE_KEY_PATH = "netsuite.sftp.privatekeypath";
	public static final String SFTP_PASSPHRASE = "netsuite.sftp.passphrase";
	
    //for write access
    public static final String FTP_REMOVEPROCESSED_FILE = "netsuite.ftp.removeProcessedFile";
    public static final String FTP_ERROR_DIR = "netsuite.ftp.errorDir";
    public static final String FTP_PROCESSED_DIR = "netsuite.ftp.processedDir";
	
	public static final String FILE_CHARSET = "files.Charset";
    
    
    public enum CSVStorage{
        FTP,
        SFTP,
        LOCAL
    }    
    public enum LoginPreference{
        OAUTH,
        NLAUTH
    }
    
    //Tooltip
    public static final String toolTip_csvStorage	=	"This corresponds to the location of CSV file/s.";
    public static final String toolTip_localFile 	=	"This corresponds to the CSV file on the local directory that is to be processed for the CSV import.";
    public static final String toolTip_localBtn	 	=	"Click this to choose a CSV file on the local directory.";
    public static final String toolTip_FtpLbl		=	"This corresponds to the storage of CSV file/s location on the FTP server.";
    public static final String toolTip_FtpUname 	=	"This corresponds to the username that will be used to login on the FTP server.";
    public static final String toolTip_FtpPwd 		=	"This corresponds to the password that will be used to login on the FTP server.";
    public static final String toolTip_FtpHost 		=	"This corresponds to the name of the remote host of the FTP server.";
    public static final String toolTip_FtpPort 		=	"This corresponds to the port to connect to on the remote host.";
    public static final String toolTip_FtpPath 		=	"<html><p>This corresponds to the path that will be set as the working directory upon connecting to the FTP server.</p><p> Pathname that starts with slash \"/\" is considered as absolute path. \nPathname that does not start with a slash is considered as relative path.</p><p>If path not existing, the working directory will be set to \"/\".</p></html>";
    public static final String toolTip_Process 		=	"Click this to start processing the csv file/s selected.";
    public static final String toolTip_login 		=	"<html><p>This corresponds to the login preference used to connect to the Netsuite Account.</p><p> OAUTH corresponds to token based authentication while NLAUTH corresponds to the standard netsuite authentication.</p></html>";
    public static final String toolTip_mapId 		=	"This corresponds to the id of the NSTS | Integration Mapping Record for the local file. If csv storage is FTP, mapping id is dynamically detected for each file.";
    public static final String toolTip_tokenId 		=	"This corresponds to the access token id generated for a specific user.";
    public static final String toolTip_tokenSecret 	=	"This corresponds to the access token secret generated for a specific user.";
    public static final String toolTip_uname 		=	"This corresponds to the username used to log in to the Netsuite Account.";
    public static final String toolTip_pwd 			=	"This corresponds to the password used to log in to the Netsuite Account.";
    public static final String toolTip_queue 		=	"For SuiteCloud Licensed Accounts, this corresponds to the alternate queue (other than the default of 1) to use for the import job.";

    //ERRORS
    public static final String error_noAUTH         =   "Login process should be NLAUTH and OAUTH only.";
    public static final String error_noCSVStorage   =   "CSV Storage(LOCAL) should be on local folder or ftp(FTP) server only.";
    public static final String error_noCSVmapIds    =   "No CSV integration mapping ids detected.";
}
