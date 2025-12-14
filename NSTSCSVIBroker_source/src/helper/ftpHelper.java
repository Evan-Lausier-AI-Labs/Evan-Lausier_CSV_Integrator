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

import com.netsuite.tools.imex.file.PropertyFile;


import helper.ftpHelper;
import helper.fileMoveDetails;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import model.Constants;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.nio.file.Paths;
import java.util.Map;
/**
 *
 * @author rilagan
 */
public class ftpHelper {
    
    private PropertyFile configNs;
    private String inboxFolder = "";
    private String processedFolder = "";
    private String errorFolder = "";
    private String downloadDetails = "";
    private String downloadedFiles = "";
    public FTPClient ftp = new FTPClient();
        
    public ftpHelper(){
        configNs = new PropertyFile("netsuite");
        inboxFolder = configNs.getValue(Constants.FILE_INBOX);
        processedFolder = configNs.getValue(Constants.FILE_PROCESSED);
        errorFolder = configNs.getValue(Constants.FILE_ERROR);
        
    }
    public  boolean readFTP(String username, String password,String host, String path, String port) throws Exception
    {
        String ftppassword = password;
        String ftpserver = host;
        String ftpusername = username;
        ftp.setConnectTimeout(10*1000);
        //Login
        if(!port.isEmpty()){
            ftp.connect(ftpserver,Integer.parseInt(port));
        }else{
            ftp.connect(ftpserver);
        }
        if(!ftpusername.isEmpty() && !ftpusername.isEmpty()){           
            ftp.enterLocalPassiveMode();
            ftp.login(ftpusername, ftppassword);
        }
        if(path.isEmpty()){
           path = "/"; 
        }
        try
        {
            int reply;
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply))
            {
                ftp.disconnect();
                System.out.println(LoggerHelper.getDate()+ ":    FTP server refused connection.");
                return false;
            } else
            {
                ftp.changeWorkingDirectory(path);
                System.out.println(LoggerHelper.getDate()+ ":    Working directory is set to: "+ftp.printWorkingDirectory());
                downloadDetails =  downloadDetails + LoggerHelper.getDate()+ ":    Working directory is set to: "+ftp.printWorkingDirectory() + ".\n";
                FTPFile[] ftpFiles = ftp.listFiles();
                boolean dirExist = checkDirExist(inboxFolder);
                if(!dirExist){
                    System.out.println(LoggerHelper.getDate()+ ":    ERROR: DIR NOT EXISTING: "+inboxFolder);
                    return false;
                }
                System.out.println("********************Downloading all csv files in the directory******************");
                downloadDetails =  downloadDetails + "********************Downloading all csv files in the directory******************\n";
                for (FTPFile ftpFile : ftpFiles)
                {
                    String csvFile = ftpFile.getName();
                    ftp.setFileType(FTP.BINARY_FILE_TYPE);         
                    String fileExt = csvFile.substring(csvFile.lastIndexOf(".") + 1,csvFile.length()).toLowerCase();
                    // APPROACH #1: using retrieveFile(String, OutputStream)
                    if (fileExt.equalsIgnoreCase("csv")) {
                        String filePath = Paths.get(inboxFolder+"/"+csvFile).toString();
                        File downloadFile1 = new File(filePath);
                        OutputStream outputStream1 = new BufferedOutputStream(new FileOutputStream(downloadFile1));
                        boolean success = false;
                        try{
                        	success = ftp.retrieveFile(csvFile, outputStream1);
                        }finally{
                        	outputStream1.close();
                        }                       
                        if (success) {
                            System.out.println(LoggerHelper.getDate()+ ":    File "+csvFile+" has been downloaded successfully.");
                            downloadDetails =  downloadDetails +LoggerHelper.getDate()+ ":   "+ csvFile + "\n";
                            downloadedFiles = csvFile+",";
                        }
                    }

                }
                downloadDetails =  downloadDetails + "*****************************Download END************************************\n";
                System.out.println("*****************************Download END************************************");
            }
            System.out.println(LoggerHelper.getDate()+ ":    FTP DISCONNECTED: SUCCESSFUL READ");
            ftp.disconnect();
            return true;
        } catch (Exception ex)
        {
            ftp.disconnect();
            System.out.println(LoggerHelper.getDate()+ ":    FTP DISCONNECTED:   UNSUCCESSFUL READ");
            System.out.println(LoggerHelper.getDate()+ ":Exception ----->" + ex.getMessage());
            return false;
        }
    }
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
				csvFile = new BufferedReader(new InputStreamReader( new FileInputStream(csvFilename),charset));//StandardCharsets.ISO_8859_1));
				while ((sCurrentLine = csvFile.readLine()) != null) {
					csvSB.append(sCurrentLine + "\n");
				}	
			}finally {
				if (csvFile != null)
					csvFile.close();
			}
			
			return csvSB.toString();

		} catch (Exception ex) {
			ex.printStackTrace();

		}
		return "";
	}
    public String getDownloadedDetails(){
        return downloadDetails;
    }
    public String[] getDownloadedFiles(){
        if(!downloadedFiles.equals(""))
            return downloadedFiles.split(",");
        return null;
    }
    public boolean checkDirExist(String path){
        try{
            path = Paths.get(path).toString();
            File f = new File(path);
            if (!f.exists()) {
                System.out.println(LoggerHelper.getDate()+ ":    LOCAL PATH NOT existing: "+path);
                boolean success = (new File(path)).mkdirs();
                System.out.println(LoggerHelper.getDate()+ ":    TO MAKE LOCAL DIR: "+success);
                if (!success) {
                    return false;
                }
            }else{
                System.out.println(LoggerHelper.getDate()+ ":    LOCAL PATH EXISTING: "+path);
            }
            return true;
        }catch(Exception ex){
            System.out.println(LoggerHelper.getDate()+ ":    ERROR: "+ex.getMessage());
            return false;
        }
    }
    public boolean moveFiles(String username, String password,String host, String path, String port, Map<String, fileMoveDetails> csvFiles) throws Exception{
        downloadDetails = "";
        String ftppassword = password;
        String ftpserver = host;
        String ftpusername = username;
        ftp.setConnectTimeout(10*1000);
        //Login
        if(!port.isEmpty()){
            ftp.connect(ftpserver,Integer.parseInt(port));
        }else{
            ftp.connect(ftpserver);
        }
        if(!ftpusername.isEmpty() && !ftpusername.isEmpty()){           
            ftp.login(ftpusername, ftppassword);
        }
        if(path.isEmpty()){
            path = "/"; 
        }
        try{
            int reply;
        
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply))
            {
                ftp.disconnect();
                downloadDetails =   downloadDetails+LoggerHelper.getDate()+ ":    FTP server refused connection.\n";
                System.out.println(LoggerHelper.getDate()+ ":    FTP server refused connection.");
                return false;
            } else{
                System.out.println(LoggerHelper.getDate()+ ":    FTP server connected.");
                ftp.enterLocalPassiveMode();
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                ftp.changeWorkingDirectory(path);
                
                //CHECK PROCESSED DIR AND ERROR DIR
                int csvCount = 0;
                for (Map.Entry<String,  fileMoveDetails> entry : csvFiles.entrySet()) {
                    ftp.changeWorkingDirectory(path);
                    String key = entry.getKey();
                    fileMoveDetails filedetails = entry.getValue();
                    String ftpDir = filedetails.getPath();
                    String ftpFileName = filedetails.getFilename();
                    
                    String toStorePath = "";
                    if(ftpDir.equals("error")){
                        toStorePath = configNs.getValue(Constants.FTP_ERROR_DIR);
                    }else{
                        toStorePath = configNs.getValue(Constants.FTP_PROCESSED_DIR);               
                    }
            
                    boolean result = checkDirectoryExists(toStorePath);
                    //System.out.println(key + ": "+value+" checkDirectoryExists: "+ result);
                    ftp.changeWorkingDirectory(path);
                    if(result){
                        moveFile(key,ftpFileName,toStorePath);
                        csvCount++;
                    }
                }
                if(csvCount == 0){
                    downloadDetails = downloadDetails+LoggerHelper.getDate()+ ":    There are no csv files moved in the ftp directory.\n";
                    System.out.println(LoggerHelper.getDate()+ ":    There are no csv files moved in the ftp directory.\n");
                }
            }
            ftp.disconnect();
        }catch(Exception ex){
            System.out.println(ex.toString());
            ftp.disconnect();
            
        }
        return true;
    }
    
    private boolean checkDirectoryExists(String dirPath) throws IOException {
        dirPath = getPath(dirPath);
        ftp.changeWorkingDirectory(dirPath);
        int returnCode = ftp.getReplyCode();
        if (returnCode == 550) {
            if(ftp.makeDirectory(dirPath))
            {   
                downloadDetails = downloadDetails+LoggerHelper.getDate()+ ":    Directory created: "+ftp.printWorkingDirectory()+"\n";
                return true;
            }else{
                downloadDetails = downloadDetails+LoggerHelper.getDate()+ ":    Directory not successfully created: \n";
                return false;
            }
        }else{
            return true;
        }
    }
    public boolean moveFile(String localPath, String origFileName,String dir){
        try{
            File localFile = new File(Paths.get(localPath).toString());
            String csvFile = localFile.getAbsolutePath().substring(localFile.getAbsolutePath().lastIndexOf("\\")+1);
            //System.out.println(localPath + " csvfile: "+csvFile+ " dir:"+dir);
            InputStream inputStream = new FileInputStream(localFile);
            String storePath = dir + "/"+csvFile;
            boolean done = false;
            try{
            	done = ftp.storeFile(getPath(storePath), inputStream);
            }finally{
                inputStream.close();
            }
            downloadDetails = downloadDetails+LoggerHelper.getDate()+ ":    Moving file: "+origFileName+" to the directory "+dir+" with filename: "+csvFile+".\n";
            System.out.println(LoggerHelper.getDate()+ ":    Moving file: "+origFileName+" to the directory "+dir+" with filename: "+csvFile+".");
            System.out.println(LoggerHelper.getDate()+ ":    Working directory: "+ftp.printWorkingDirectory());
            if (done) {
                System.out.println(LoggerHelper.getDate()+ ":    File successfully moved to "+dir+".");
                downloadDetails = downloadDetails+LoggerHelper.getDate()+ ":    File successfully moved to "+dir+"."+"\n";
                done = ftp.deleteFile(origFileName);
                if(done){
                    System.out.println(LoggerHelper.getDate()+ ":    File deleted on the working directory path.");
                    downloadDetails = downloadDetails+LoggerHelper.getDate()+ ":    File deleted on the working directory path.\n";
                }else{
                    System.out.println(LoggerHelper.getDate()+ ":    File not deleted on the working directory path.");
                    downloadDetails = downloadDetails+LoggerHelper.getDate()+ ":    File not deleted on the working directory path.\n";
                }
                return true;
            }else{
                System.out.println(LoggerHelper.getDate()+ ":    File not successfully moved to "+dir+".");
                downloadDetails = downloadDetails+LoggerHelper.getDate()+ ":    File not successfully moved to "+dir+"."+"\n";
                return false;
            }
        }catch(Exception ex){
            System.out.println(LoggerHelper.getDate()+ ":   "+ex.getMessage());
        }
        return false;
    }
    public String getPath(String path){
        String getPath = Paths.get(path).toString();
        path = getPath.replace("\\", "/");
        return path;
    }
}
