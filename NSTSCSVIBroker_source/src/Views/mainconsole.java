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

import helper.LoggerHelper;
import helper.SftpHelper;
import helper.UnicodeBOMInputStream;
import helper.fileOutputHelper;
import helper.fileMoveDetails;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Date;

import model.Constants;
import model.mapConfig;
import Exceptions.invalidcsvHeaderException;

import com.netsuite.tools.imex.file.PropertyFile;

import controller.WebRequesController;
import helper.ftpHelper;

import java.util.HashMap;
import java.util.Map;


/******************************************************************************************************************
 * CLASS MAINCONSOLE
 * CONSOLE APPLICATION 
 *****************************************************************************************************************/
public class mainconsole {
	private PropertyFile configNs;

	private String _baseLogfile;
	private String _inboxDir;
	private String _processingDir;
	private String _errorDir;
	private String _sDate;
    private boolean bsuiteCloudLAcct = false;
    private int csvqueue = 2;

    private Map<String, fileMoveDetails> ftpCsvFiles;
	
    fileOutputHelper inboxFolder;
	fileOutputHelper processingFolder;
	fileOutputHelper errorFolder;

	WebRequesController request;
    
    String LoginProc = "";
    String fileStorage = "";
    
    String ftpUsername; 
    String ftpPassword ;
    String ftpHost;
    String ftpPort;
    String ftpPath;

    private ftpHelper ftpClient;
    private SftpHelper sftpClient;
    
	public mainconsole() {
        
        try{
            
            //check if login preference and csv storage is set
            try{
                configNs = new PropertyFile("netsuite");
                LoginProc = configNs.getValue(Constants.NS_LOGIN_PROCESS);
                if(!isEmpty(LoginProc)){
                    LoginProc = LoginProc.toUpperCase();
                }
                if(!LoginProc.equals(Constants.LoginPreference.NLAUTH.toString()) && !LoginProc.equals(Constants.LoginPreference.OAUTH.toString()) ){
                    System.out.println(LoggerHelper.getDate()+ ":   "+Constants.error_noAUTH);
                    System.exit(1);
                }
                fileStorage = configNs.getValue(Constants.NS_CSV_STORAGE);
                if(!isEmpty(fileStorage)){
                    fileStorage = fileStorage.toUpperCase();
                }
                if(!fileStorage.equals(Constants.CSVStorage.LOCAL.toString()) && !fileStorage.equals(Constants.CSVStorage.FTP.toString()) && !fileStorage.equals(Constants.CSVStorage.SFTP.toString())){
                    System.out.println(LoggerHelper.getDate()+ ":   "+Constants.error_noCSVStorage);
                    System.exit(1);
                }
                //check if queuing is supported
                //check if CSV Import Queue must be enabled
                String suiteCloudLAcct = configNs.getValue(Constants.NS_SUITECLOUD);
                if(!isEmpty(suiteCloudLAcct)){
                    suiteCloudLAcct = suiteCloudLAcct.toUpperCase();
                    if(suiteCloudLAcct.equals("TRUE"))
                    {
                        bsuiteCloudLAcct = true;
                        if(bsuiteCloudLAcct){
                            String queue = configNs.getValue(Constants.NS_CSVQUEUE);
                            if(!isEmpty(queue)){
                                try{
                                    csvqueue = Integer.parseInt(queue);
                                    if(!(csvqueue > 1 && csvqueue < 6)){
                                        System.out.println(LoggerHelper.getDate()+":     ERROR: CSV import queue number must be within 2-5.");
                                        System.exit(1);
                                    }
                                }catch(Exception ex){
                                    System.out.println(LoggerHelper.getDate()+":     ERROR: Invalid queue number. " +ex.getMessage());
                                    System.exit(1);
                                }
                            }else{
                                    System.out.println(LoggerHelper.getDate()+":     ERROR: CSV import queue is required for multiqueue. ");
                                    System.exit(1);                                
                            }
                        }
                    }
                }else{
                }

                request = new WebRequesController(LoginProc);
                if(bsuiteCloudLAcct){
                    request.setQueue(csvqueue);
                }
            }catch(Exception ex){
                System.out.println(LoggerHelper.getDate()+ ":    ERROR:  "+Constants.error_noCSVmapIds);// +ex.toString());
                System.exit(1);
            }
            Date dtNow = new Date();

            //base log file setup
             _sDate = new SimpleDateFormat("MM-dd-yyyy").format(dtNow);
            if(request.getMapConfigTotal() < 1){
                System.out.println(LoggerHelper.getDate()+ ":   "+Constants.error_noCSVmapIds);
                System.exit(1); 
            }
            _baseLogfile = configNs.getValue(Constants.FILE_BASELOGDIR);		
            _baseLogfile = String.format("%sLOGS %s.log", _baseLogfile, _sDate);

            //read files on ftp server if csv storage is FTP
            if(fileStorage.equals(Constants.CSVStorage.LOCAL.toString())){
            	_inboxDir = configNs.getValue(Constants.FILE_INBOX);
            }else{
                try{           
                    System.out.println("**********\n");     
                    ftpUsername = configNs.getValue(Constants.FTP_USERNAME);
                    ftpPassword = configNs.getValue(Constants.FTP_PASSWORD);
                    ftpHost = configNs.getValue(Constants.FTP_HOST);
                    ftpPort = configNs.getValue(Constants.FTP_PORT);
                    ftpPath = configNs.getValue(Constants.FTP_PATH);
                    Boolean isSuccess = false;
                    
                    if(fileStorage.equals(Constants.CSVStorage.FTP.toString())){
                    	ftpClient = new ftpHelper();
                        System.out.println(LoggerHelper.getDate()+ ":    Connecting to FTP Server: "+ftpHost+"\n");
                        System.out.println(LoggerHelper.getDate()+ ":    Downloading all files to the directory: "+configNs.getValue(Constants.FILE_INBOX)+"\n");
                        isSuccess = ftpClient.readFTP(ftpUsername, ftpPassword, ftpHost, ftpPath, ftpPort);
                    }else if(fileStorage.equals(Constants.CSVStorage.SFTP.toString())){
                    	sftpClient = new SftpHelper();
                        System.out.println(LoggerHelper.getDate()+ ":    Connecting to SFTP Server: "+ftpHost+"\n");
                        System.out.println(LoggerHelper.getDate()+ ":    Downloading all files to the directory: "+configNs.getValue(Constants.FILE_INBOX)+"\n");
                        isSuccess = sftpClient.readFTP(ftpUsername, ftpPassword, ftpHost, ftpPath, ftpPort);
                    }
                    
                    if(isSuccess){                   
                        _inboxDir = configNs.getValue(Constants.FILE_INBOX);
                    }else{
                        System.out.println(LoggerHelper.getDate()+ ":    ERROR: There was an error found in downloading csv files from ftp server.");
                        System.out.println("**********\n");
                        System.exit(1);
                    }
                }catch(Exception ex){
                    System.out.println(LoggerHelper.getDate()+ ":    ERROR: "+ex.getMessage());
                    System.exit(1);
                }
                
            }
            _processingDir = configNs.getValue(Constants.FILE_PROCESSED);
            _errorDir = configNs.getValue(Constants.FILE_ERROR);

            //appLog = new fileOutputHelper(_baseLogfile,false);
            inboxFolder = new fileOutputHelper(_inboxDir);
            processingFolder = new fileOutputHelper(_processingDir);
            errorFolder = new fileOutputHelper(_errorDir);

            //set charset for encoding
            String charset = configNs.getValue(Constants.FILE_CHARSET);
            if(charset.isEmpty()){
                charset = "ISO-8859-1";
            }
            System.out.println("##############################################\n");
            System.out.println("CHAR SET\t:\t" + charset);
            System.out.println("##############################################\n");

            if(!isEmpty(_inboxDir)){
                
                ftpCsvFiles = new HashMap<String, fileMoveDetails>();
                processInbox();//******************MOVE FILES ON FTP LOCATION********************************************************************************                      
                String ftpWrite = configNs.getValue(Constants.FTP_REMOVEPROCESSED_FILE);
                if(!isEmpty(ftpWrite)){
                    ftpWrite = ftpWrite.trim().toLowerCase();
                    if(ftpWrite.equals("true") &&  !ftpCsvFiles.isEmpty() && fileStorage.equals(Constants.CSVStorage.FTP.toString())){                        
                        System.out.println(LoggerHelper.getDate()+ ":    MOVING FILES ON FTP LOCATION:\n");
                        String ftpErrorDir      = configNs.getValue(Constants.FTP_ERROR_DIR);
                        String ftpProcessedDir  = configNs.getValue(Constants.FTP_PROCESSED_DIR);
                        if(!isEmpty(ftpErrorDir) && !isEmpty(ftpProcessedDir)){
                            //System.out.println(ftpErrorDir + " "+ftpProcessedDir);
                            ftpClient.moveFiles(ftpUsername, ftpPassword, ftpHost, ftpPath, ftpPort,ftpCsvFiles);

                        }else{
                            System.out.println(LoggerHelper.getDate()+ ":    FTP ERROR and PROCESSED directory is required.");
                        }
                        System.out.println("*******************************************************\n");
                    }
                }
                //******************MOVE FILES ON FTP LOCATION********************************************************************************
            }
            
            StringBuilder argInfo = new StringBuilder();
			
			String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
			argInfo.append("*** End Time (Timestamp) : ");
			argInfo.append(timeStamp);
			argInfo.append("\n\n");
			System.out.println(argInfo.toString());
			
        }catch(Exception ex){
            System.out.println(LoggerHelper.getDate()+ ":   ERROR: "+ex.getMessage());
        }

	}

    /******************************************************************************************************************
     * FUNCTION processInbox
     * Read csv files downloaded/placed on the specified inbox folder and process for csv import
     *****************************************************************************************************************/
	public void processInbox() {
		Date today = new Date();
		SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss a");
		String date = DATE_FORMAT.format(today);
		String filename = "";
		String csvFilename;
		String fileExt;
		String errorLogFilename = String.format("ERROR LOG %s", _sDate);
		//System.out.println(inboxFolder.getAbsolutePath());
		for (File fl : inboxFolder.listFiles()) {
			fileOutputHelper flh = new fileOutputHelper(fl.getPath(), true);
			filename = fl.getName();
			try {

				fileExt = filename.substring(filename.lastIndexOf(".") + 1,filename.length());
				if (fileExt.equalsIgnoreCase("csv")) {
					System.out.println(LoggerHelper.getDate()+ ":    CSV FILE: " + filename);
					csvFilename = flh.getPath();
					String data = readFile(csvFilename);
					if(processData(data, 0, csvFilename)){                      
                        System.out.println(LoggerHelper.getDate()+ ":    SUCCESSFULLY PROCESSED FILE.");
                        csvFilename = String.format("%s%s", _processingDir, LoggerHelper.getDate()+ "_"+filename);
                        LoggerHelper.getAppLog().writeLine( String.format( "[SUCCESS @%s ]",date));
                        LoggerHelper.getAppLog().writeLine(" -filename: "+ csvFilename);
                        if(flh.moveTo2(csvFilename)){                           
                            System.out.println(LoggerHelper.getDate()+ ":    FILE MOVED TO PROCESSED FOLDER: " + csvFilename);
                        }else{
                            System.out.println(LoggerHelper.getDate()+ ":    FILE NOT MOVED TO PROCESSED FOLDER: " + csvFilename);                           
                        }
                        System.out.println(LoggerHelper.getDate()+ ":    IMPORT CSV : CSV File Added to Import Queue\n");
                        //move to process folder in FTP
                        ftpCsvFiles.put(csvFilename, new fileMoveDetails(filename,"processed"));       
                    }else{
                        csvFilename = String.format("%s%s", _errorDir, LoggerHelper.getDate()+ "_"+filename);
                        LoggerHelper.getAppLog().writeLine( String.format( "[ERROR @%s]",date));
                        LoggerHelper.getAppLog().writeLine(" -filename: "+ csvFilename);
                        LoggerHelper.getAppLog().writeLine(" -message: ERROR IN PROCESSING FILE");
                        if(flh.moveTo2(csvFilename)){
                            System.out.println(LoggerHelper.getDate()+ ":    FILE MOVED TO ERROR FOLDER: " + csvFilename); 
                        }else{
                            System.out.println(LoggerHelper.getDate()+ ":    FILE NOT MOVED TO ERROR FOLDER: " + csvFilename); 
                        }
                        System.out.println(LoggerHelper.getDate()+ ":    IMPORT CSV : ERROR OCCUR");     
                        //move to process folder in FTP
                        ftpCsvFiles.put(csvFilename, new fileMoveDetails(filename,"error"));                 
                    }
					
				}
			} catch (Exception ex) {
				csvFilename = String.format("%s%s", _errorDir, LoggerHelper.getDate()+ "_"+filename);
				System.out.println(LoggerHelper.getDate()+ ":    IMPORT CSV : ERROR OCCUR");
				try {
					System.out.println(LoggerHelper.getDate()+ ":    ERR:" + ex.toString());
					if(flh.moveTo2(csvFilename)){
                        System.out.println(LoggerHelper.getDate()+ ":    FILE MOVED TO ERROR FOLDER: " + csvFilename);
                    }else{
                        System.out.println(LoggerHelper.getDate()+ ":    FILE NOT MOVED TO ERROR FOLDER: " + csvFilename);
                    }
					fileOutputHelper eLogs = LoggerHelper.getAppLog().createFileOutput(errorLogFilename + "_" + filename + ".log");
					eLogs.writeLine("#[START]=================================================#"); 
					eLogs.writeLine(date);
					eLogs.writeLine(ex.toString()); 
					for(StackTraceElement ste : ex.getStackTrace()){
						eLogs.writeLine(String.format("\t%6d : %s.%s\t#%s",ste.getLineNumber(), ste.getClassName(),ste.getMethodName(),ste.getClass())); 
					}
					eLogs.writeLine("#[END]===================================================#"); 
                    //move to process folder in FTP
                    ftpCsvFiles.put(csvFilename, new fileMoveDetails(filename,"error"));
					
					LoggerHelper.getAppLog().writeLine( String.format( "[ERROR @%s]",date));
					LoggerHelper.getAppLog().writeLine(" -filename: "+ csvFilename);
					LoggerHelper.getAppLog().writeLine(" -message: "+ ex.toString());
					
				} catch (Exception ex2) {
					ex.printStackTrace();
				}
				System.out.println(LoggerHelper.getDate()+ ":    IMPORT CSV : Has Failed\n");
			}
		}

	}

    /******************************************************************************************************************
     * FUNCTION processData
     * @param data, option 0: to auto identify mapConfig, filename
     * Sends csv data for csv import
     *****************************************************************************************************************/
	private boolean processData(String data, int option, String fileName){
        try{           
            boolean result = true;
            if (data.isEmpty())
                return false;

            switch (option) {
                case 0:
                    String csvHeader = data.split("\n")[0];
                    mapConfig config = request.getMapConfig(csvHeader);
                    String id = "";
                    String pass = "";
                    String delimiter = configNs.getValue(Constants.NS_CSV_FILE_DELIMITER);

                    if(delimiter.isEmpty()){
                    	delimiter = ",";
                    }else{
                    	if(!delimiter.equals(",") && !delimiter.equals("\\|") && !delimiter.equals(";") && !delimiter.equals("\\s") && !delimiter.equals("\t")){
                    		throw new InvalidParameterException("Invalid CSV Delimiter defined in the properties file.");
                    	}
                    }
                    
                    if (config != null) {
                        if(LoginProc.equals(Constants.LoginPreference.NLAUTH.toString()) ){
                            id = configNs.getValue(Constants.NS_EMAIL);
                            pass = configNs.getValue(Constants.NS_PASSWORD);
                        }else{
                            id = configNs.getValue(Constants.NS_TOKEN_ID);
                            pass = configNs.getValue(Constants.NS_TOKEN_SECRET);                   
                        }
                        result = request.createWebRequest(LoginProc, config.id, id,pass,data, fileName, delimiter);
                        if(!result){
                            System.out.println(LoggerHelper.getDate()+ ":    PROCESS ERROR: "+request.getError());
                        }
                    }else{
                        throw new invalidcsvHeaderException();
                    }
                    break;
            }
            return result;
        }catch(Exception ex){
            System.out.println(LoggerHelper.getDate()+ ":    PROCESS ERROR: "+ex.getMessage());
            return false;
        }
	}
    /******************************************************************************************************************
     * FUNCTION readFile
     * @param csv filename
     * Reads csv file and encode it to preferred charset specified in the properties
     *****************************************************************************************************************/
	
	public String readFile(String csvFilename) {
		BufferedReader csvFile = null;
		try {
			String sCurrentLine;
			//csvFile = new BufferedReader(new FileReader(csvFilename));
			String charset = configNs.getValue(Constants.FILE_CHARSET);
			if(charset.isEmpty()){
				charset = "ISO-8859-1";
			}
			StringBuilder csvSB = new StringBuilder();
			try {
				
				UnicodeBOMInputStream ubis = new UnicodeBOMInputStream(new FileInputStream(csvFilename));
				csvFile = new BufferedReader(new InputStreamReader(ubis ,charset));
				ubis.skipBOM();
				while ((sCurrentLine = csvFile.readLine()) != null) {
					csvSB.append(sCurrentLine + "\n");
				}
			}finally {
				csvFile.close();				
			}
			return csvSB.toString();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return "";
	}
    
    /******************************************************************************************************************
     * FUNCTION isEmpty
     * Check if string is empty
     *****************************************************************************************************************/
    private boolean isEmpty(String word){
        try{
            if(!word.isEmpty()){
                if((word.trim()).length() > 0)
                    return false;
                else
                    return true;
            }
            return true;
        }catch(Exception ex){
            return true;
        }
    }
}