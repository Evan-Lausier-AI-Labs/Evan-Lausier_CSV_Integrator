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

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ScrollPaneConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import model.CimboboxItem;
import model.Constants;
import model.mapConfig;
import helper.ftpHelper;
import Exceptions.invalidcsvHeaderException;

import com.netsuite.tools.imex.file.PropertyFile;

import controller.WebRequesController;
import helper.LoggerHelper;
import helper.SftpHelper;
import helper.fileOutputHelper;
import helper.fileMoveDetails;

import java.io.File;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/******************************************************************************************************************
 * CLASS MAINVIEW
 * APPLICAN RUN VIA USER INTERFACE
 *****************************************************************************************************************/
public class mainview extends JFrame {

    //Labels
	private JLabel lblCSVStorage    = new JLabel("CSV STORAGE:");
    private JLabel lblCSVLocal      = new JLabel("LOCAL FILE:");
    private JLabel lblCSVFTP        = new JLabel("FTP:");
    private JLabel lblQueue         = new JLabel("CSV Import Queue:");
    private JLabel lblLogin         = new JLabel("LOGIN VIA:");            	
	private JLabel lblNsEmail       = new JLabel("TOKEN ID");		
	private JLabel lblPassword      = new JLabel("TOKEN SECRET");
    private JLabel lblFTPUsername   = new JLabel("Username:");
    private JLabel lblFTPPassword   = new JLabel("Password:");
    private JLabel lblFTPHost       = new JLabel("Host:");
    private JLabel lblFTPFilePath   = new JLabel("File Path:");
    private JLabel lblFTPPort       = new JLabel("Port:");
    //Textfields
	private JTextField txtFTPUsername       = new JTextField();
	private JPasswordField txtFTPPassword   = new JPasswordField();
	private JTextField txtFTPHost           = new JTextField();
	private JTextField txtFTPFilePath       = new JTextField();
	private JTextField txtFTPPort           = new JTextField();
        
	private JTextArea textArea          = new JTextArea();
	private JButton btnCsv              = new JButton("_");
	private JTextArea textArea_1        = new JTextArea();
	private String csvFullFilename      = "";
	private String csvFileName          = "";
	private JTextField txtEmail         = new JTextField();
	private JPasswordField txtPassword  = new JPasswordField();
	private JButton btnProc             = new JButton("Process");
	private JComboBox cmbo_mapid        = new JComboBox<>();
    private String[] CSVStorage         = {Constants.CSVStorage.LOCAL.toString(),Constants.CSVStorage.LOCAL.FTP.toString(),Constants.CSVStorage.SFTP.toString()};
    private String[] LoginProcess       = {Constants.LoginPreference.OAUTH.toString(),Constants.LoginPreference.NLAUTH.toString()};
    private String[] Queue              = {"2","3","4","5"};            
	private JComboBox cmbo_csv      = new JComboBox(CSVStorage);
	private JComboBox cmbo_login    = new JComboBox(LoginProcess);
	private JComboBox cmbo_queue    = new JComboBox(Queue);
	
	private PropertyFile configNs;
	
	private WebRequesController request;
	
    //OAUTH
    private String consumerKey;
    private String consumerSecret;
    private String tokenId;
    private String tokenSecret;
    
    //NLAUTH
    private String username;
    private String password;
    private String account;
    private String role;
    private String restletUrl;
    
    //CSV Local STORAGE
    private String csvFile;
    
    //CSV FTP STORAGE
    private String ftpUsername;
    private String ftpPassword;
    private String ftpHost;
    private String ftpPort;
    private String ftpPath;
    private String sftpKnownHostPath;
    private String sftpPrivateKeyPath;
    
    //CSV STORAGE PROCESS
    private String csvStorage;
    
    //ERROR STORAGE
    private String validationError;
    //LOGIN PROCESS
    private String LoginProc;
    
    //Suite Cloud Licensed Account
    private String suiteCloudLAcct;
    private boolean bsuiteCloudLAcct = false;
    
    //for ftp processing
    private String _baseLogfile;
	private String _inboxDir;
	private String _processingDir;
	private String _errorDir;
	private String _sDate;
    private String ftpResult = "";

	fileOutputHelper inboxFolder;
	fileOutputHelper processingFolder;
	fileOutputHelper errorFolder;
    
    private ftpHelper ftpClient ;
    private SftpHelper sftpClient ;
    private Map<String, fileMoveDetails> ftpCsvFiles;
    
    /******************************************************************************************************************
     * CONSTRUCTOR
     * Check if csv storage and login preference is specified to run application
     *****************************************************************************************************************/
	public mainview() {
        try{       
            try{
                configNs = new PropertyFile("netsuite");
            }catch(Exception error){
                System.out.println("ERROR: Cannot find properties file.");
                System.exit(1);
            }
            
            //create request to get integration mapping ids
            try{      
                LoginProc = configNs.getValue(Constants.NS_LOGIN_PROCESS);
                if(!isEmpty(LoginProc)){
                    LoginProc = LoginProc.toUpperCase();
                }
                if(!LoginProc.equals(Constants.LoginPreference.NLAUTH.toString()) && !LoginProc.equals(Constants.LoginPreference.OAUTH.toString()) ){
                    System.out.println(Constants.error_noAUTH);
                    System.exit(1);
                }
                request = new WebRequesController(LoginProc);
                if(request.getMapConfigTotal() < 1){
                    System.out.println(Constants.error_noCSVStorage);
                    System.exit(1); 
                }
            }catch(Exception ex){
                System.out.println("ERROR: "+Constants.error_noCSVmapIds);//+ex.toString());
                //System.exit(1);
            }
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setBounds(100, 100, 800, 700);
			getContentPane().setLayout(new GridLayout(0, 1, 0, 0));
			
			JPanel panel = new JPanel();
			getContentPane().add(panel);
			
            //set label tool tip
            lblCSVStorage.setToolTipText(Constants.toolTip_csvStorage);    
            lblCSVLocal.setToolTipText(Constants.toolTip_localFile);      
            lblCSVFTP.setToolTipText(Constants.toolTip_FtpLbl);        
            lblLogin.setToolTipText(Constants.toolTip_login);         
            lblNsEmail.setToolTipText(Constants.toolTip_tokenId);       
            lblPassword.setToolTipText(Constants.toolTip_tokenSecret);      
            lblFTPUsername.setToolTipText(Constants.toolTip_FtpUname);   
            lblFTPPassword.setToolTipText(Constants.toolTip_FtpPwd);   
            lblFTPHost.setToolTipText(Constants.toolTip_FtpHost);       
            lblFTPFilePath.setToolTipText(Constants.toolTip_FtpPath);   
            lblFTPPort.setToolTipText(Constants.toolTip_FtpPort); 
            lblQueue.setToolTipText(Constants.toolTip_queue); 
            
			textArea.setBackground(SystemColor.control);
			textArea.setEditable(false);
			textArea.setLineWrap(true);
			textArea.setWrapStyleWord(true);
			
			btnCsv.addActionListener(btnCsvEvent);
			btnCsv.setFont(new Font("Tahoma", Font.PLAIN, 9));
			btnCsv.setToolTipText(Constants.toolTip_localBtn);
            
			textArea_1.setBounds(5, 121, 415, 121);			
			textArea.setToolTipText(Constants.toolTip_localFile);
            
			JScrollPane scrollBar = new JScrollPane(textArea_1);
			scrollBar.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
            //set default on combobox
            cmbo_csv.setSelectedIndex(0);
			cmbo_csv.setToolTipText(Constants.toolTip_csvStorage);
            
            cmbo_login.setSelectedIndex(0);
			cmbo_login.setToolTipText(Constants.toolTip_login);
			cmbo_login.setEditable(false);
			cmbo_login.setEnabled(false);
			cmbo_login.setSelectedItem(LoginProc);
			cmbo_mapid.setToolTipText(Constants.toolTip_mapId);
            cmbo_queue.setToolTipText(Constants.toolTip_queue);
            
            //disable ftp username,password,host,port and filepath
            txtFTPUsername.setEditable(false);
			txtFTPUsername.setToolTipText(Constants.toolTip_FtpUname);
            
            txtFTPPassword.setEditable(false);
			txtFTPPassword.setToolTipText(Constants.toolTip_FtpPwd);
            
            txtFTPHost.setEditable(false);
			txtFTPHost.setToolTipText(Constants.toolTip_FtpHost);
            
            txtFTPFilePath.setEditable(false);
			txtFTPFilePath.setToolTipText(Constants.toolTip_FtpPath);
            
            txtFTPPort.setEditable(false);
			txtFTPPort.setToolTipText(Constants.toolTip_FtpPort);
            
			btnProc.setEnabled(true);
			btnProc.addActionListener(btnProcEvent);
			btnProc.setFont(new Font("Tahoma", Font.PLAIN, 9));
			btnProc.setToolTipText(Constants.toolTip_Process);
			
			JLabel lblMapId = new JLabel("MAP ID");
			lblMapId.setToolTipText(Constants.toolTip_mapId);
		
			//set default value
			txtEmail.setText(configNs.getValue(Constants.NS_TOKEN_ID));
			txtEmail.setToolTipText(Constants.toolTip_tokenId);
            
			txtPassword.setText(configNs.getValue(Constants.NS_TOKEN_SECRET));
			txtPassword.setToolTipText(Constants.toolTip_tokenSecret);
			
			//fill combo box with nsts csv integration mapping ids
			if (request.getAllMapConfig() != null){
				for (mapConfig config : request.getAllMapConfig()) {
					cmbo_mapid.addItem(new CimboboxItem(config.name, config.id)); 
				}
			}else{                
                System.out.println("ERROR: "+Constants.error_noCSVmapIds);
                System.exit(1);
            }
			
            cmbo_csv.addActionListener(comboCSVEvent);
			cmbo_login.addActionListener(comboLoginEvent);
			comboLoginEvent.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null) {
		          //Nothing need go here, the actionPerformed method (with the
		          //above arguments) will trigger the respective listener
		    });
            //check if CSV Import Queue must be enabled
            suiteCloudLAcct = configNs.getValue(Constants.NS_SUITECLOUD);
            if(!isEmpty(suiteCloudLAcct)){
                suiteCloudLAcct = suiteCloudLAcct.toUpperCase();
                if(suiteCloudLAcct.equals("TRUE"))
                {
                    cmbo_queue.setEnabled(true);
                    bsuiteCloudLAcct = true;
                }
                else
                    cmbo_queue.setEnabled(false);
            }else{
                cmbo_queue.setEnabled(false);
            }
            /******************************USER INTERFACE LAYOUT***********************************************************/
            GroupLayout gl_panel = new GroupLayout(panel);
			
			gl_panel.setHorizontalGroup(
				gl_panel.createParallelGroup(Alignment.TRAILING)
					.addGroup(gl_panel.createSequentialGroup()
						.addContainerGap()
						.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
							.addComponent(scrollBar, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 663, Short.MAX_VALUE)
							.addGroup(gl_panel.createSequentialGroup()
								.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
																.addComponent(lblCSVStorage)	
																.addGap(100)
																.addComponent(lblCSVLocal)	
																.addGap(100)
																.addComponent(lblCSVFTP))
															
								.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
																.addComponent(cmbo_csv, 20, 40, 80)
																.addComponent(textArea, GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
																
																.addGroup(gl_panel.createSequentialGroup()
																	.addGroup(gl_panel.createParallelGroup(GroupLayout.Alignment.LEADING)
																		.addComponent(lblFTPUsername)
																		.addComponent(lblFTPPassword))
																	.addGroup(gl_panel.createParallelGroup(GroupLayout.Alignment.LEADING)
																		.addComponent(txtFTPUsername,  GroupLayout.DEFAULT_SIZE, 100, 200)
																		.addComponent(txtFTPPassword, GroupLayout.DEFAULT_SIZE, 100, 200))
																	.addGroup(gl_panel.createParallelGroup(GroupLayout.Alignment.LEADING)
																		.addComponent(lblFTPHost)
																		.addComponent(lblFTPFilePath))
																	.addGroup(gl_panel.createParallelGroup(GroupLayout.Alignment.LEADING)
																		.addComponent(txtFTPHost, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE)
																		.addComponent(txtFTPFilePath, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE))
																	.addGroup(gl_panel.createParallelGroup(GroupLayout.Alignment.LEADING)                                                                   
																		.addComponent(lblFTPPort))
																	.addGroup(gl_panel.createParallelGroup(GroupLayout.Alignment.LEADING)                                                                   
																		.addComponent(txtFTPPort, 10, 10,50))))
								.addPreferredGap(ComponentPlacement.UNRELATED)
								.addComponent(btnCsv))
							.addGroup(gl_panel.createSequentialGroup()
								.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
									.addComponent(lblLogin)
									.addComponent(lblNsEmail)
									.addComponent(lblPassword)
									.addComponent(lblMapId)
									.addComponent(lblQueue))
								.addGap(18)
								.addGroup(gl_panel.createParallelGroup(Alignment.LEADING, false)
									.addComponent(cmbo_login, GroupLayout.PREFERRED_SIZE, 330, GroupLayout.PREFERRED_SIZE)
																	.addComponent(txtPassword)
									.addComponent(txtEmail, GroupLayout.DEFAULT_SIZE, 232, Short.MAX_VALUE)
									.addComponent(cmbo_mapid, GroupLayout.PREFERRED_SIZE, 330, GroupLayout.PREFERRED_SIZE)
									.addComponent(cmbo_queue, GroupLayout.PREFERRED_SIZE, 330, GroupLayout.PREFERRED_SIZE))
								.addPreferredGap(ComponentPlacement.RELATED, 180, Short.MAX_VALUE)
								.addComponent(btnProc)))
						.addContainerGap())
			);
			gl_panel.setVerticalGroup(
				gl_panel.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_panel.createSequentialGroup()
						.addContainerGap()
						.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
							.addComponent(lblCSVStorage)	
													.addGap(50)
							.addComponent(cmbo_csv, 20, 40, 80))
						
						.addContainerGap()
						.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
							.addComponent(lblCSVLocal)
							.addComponent(textArea, GroupLayout.PREFERRED_SIZE, 48, Short.MAX_VALUE)
							.addComponent(btnCsv, GroupLayout.DEFAULT_SIZE, 27, Short.MAX_VALUE))
											.addContainerGap()
											.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
												.addGroup(gl_panel.createSequentialGroup()
													.addComponent(lblCSVFTP)	
													.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
														.addComponent(lblFTPUsername)
														.addComponent(txtFTPUsername, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
															
													.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
														 .addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
														.addComponent(lblFTPHost)
														.addComponent(txtFTPHost, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
															
														.addComponent(lblFTPPort)
														.addComponent(txtFTPPort, 20,20,50)))
													.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
														.addComponent(lblFTPPassword)
														.addComponent(txtFTPPassword)
														.addComponent(lblFTPFilePath)
														.addComponent(txtFTPFilePath, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(scrollBar, GroupLayout.PREFERRED_SIZE, 322, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.UNRELATED)
						.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
							.addGroup(gl_panel.createSequentialGroup()
								.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
									.addComponent(lblLogin)
									.addComponent(cmbo_login, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
															.addGap(8)
								.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
									.addComponent(lblMapId)
									.addComponent(cmbo_mapid, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                    .addGap(8)
								.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
									.addComponent(lblQueue)
									.addComponent(cmbo_queue, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
								.addGap(8)
								.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
									.addComponent(lblNsEmail)
									.addComponent(txtEmail, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
								.addPreferredGap(ComponentPlacement.RELATED)
								.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
									.addComponent(txtPassword, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
									.addComponent(lblPassword)))
							.addComponent(btnProc, GroupLayout.PREFERRED_SIZE, 57, GroupLayout.PREFERRED_SIZE))
						.addGap(13))
			);
			panel.setLayout(gl_panel);		                
            /******************************END OF USER INTERFACE LAYOUT***********************************************************/
        }catch(Exception ex){
            JOptionPane optn = new JOptionPane("INVALID CREDENTIALS : " +  ex.getMessage(),JOptionPane.ERROR_MESSAGE);
            JDialog dialog = optn.createDialog("ERROR");
            dialog.setVisible(true);
            btnProc.setEnabled(false);
            System.exit(1);
            
            
        }
		
	}
	
    /******************************************************************************************************************
     * FUNCTION ActionListener btnCsvEvent
     * Filters csv files only to be selected on local folder when LOCAL is the csv storage
     *****************************************************************************************************************/
	private ActionListener btnCsvEvent = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			File dir = null;
            final JFileChooser fl = new JFileChooser();
            
            _inboxDir = configNs.getValue(Constants.FILE_INBOX);
            
            try{
                _inboxDir = configNs.getValue(Constants.FILE_INBOX);
                dir = new File(_inboxDir);
                if(!isEmpty(_inboxDir)){
                    fl.setCurrentDirectory(dir);
                }
                System.out.println(_inboxDir );
            }catch(Exception ex){  
                ex.printStackTrace();
            }
            /**if(dir != null){
                fl= new JFileChooser(_inboxDir);
            }else{
                fl= new JFileChooser();
            }*/
			fl.setFileFilter(new FileNameExtensionFilter("CSV", "CSV"));
			int status = fl.showOpenDialog(mainview.this);
		
			fl.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent arg0) {
				}
			});
			
			if(status == JFileChooser.APPROVE_OPTION){
				csvFullFilename = fl.getSelectedFile().getAbsolutePath();
				csvFileName = fl.getSelectedFile().getName();
				textArea.setText(csvFullFilename);
			}
		}
	};	
	/******************************************************************************************************************
     * FUNCTION ActionListener comboCSVEvent
     * FTP /LOCAL preference for CSV Storage
     *****************************************************************************************************************/	
    private ActionListener comboCSVEvent = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            String strCSVStorage = cmbo_csv.getSelectedItem().toString();
            if(strCSVStorage.equals(Constants.CSVStorage.FTP.toString()) || strCSVStorage.equals(Constants.CSVStorage.SFTP.toString())){
                //enable ftp username,password,host,port and filepath
                txtFTPUsername.setEditable(true);
                txtFTPPassword.setEditable(true);
                txtFTPHost.setEditable(true);
                txtFTPFilePath.setEditable(true);
                txtFTPPort.setEditable(true);
                
                //set values
                txtFTPUsername.setText(configNs.getValue(Constants.FTP_USERNAME));
                txtFTPPassword.setText(configNs.getValue(Constants.FTP_PASSWORD));
                txtFTPHost.setText(configNs.getValue(Constants.FTP_HOST));
                txtFTPFilePath.setText(configNs.getValue(Constants.FTP_PATH));
                txtFTPPort.setText(configNs.getValue(Constants.FTP_PORT));
                
                //disable local storage
                btnCsv.setEnabled(false);
                textArea.setText("");
                cmbo_mapid.setEnabled(false);
            }
            if(strCSVStorage.equals(Constants.CSVStorage.LOCAL.toString())){
                
                //enable local storage
                btnCsv.setEnabled(true);
                textArea.setText("");
                
                //disable ftp username,password,host,port and filepath
                txtFTPUsername.setEditable(false);
                txtFTPPassword.setEditable(false);
                txtFTPHost.setEditable(false);
                txtFTPFilePath.setEditable(false);
                txtFTPPort.setEditable(false);
                
                //clear text
                txtFTPUsername.setText("");
                txtFTPPassword.setText("");
                txtFTPHost.setText("");
                txtFTPFilePath.setText("");
                txtFTPPort.setText("");
                cmbo_mapid.setEnabled(true);
            }
        }
    };
    
    /******************************************************************************************************************
     * FUNCTION ActionListener comboLoginEvent
     * Login Preference/ NLAUTH for standard authentication or OAUTH for token based authentication
     *****************************************************************************************************************/	
    private ActionListener comboLoginEvent = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            String strCSVStorage = cmbo_login.getSelectedItem().toString();
            if(strCSVStorage.equals(Constants.LoginPreference.OAUTH.toString())){
                lblNsEmail.setText("TOKEN ID");
                lblPassword.setText("TOKEN SECRET");		
                txtEmail.setText(configNs.getValue(Constants.NS_TOKEN_ID));
                txtPassword.setText(configNs.getValue(Constants.NS_TOKEN_SECRET));
                
                //set tool tip
                lblNsEmail.setToolTipText(Constants.toolTip_tokenId);
                txtEmail.setToolTipText(Constants.toolTip_tokenId);
                lblPassword.setToolTipText(Constants.toolTip_tokenSecret);
                txtPassword.setToolTipText(Constants.toolTip_tokenSecret);
            }
            if(strCSVStorage.equals(Constants.LoginPreference.NLAUTH.toString())){
                lblNsEmail.setText("USERNAME");
                lblPassword.setText("PASSWORD");		
                txtEmail.setText(configNs.getValue(Constants.NS_EMAIL));
                txtPassword.setText(configNs.getValue(Constants.NS_PASSWORD));
                
                //set tool tip
                lblNsEmail.setToolTipText(Constants.toolTip_uname);
                txtEmail.setToolTipText(Constants.toolTip_uname);
                lblPassword.setToolTipText(Constants.toolTip_pwd);
                txtPassword.setToolTipText(Constants.toolTip_pwd);
                                    
            }
        }
    };
    
    /******************************************************************************************************************
     * FUNCTION validateForm
     * Check if required fields in form is filled up before submitting
     *****************************************************************************************************************/
    private boolean validateForm(){
        try{            
            String loginProcess = cmbo_login.getSelectedItem().toString();
            LoginProc = loginProcess;
            String strCSVStorage = cmbo_csv.getSelectedItem().toString();
            String mapId        = Integer.toString(((CimboboxItem)cmbo_mapid.getSelectedItem()).value);
            
            //check login
            restletUrl          = configNs.getValue(Constants.NS_RESLET_URL);
            account             = configNs.getValue(Constants.NS_ACCOUNT);

            if(!isEmpty(restletUrl) && !isEmpty(account)&& !isEmpty(mapId)){
                restletUrl          = restletUrl.trim(); 
                account             = account.trim(); 
            }else{
                validationError = "Account name, netsuite restlet url and csv integration mapping id is required.";
                return false;
            }
            if(loginProcess.equals(Constants.LoginPreference.OAUTH.toString())){
                consumerKey         = configNs.getValue(Constants.NS_CONSUMER_KEY);
                consumerSecret      = configNs.getValue(Constants.NS_CONSUMER_SECRET);
                tokenId             = txtEmail.getText();
                tokenSecret         = txtPassword.getText();

                if(!isEmpty(consumerKey)&& !isEmpty(consumerSecret) && !isEmpty(tokenId)  && !isEmpty(tokenSecret)  ){
                                consumerKey         = consumerKey.trim();   
                                consumerSecret      = consumerSecret.trim();
                                tokenId             = tokenId.trim();       
                                tokenSecret         = tokenSecret.trim(); 
                }else{
                    validationError = "Consumer key, consumer secret, token id and token secret is required on token based authentication.";
                    return false;
                }
            }else{                
                username    = txtEmail.getText();
                password    = txtPassword.getText();
                role        = configNs.getValue(Constants.NS_ROLE);
                if(!isEmpty(username) && !isEmpty(password) && !isEmpty(role)){
                    username    = username.trim();   
                    password    = password.trim();
                    role        = role.trim();       
                }else{
                    validationError = "Email/username, password and role is required on NLAuth.";
                    return false;
                }
            }

            //check csv storage
            if(strCSVStorage.equals(Constants.CSVStorage.LOCAL.toString())){
                csvFile = textArea.getText();
                csvStorage = Constants.CSVStorage.LOCAL.toString();
                System.out.println(" csvFile:"+csvFile);
                if(!isEmpty(csvFile)){
                    csvFile = csvFile.trim();
                }else{
                    validationError = "Local CSV file path is required.";
                    return false;
                }
            }else{
            	if(strCSVStorage.equals(Constants.CSVStorage.FTP.toString())){
            		csvStorage      = Constants.CSVStorage.FTP.toString();
            	}else if(strCSVStorage.equals(Constants.CSVStorage.SFTP.toString())){
            		csvStorage      = Constants.CSVStorage.SFTP.toString();
            	}
                
                ftpUsername     = txtFTPUsername.getText();
                ftpPassword     = txtFTPPassword.getText();
                ftpHost         = txtFTPHost.getText();
                ftpPort         = txtFTPPort.getText();
                ftpPath         = txtFTPFilePath.getText();
                if(!isEmpty(ftpHost)){                
                        ftpUsername     = ftpUsername.trim();   
                        ftpPassword     = ftpPassword.trim();
                        ftpHost         = ftpHost.trim();       
                        ftpPort         = ftpPort.trim();     
                        ftpPath         = ftpPath.trim(); 
                }else{
                    validationError = "FTP HOST is required.";
                    return false;
                }
            }
            return true;
        }catch(Exception ex){
            validationError = ex.getMessage().toString();
            return false;
        }
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
    
    /******************************************************************************************************************
     * FUNCTION processInbox
     * Process inbox folder for downloaded files in the ftp server
     *****************************************************************************************************************/ 
	public void processInbox() {
		String filename = "";
		String csvFilename;
		String fileExt;
		String errorLogFilename = String.format("ERROR LOG %s", LoggerHelper.getDate());
		System.out.println(inboxFolder.getAbsolutePath());
		for (File fl : inboxFolder.listFiles()) {
			fileOutputHelper flh = new fileOutputHelper(fl.getPath(), true);
			filename = fl.getName();
            
            System.out.println("main path: "+fl.getAbsolutePath());
			try {

				fileExt = filename.substring(filename.lastIndexOf(".") + 1,filename.length());
				if (fileExt.equalsIgnoreCase("csv")) {
					textArea_1.append(LoggerHelper.getDate()+ ":    CSV FILE: " + filename+ "\n");
					csvFilename = flh.getPath();
					String data = ftpClient.readFile(csvFilename);
					if(processData(data, 0, csvFilename)){                        
                        csvFilename = String.format("%s%s", _processingDir, LoggerHelper.getDate()+ "_"+filename);
                        textArea_1.append(request.getDetails());
                        request.resetDetails();
                        textArea_1.append(LoggerHelper.getDate()+ ":    SUCCESSFULLY PROCESSED FILE.\n");
                        
                        LoggerHelper.getAppLog().writeLine( String.format( "[SUCCESS @%s ]",LoggerHelper.getDate()));
                        LoggerHelper.getAppLog().writeLine(" -filename: "+ csvFilename);          
                        
                        //MOVE FILES TO PROCESSED DIRECTORY AND DELETE ON CURRENT DIRECTORY
                        if(flh.moveTo2(csvFilename)){
                            textArea_1.append(LoggerHelper.getDate()+ ":    FILE MOVED TO PROCESSED FOLDER: " + csvFilename+ "\n");
       
                        }else{
                            textArea_1.append(LoggerHelper.getDate()+ ":    FILE NOT MOVED TO PROCESSED FOLDER: " + csvFilename+ "\n");
                        }
                        textArea_1.append(LoggerHelper.getDate()+ ":    IMPORT CSV : CSV File Added to Import Queue\n\n"); 

                        //STORE CSV FILES PROCESSED IN HASHMAP
                        ftpCsvFiles.put(csvFilename, new fileMoveDetails(filename,"processed"));
                    }else{
                        csvFilename = String.format("%s%s", _errorDir, LoggerHelper.getDate()+ "_"+filename);
                        
                        LoggerHelper.getAppLog().writeLine( String.format( "[ERROR @%s]",LoggerHelper.getDate()));
                        LoggerHelper.getAppLog().writeLine(" -filename: "+ csvFilename);
                        LoggerHelper.getAppLog().writeLine(" -message:  ERROR OCCUR");
                        
                        //MOVE FILES TO ERROR DIRECTORY
                        if(flh.moveTo2(csvFilename)){
                            textArea_1.append(LoggerHelper.getDate()+ ":    FILE MOVED TO ERROR FOLDER: " + csvFilename+".\n"); 
                        }else{
                            textArea_1.append(LoggerHelper.getDate()+ ":    FILE NOT MOVED TO ERROR FOLDER: " + csvFilename+".\n");      
                        }   
                        
                        textArea_1.append(LoggerHelper.getDate()+ ":    IMPORT CSV : ERROR OCCUR\n\n");          
                        
                        //move to process folder in FTP
                        ftpCsvFiles.put(csvFilename, new fileMoveDetails(filename,"error"));
                    }
					
				}
			} catch (Exception ex) {
				csvFilename = String.format("%s%s", _errorDir, LoggerHelper.getDate()+ "_"+filename);
				try {
					System.out.println("#   ERROR:" + ex.toString());
                    
                    //MOVE FILES TO ERROR DIRECTORY
					if(flh.moveTo2(csvFilename)){                        
                        textArea_1.append(LoggerHelper.getDate()+ ":    FILE MOVED TO ERROR FOLDER: " + csvFilename+".\n");
                        textArea_1.append(LoggerHelper.getDate()+ ":    IMPORT CSV : ERROR OCCUR\n\n");
                    }else{                       
                        textArea_1.append(LoggerHelper.getDate()+ ":    FILE NOT MOVED TO ERROR FOLDER: " + csvFilename+".\n");
                        textArea_1.append(LoggerHelper.getDate()+ ":    IMPORT CSV : ERROR OCCUR\n\n");
                    }
					fileOutputHelper eLogs = LoggerHelper.getAppLog().createFileOutput(errorLogFilename + "_" + filename + ".log");
					eLogs.writeLine("#[START]=================================================#"); 
					eLogs.writeLine(LoggerHelper.getDate());
					eLogs.writeLine(ex.toString()); 
					for(StackTraceElement ste : ex.getStackTrace()){
						eLogs.writeLine(String.format("\t%6d : %s.%s\t#%s",ste.getLineNumber(), ste.getClassName(),ste.getMethodName(),ste.getClass())); 
					}
					eLogs.writeLine("#[END]===================================================#"); 
					
                    //move to process folder in FTP
                    ftpCsvFiles.put(csvFilename, new fileMoveDetails(filename,"error"));
                    
					LoggerHelper.getAppLog().writeLine( String.format( "[ERROR @%s]",LoggerHelper.getDate()));
					LoggerHelper.getAppLog().writeLine(" -filename: "+ csvFilename);
					LoggerHelper.getAppLog().writeLine(" -message: "+ ex.toString());
					
				} catch (Exception ex2) {
					ex.printStackTrace();
				}
                System.out.println(LoggerHelper.getDate()+ ":    IMPORT CSV : Has Failed\n\n");
			}
		}
	}
	
	public void processSFTPInbox() {
		String filename = "";
		String csvFilename;
		String fileExt;
		String errorLogFilename = String.format("ERROR LOG %s", LoggerHelper.getDate());
		System.out.println(inboxFolder.getAbsolutePath());
		for (File fl : inboxFolder.listFiles()) {
			fileOutputHelper flh = new fileOutputHelper(fl.getPath(), true);
			filename = fl.getName();
            
            System.out.println("main path: "+fl.getAbsolutePath());
			try {

				fileExt = filename.substring(filename.lastIndexOf(".") + 1,filename.length());
				if (fileExt.equalsIgnoreCase("csv")) {
					textArea_1.append(LoggerHelper.getDate()+ ":    CSV FILE: " + filename+ "\n");
					csvFilename = flh.getPath();
					String data = sftpClient.readFile(csvFilename);
					if(processData(data, 0, csvFilename)){                        
                        csvFilename = String.format("%s%s", _processingDir, LoggerHelper.getDate()+ "_"+filename);
                        textArea_1.append(request.getDetails());
                        request.resetDetails();
                        textArea_1.append(LoggerHelper.getDate()+ ":    SUCCESSFULLY PROCESSED FILE.\n");
                        
                        LoggerHelper.getAppLog().writeLine( String.format( "[SUCCESS @%s ]",LoggerHelper.getDate()));
                        LoggerHelper.getAppLog().writeLine(" -filename: "+ csvFilename);          
                        
                        //MOVE FILES TO PROCESSED DIRECTORY AND DELETE ON CURRENT DIRECTORY
                        if(flh.moveTo2(csvFilename)){
                            textArea_1.append(LoggerHelper.getDate()+ ":    FILE MOVED TO PROCESSED FOLDER: " + csvFilename+ "\n");
       
                        }else{
                            textArea_1.append(LoggerHelper.getDate()+ ":    FILE NOT MOVED TO PROCESSED FOLDER: " + csvFilename+ "\n");
                        }
                        textArea_1.append(LoggerHelper.getDate()+ ":    IMPORT CSV : CSV File Added to Import Queue\n\n"); 

                        //STORE CSV FILES PROCESSED IN HASHMAP
                        ftpCsvFiles.put(csvFilename, new fileMoveDetails(filename,"processed"));
                    }else{
                        csvFilename = String.format("%s%s", _errorDir, LoggerHelper.getDate()+ "_"+filename);
                        
                        LoggerHelper.getAppLog().writeLine( String.format( "[ERROR @%s]",LoggerHelper.getDate()));
                        LoggerHelper.getAppLog().writeLine(" -filename: "+ csvFilename);
                        LoggerHelper.getAppLog().writeLine(" -message:  ERROR OCCUR");
                        
                        //MOVE FILES TO ERROR DIRECTORY
                        if(flh.moveTo2(csvFilename)){
                            textArea_1.append(LoggerHelper.getDate()+ ":    FILE MOVED TO ERROR FOLDER: " + csvFilename+".\n"); 
                        }else{
                            textArea_1.append(LoggerHelper.getDate()+ ":    FILE NOT MOVED TO ERROR FOLDER: " + csvFilename+".\n");      
                        }   
                        
                        textArea_1.append(LoggerHelper.getDate()+ ":    IMPORT CSV : ERROR OCCUR\n\n");          
                        
                        //move to process folder in FTP
                        ftpCsvFiles.put(csvFilename, new fileMoveDetails(filename,"error"));
                    }
					
				}
			} catch (Exception ex) {
				csvFilename = String.format("%s%s", _errorDir, LoggerHelper.getDate()+ "_"+filename);
				try {
					System.out.println("#   ERROR:" + ex.toString());
                    
                    //MOVE FILES TO ERROR DIRECTORY
					if(flh.moveTo2(csvFilename)){                        
                        textArea_1.append(LoggerHelper.getDate()+ ":    FILE MOVED TO ERROR FOLDER: " + csvFilename+".\n");
                        textArea_1.append(LoggerHelper.getDate()+ ":    IMPORT CSV : ERROR OCCUR\n\n");
                    }else{                       
                        textArea_1.append(LoggerHelper.getDate()+ ":    FILE NOT MOVED TO ERROR FOLDER: " + csvFilename+".\n");
                        textArea_1.append(LoggerHelper.getDate()+ ":    IMPORT CSV : ERROR OCCUR\n\n");
                    }
					fileOutputHelper eLogs = LoggerHelper.getAppLog().createFileOutput(errorLogFilename + "_" + filename + ".log");
					eLogs.writeLine("#[START]=================================================#"); 
					eLogs.writeLine(LoggerHelper.getDate());
					eLogs.writeLine(ex.toString()); 
					for(StackTraceElement ste : ex.getStackTrace()){
						eLogs.writeLine(String.format("\t%6d : %s.%s\t#%s",ste.getLineNumber(), ste.getClassName(),ste.getMethodName(),ste.getClass())); 
					}
					eLogs.writeLine("#[END]===================================================#"); 
					
                    //move to process folder in FTP
                    ftpCsvFiles.put(csvFilename, new fileMoveDetails(filename,"error"));
                    
					LoggerHelper.getAppLog().writeLine( String.format( "[ERROR @%s]",LoggerHelper.getDate()));
					LoggerHelper.getAppLog().writeLine(" -filename: "+ csvFilename);
					LoggerHelper.getAppLog().writeLine(" -message: "+ ex.toString());
					
				} catch (Exception ex2) {
					ex.printStackTrace();
				}
                System.out.println(LoggerHelper.getDate()+ ":    IMPORT CSV : Has Failed\n\n");
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
                            id = username;
                            pass = password;
                        }else{
                            id = tokenId;
                            pass = tokenSecret;            
                        }
                        //result = request.createWebRequest(LoginProc, ((CimboboxItem)cmbo_mapid.getSelectedItem()).value, id,pass,data, fileName);
                        textArea_1.append(LoggerHelper.getDate()+ ":    MAP ID DETECTED: "+config.id+".\n");
                        result = request.createWebRequest(LoginProc, config.id, id,pass,data, fileName, delimiter);
                        if(!result){
                            textArea_1.append(LoggerHelper.getDate()+ ":    PROCESS ERROR: "+request.getError());
                        }
                    }else{
                        throw new invalidcsvHeaderException();
                    }
                    break;
            }   
            return result;
        }catch(Exception ex){
            textArea_1.append(LoggerHelper.getDate()+ ":    ERROR: "+ex.getMessage()+"\n");
            return false;
        }
	}
    /******************************************************************************************************************
     * FUNCTION ActionListener btnProcEvent
     * Check files then send for csv import
     *****************************************************************************************************************/
    private ActionListener btnProcEvent = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            
            BufferedReader csvFile=null;

            StringBuilder argInfo = new StringBuilder();
			
			String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
			argInfo.append("\n\n*** Start Time (Timestamp) : ");
			argInfo.append(timeStamp);
			argInfo.append("\n\n");
			System.out.println(argInfo.toString());
			
            //disable button
            btnProc.setEnabled(false);
            try {
                boolean validationResult = validateForm();
                if(validationResult){			
                    String sCurrentLine;
                    String charset = configNs.getValue(Constants.FILE_CHARSET);
                    String delimiter = configNs.getValue(Constants.NS_CSV_FILE_DELIMITER);
                    
                    if(delimiter.isEmpty()){
                    	delimiter = ",";
                    }else{
                    	if(!delimiter.equals(",") && !delimiter.equals("\\|") && !delimiter.equals(";") && !delimiter.equals("\\s") && !delimiter.equals("\t")){
                    		throw new InvalidParameterException("Invalid CSV Delimiter defined in the properties file.");
                    	}
                    }
                    
                    if(charset.isEmpty()){
                        charset = "ISO-8859-1";
                    }
                    //set queue
                    if(bsuiteCloudLAcct)
                    {   System.out.println("queue is set"); request.setQueue(Integer.parseInt(cmbo_queue.getSelectedItem().toString()));    
                    }
                    System.out.println(csvStorage);
                    if (csvStorage.equals(Constants.CSVStorage.LOCAL.toString())){                        
                        try{
                            csvFile = new BufferedReader(new InputStreamReader( new FileInputStream(csvFullFilename),charset));//StandardCharsets.ISO_8859_1));
                            textArea_1.append(LoggerHelper.getDate()+ ":    Import CSV : Starting...\n");
                            
                            StringBuilder csvSB = new StringBuilder();
                            while ((sCurrentLine = csvFile.readLine()) != null) {
                                csvSB.append(sCurrentLine + "\n");
                            }
                            
                            boolean requestResult = false;
                            if(LoginProc.equals(Constants.LoginPreference.OAUTH.toString()))
                                requestResult = request.createWebRequest(LoginProc,((CimboboxItem)cmbo_mapid.getSelectedItem()).value, tokenId, tokenSecret, csvSB.toString(), csvFileName, delimiter);
                            else
                                requestResult = request.createWebRequest(LoginProc,((CimboboxItem)cmbo_mapid.getSelectedItem()).value,username, password, csvSB.toString(), csvFileName, delimiter);
                            if(requestResult){    
                                textArea_1.append(request.getDetails());
                                request.resetDetails();
                                JOptionPane optn = new JOptionPane("PROCESS CSV File Added to Import Queue",JOptionPane.INFORMATION_MESSAGE);
                                JDialog dialog = optn.createDialog("COMPLETE");
                                dialog.setVisible(true);
                                textArea_1.append(LoggerHelper.getDate()+ ":    Import CSV : CSV File Added to Import Queue\n\n");
                            }else{                                      
                                JOptionPane optn = new JOptionPane("PROCESS Import ERROR",JOptionPane.ERROR_MESSAGE);
                                JDialog dialog = optn.createDialog("ERROR");
                                dialog.setVisible(true);
                                textArea_1.append(LoggerHelper.getDate()+ ":    ERROR: "+request.getError());                         
                            }
                        }finally{
                        	csvFile.close();
                        }
                        

                    }else if (csvStorage.equals(Constants.CSVStorage.SFTP.toString())){
                    	sftpClient = new SftpHelper();
                    	Boolean isSuccess = sftpClient.readFTP(ftpUsername, ftpPassword, ftpHost, ftpPath, ftpPort);
                    	
                    	if(isSuccess){
                            String downloadedDetails   = sftpClient.getDownloadedDetails();
                            String[] downloadedFiles   = sftpClient.getDownloadedFiles();
                            if(!downloadedDetails.equals("")){
                                ftpCsvFiles = new HashMap<String, fileMoveDetails>();
                                
                                textArea_1.append(downloadedDetails);
                                
                                //set fields
                                Date dtNow = new Date();
                                
                                _baseLogfile = configNs.getValue(Constants.FILE_BASELOGDIR);

                                _baseLogfile = String.format("%sLOGS %s.log", _baseLogfile, LoggerHelper.getDate());

                                _inboxDir = configNs.getValue(Constants.FILE_INBOX);
                                _processingDir = configNs.getValue(Constants.FILE_PROCESSED);
                                _errorDir = configNs.getValue(Constants.FILE_ERROR);
                                
                                inboxFolder = new fileOutputHelper(_inboxDir);
                                processingFolder = new fileOutputHelper(_processingDir);
                                errorFolder = new fileOutputHelper(_errorDir);
                                
                                processSFTPInbox();
                                
                                //******************MOVE FILES ON FTP LOCATION********************************************************************************                                
                                String ftpWrite = configNs.getValue(Constants.FTP_REMOVEPROCESSED_FILE);
                                if(!isEmpty(ftpWrite)){
                                    ftpWrite = ftpWrite.trim().toLowerCase();
                                    if(ftpWrite.equals("true") &&  !ftpCsvFiles.isEmpty()){
                                        textArea_1.append(LoggerHelper.getDate()+ ":    MOVING FILES ON FTP LOCATION:\n");
                                        String ftpErrorDir      = configNs.getValue(Constants.FTP_ERROR_DIR);
                                        String ftpProcessedDir  = configNs.getValue(Constants.FTP_PROCESSED_DIR);
                                        if(!isEmpty(ftpErrorDir) && !isEmpty(ftpProcessedDir)){
                                            //System.out.println(ftpErrorDir + " "+ftpProcessedDir);
                                        	ftpClient.moveFiles(ftpUsername, ftpPassword, ftpHost, ftpPath, ftpPort,ftpCsvFiles);
                                            downloadedDetails   = sftpClient.getDownloadedDetails();
                                            textArea_1.append(downloadedDetails);
                                        }else{
                                            textArea_1.append(LoggerHelper.getDate()+ ":    ERROR: SFTP ERROR and PROCESSED directory is required.");
                                        }
                                        textArea_1.append("**********************************************************************************\n");
                                    }
                                }
                                //******************MOVE FILES ON FTP LOCATION********************************************************************************
                                
                                JOptionPane optn = new JOptionPane("Processing of CSV files on SFTP Server is complete.",JOptionPane.INFORMATION_MESSAGE);
                                JDialog dialog = optn.createDialog("COMPLETE");
                                dialog.setVisible(true);                            
                            }else{
                                JOptionPane optn = new JOptionPane("No csv files detected.",JOptionPane.INFORMATION_MESSAGE);
                                JDialog dialog = optn.createDialog("COMPLETE");
                                dialog.setVisible(true);  
                            }
                        }else{
                            textArea_1.append(LoggerHelper.getDate()+ ":    ERROR: There was an error in processing your files on the ftp server.\n");
  
                        }
                    }else{
                        ftpClient = new ftpHelper();
                        textArea_1.append(LoggerHelper.getDate()+ ":    Connecting to FTP Server: "+ftpHost+"\n");
                        
                        Boolean isSuccess = ftpClient.readFTP(ftpUsername, ftpPassword, ftpHost, ftpPath, ftpPort);
                        
                        if(isSuccess){
                            String downloadedDetails   = ftpClient.getDownloadedDetails();
                            String[] downloadedFiles   = ftpClient.getDownloadedFiles();
                            if(!downloadedDetails.equals("")){
                                ftpCsvFiles = new HashMap<String, fileMoveDetails>();
                                
                                textArea_1.append(downloadedDetails);
                                
                                //set fields
                                Date dtNow = new Date();
                                
                                _baseLogfile = configNs.getValue(Constants.FILE_BASELOGDIR);

                                _baseLogfile = String.format("%s\\LOGS %s.log", _baseLogfile, LoggerHelper.getDate());

                                _inboxDir = configNs.getValue(Constants.FILE_INBOX);
                                _processingDir = configNs.getValue(Constants.FILE_PROCESSED);
                                _errorDir = configNs.getValue(Constants.FILE_ERROR);
                                
                                inboxFolder = new fileOutputHelper(_inboxDir);
                                processingFolder = new fileOutputHelper(_processingDir);
                                errorFolder = new fileOutputHelper(_errorDir);
                                processInbox();
                                
                                //******************MOVE FILES ON FTP LOCATION********************************************************************************                                
                                String ftpWrite = configNs.getValue(Constants.FTP_REMOVEPROCESSED_FILE);
                                if(!isEmpty(ftpWrite)){
                                    ftpWrite = ftpWrite.trim().toLowerCase();
                                    if(ftpWrite.equals("true") &&  !ftpCsvFiles.isEmpty()){
                                        textArea_1.append(LoggerHelper.getDate()+ ":    MOVING FILES ON FTP LOCATION:\n");
                                        String ftpErrorDir      = configNs.getValue(Constants.FTP_ERROR_DIR);
                                        String ftpProcessedDir  = configNs.getValue(Constants.FTP_PROCESSED_DIR);
                                        if(!isEmpty(ftpErrorDir) && !isEmpty(ftpProcessedDir)){
                                            //System.out.println(ftpErrorDir + " "+ftpProcessedDir);
                                            ftpClient.moveFiles(ftpUsername, ftpPassword, ftpHost, ftpPath, ftpPort,ftpCsvFiles);
                                            downloadedDetails   = ftpClient.getDownloadedDetails();
                                            textArea_1.append(downloadedDetails);
                                        }else{
                                            textArea_1.append(LoggerHelper.getDate()+ ":    ERROR: FTP ERROR and PROCESSED directory is required.");
                                        }
                                        textArea_1.append("**********************************************************************************\n");
                                    }
                                }
                                //******************MOVE FILES ON FTP LOCATION********************************************************************************
                                
                                JOptionPane optn = new JOptionPane("Processing of CSV files on FTP Server is complete.",JOptionPane.INFORMATION_MESSAGE);
                                JDialog dialog = optn.createDialog("COMPLETE");
                                dialog.setVisible(true);                            
                            }else{
                                JOptionPane optn = new JOptionPane("No csv files detected.",JOptionPane.INFORMATION_MESSAGE);
                                JDialog dialog = optn.createDialog("COMPLETE");
                                dialog.setVisible(true);  
                            }
                        }else{
                            textArea_1.append(LoggerHelper.getDate()+ ":    ERROR: There was an error in processing your files on the ftp server.\n");
  
                        }
                    }				    
                }else{
                    JOptionPane optn = new JOptionPane(validationError,JOptionPane.ERROR_MESSAGE);
                    JDialog dialog = optn.createDialog("Error");
                    dialog.setVisible(true);

                }
                btnProc.setEnabled(true);
            }catch(invalidcsvHeaderException ex){
                textArea_1.append(LoggerHelper.getDate()+ ":    ERROR: " + ex.getMessage() + "\n");
                JOptionPane optn = new JOptionPane(ex,JOptionPane.ERROR_MESSAGE);
                JDialog dialog = optn.createDialog("Error");
                dialog.setVisible(true); 
                btnProc.setEnabled(true);

            }catch (Exception  ex) {
                ex.printStackTrace();
                JOptionPane optn = new JOptionPane(ex,JOptionPane.ERROR_MESSAGE);
                JDialog dialog = optn.createDialog("Error");
                dialog.setVisible(true);
                btnProc.setEnabled(true);

            }finally {
                try {
                    if (csvFile != null)csvFile.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                btnProc.setEnabled(true);
                
                argInfo = new StringBuilder();
    			
    			timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
    			argInfo.append("*** End Time (Timestamp) : ");
    			argInfo.append(timeStamp);
    			argInfo.append("\n\n");
    			System.out.println(argInfo.toString());
            }	
        }
    };
}
