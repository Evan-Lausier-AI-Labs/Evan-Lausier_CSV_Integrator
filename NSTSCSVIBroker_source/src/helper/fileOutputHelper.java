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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Dictionary;

public class fileOutputHelper extends File {

	private String _ROOTPATH;

	public fileOutputHelper(String pathname, boolean openOnly) {
		
		super(pathname);
		
/*		if(!pathname.isEmpty()){
			pathname.replaceAll("\\", "\\\\");
			System.out.println(pathname);
		}*/
		if (!super.exists()) {
			try {
				
				boolean isFile = (super.getName().lastIndexOf(".") >= 0)? true: false ;				
				if (isFile) {
					if (!openOnly) {
						super.getParentFile().mkdirs();
						super.createNewFile();
					}
				}else{
					super.mkdirs();
				}


				if (super.isFile()) {
					_ROOTPATH = super.getParent();
					
				}else{
					_ROOTPATH = super.getPath();
				}
				
			} catch (IOException ex) {
				System.out.println(ex);
			}
		}else{
			if (super.isFile()) {
				_ROOTPATH = super.getParent();
			}else{
				_ROOTPATH = super.getPath();
			}

		}
	}

	public fileOutputHelper(String pathname) {
		this(pathname, false);
	}

	public fileOutputHelper createFileOutput(String filename){
		System.out.println(_ROOTPATH + File.separator + filename);
		fileOutputHelper floh = new fileOutputHelper(_ROOTPATH + File.separator + filename,false);
		return floh;
	}
	
	public void writeLine(String text) {
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(this, true));
			try{
				output.write(text);
				output.newLine();
			}finally{
				output.close();
			}
		} catch (IOException ex) {
			System.out.println(ex);
		}
	}

	public boolean moveTo2(String path){
        try{           
            String srcFile = this.getPath();
            if (srcFile != path) {
                Files.move(Paths.get(srcFile), Paths.get(path),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        }catch(Exception ex){
            return false;
        }
	}

}
