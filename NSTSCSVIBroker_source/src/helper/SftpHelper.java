/**
 * 
 */
package helper;


import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import model.Constants;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.netsuite.tools.imex.file.PropertyFile;

/**
 * @author vbien
 *
 */
public class SftpHelper {
	
	private PropertyFile configNs;
	private String inboxFolder = "";
	private String processedFolder = "";
	private String errorFolder = "";
	private String downloadDetails = "";
	private String privateKeyPath = "";
	private String knownHostsPath = "";
	private String passphrase = "";
    private String downloadedFiles = "";
    
	private JSch jsch = new JSch();
	
	public SftpHelper(){
		configNs = new PropertyFile("netsuite");
		inboxFolder = configNs.getValue(Constants.FILE_INBOX);
		processedFolder = configNs.getValue(Constants.FILE_PROCESSED);
		errorFolder = configNs.getValue(Constants.FILE_ERROR);

		privateKeyPath = configNs.getValue(Constants.SFTP_PRIVATE_KEY_PATH);
		knownHostsPath = configNs.getValue(Constants.SFTP_KNOWN_HOST_PATH);
		passphrase = configNs.getValue(Constants.SFTP_PASSPHRASE);
	}
	
	public boolean readFTP(String username, String password, String host, String filePath, String port) throws Exception
    {
		try {
			JSch jsch = new JSch();
			
			Integer portInt = Integer.parseInt(port);
			
			jsch.addIdentity(privateKeyPath, passphrase);
			System.out.println("identity added ");
			
			System.out.println("Username | Password | Host | Port | File Path : " + username + " | " + password + " | " + host + " | " + port + " | " + filePath);
			
			Session session = jsch.getSession(username, host, portInt);
			System.out.println("session created.");
			
			jsch.setKnownHosts(knownHostsPath);
			/*Properties prop = new Properties();
	        prop.put("StrictHostKeyChecking", "no");
	        
	        session.setConfig(prop);*/
			
			session.connect();
			System.out.println("session connected.....");
			
			if(checkDirExist(inboxFolder)){
				System.out.println("********************Downloading all csv files in the directory******************");
				downloadDetails =  downloadDetails + "********************Downloading all csv files in the directory******************\n";
				
				Channel channel = session.openChannel("sftp");
				channel.setInputStream(System.in);
				channel.setOutputStream(System.out);
				channel.connect();
				System.out.println("shell channel connected....");
				
				ChannelSftp channelSftp = (ChannelSftp) channel;
								
				List<ChannelSftp.LsEntry> list = getFiles(channelSftp, filePath, inboxFolder);
				
				try {
					for (ChannelSftp.LsEntry file : list) {
						String csvFile = file.getFilename();
						
						String fileExt = csvFile.substring(csvFile.lastIndexOf(".") + 1,csvFile.length()).toLowerCase();
						
						if(fileExt.equalsIgnoreCase("csv")) {
							channelSftp.get(file.getFilename(), file.getFilename());
						}
					}
				}finally {
					if (channelSftp != null){
						channelSftp.disconnect();
					}
				}
				
				downloadDetails =  downloadDetails + "*****************************Download END************************************\n";
				System.out.println("*****************************Download END************************************");
				System.out.println(LoggerHelper.getDate()+ ":    FTP DISCONNECTED: SUCCESSFUL READ");
				
				channelSftp.exit();

				session.disconnect();
				return true;
			}else{
				System.out.println(LoggerHelper.getDate() + ":    ERROR: DIR NOT EXISTING: " + inboxFolder);
				session.disconnect();
                return false;	
			}
		} catch (Exception e) {
			System.err.println(e);
			return false;
		}
	}

	public static List<ChannelSftp.LsEntry> getFiles(ChannelSftp sftpChannel, String srcDir, String destDir) throws Exception {
		 sftpChannel.lcd(destDir);
		 sftpChannel.cd(srcDir);
	
		//Get a listing of the remote directory
		@SuppressWarnings("unchecked")
		List<ChannelSftp.LsEntry> list = sftpChannel.ls(".");
		
		return list;
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

	public String getDownloadedDetails(){
        return downloadDetails;
    }
	
    public String[] getDownloadedFiles(){
        if(!downloadedFiles.equals(""))
            return downloadedFiles.split(",");
        return null;
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
}
