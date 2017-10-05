package com.amfam.billing.acquirer;

import java.io.IOException;
import java.util.Properties;

public class PropertyLoadTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try{
			Properties authProperties = System.getProperties();
			String env = System.getProperty("env");
			String fileName = "TSYSTestCredentials-"+env+".properties";
			System.out.println("Property file name:"+fileName);
			authProperties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("com/amfam/billing/acquirer/"+fileName));
			String mechantIDNumber = authProperties.getProperty("MerchantIDNumber"); // Appending 3 spaces to match 15 digit number
			int i = mechantIDNumber.length(); 
			StringBuilder merchantID = new StringBuilder();
			while(i < 15){
				merchantID.append( " ");
				i++;
			}
			System.out.println("MerchantId:"+merchantID.toString()+mechantIDNumber);
			String accountNumber = "1234509876123456";
			System.out.println("Length in hex"+Integer.toHexString(16));
			
		}catch(IOException ie){
			System.out.println("TSYSTestCredentials.properties file is not found");
		}
	}

	
}
