package com.amfam.billing.acquirer;


import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amfam.crypto.CryptoUtil;
import com.amfam.crypto.ICrypto;
import com.amfam.crypto.icsf.cics.ECICrypto;
import com.amfam.reuse.code.CodeSet;
import com.amfam.reuse.code.CodeValue;
import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.IsoType;
import com.solab.iso8583.IsoValue;
import com.solab.iso8583.MessageFactory;

/**
 * 
 * Added to Build the request message as per the TSYS request format specified in TSYS ISO spec's.
 * Added separate method's to create request messages to different types of messages / requests which 
 * are going to initialize.
 *
 */
public class SaratogaRequestBuilder {
	
	private static final Log LOG = LogFactory.getLog( SaratogaRequestBuilder.class );
	private MessageFactory<IsoMessage> messageFactory;
	private HashMap<String,String> stateCodeMap;
	private CryptoUtil cryptoUtil;
	
	private String terminalNumber;
	private String merchantIDNumber;
	private String categoryCode;
	private String bin;
	private String store;
	private String cardAcceptorName;
	private String nationalPOSGeoData;
	private String posInformation;
	private String cpsFieldBitmap;

	private static final String AUTHORIZATION_CHAR_IND = "Y";
	private static final String BLANK_SPACE = " ";
	private static String TRACE_AUDIT_NUMBER = "0";
	private static final String HEADER_FIELD_CD_TYPE = "TISO";
	
	public SaratogaRequestBuilder(){
		ICrypto crypto = new ECICrypto();
		String keyName = "FINANCIAL.ACCOUNT.DATA.RECEIPT";
		cryptoUtil = new CryptoUtil();
		cryptoUtil.setCrypto(crypto);
		cryptoUtil.setKeyName(keyName);
	}
	
	public HashMap<String, String> getStateCodeMap() {
		return stateCodeMap;
	}

	public void setStateCodeMap(HashMap<String, String> stateCodeMap) {
		this.stateCodeMap = stateCodeMap;
	}

	public MessageFactory<IsoMessage> getMessageFactory() {
		return messageFactory;
	}

	public  void setMessageFactory(MessageFactory<IsoMessage> messageFactory) {
		this.messageFactory = messageFactory;
	}
	
	public String getTerminalNumber() {
		return terminalNumber;
	}
	
	public void setTerminalNumber(String terminalNumber) {
		this.terminalNumber = terminalNumber;
	}
	
	public String getMerchantIDNumber() {
		return merchantIDNumber;
	}
	
	public void setMerchantIDNumber(String merchantIDNumber) {
		this.merchantIDNumber = merchantIDNumber;
	}
	
	public String getCategoryCode() {
		return categoryCode;
	}
	
	public void setCategoryCode(String categoryCode) {
		this.categoryCode = categoryCode;
	}
	
	public String getBin() {
		return bin;
	}
	
	public void setBin(String bin) {
		this.bin = bin;
	}
	
	public String getStore() {
		return store;
	}
	
	public void setStore(String store) {
		this.store = store;
	}

	public String getCardAcceptorName() {
		return cardAcceptorName;
	}
	
	public void setCardAcceptorName(String cardAcceptorName) {
		this.cardAcceptorName = cardAcceptorName;
	}
	
	public String getNationalPOSGeoData() {
		return nationalPOSGeoData;
	}
	
	public void setNationalPOSGeoData(String nationalPOSGeoData) {
		this.nationalPOSGeoData = nationalPOSGeoData;
	}
	
	public String getPosInformation() {
		return posInformation;
	}
	
	public void setPosInformation(String posInformation) {
		this.posInformation = posInformation;
	}
	
	public String getCpsFieldBitmap() {
		return cpsFieldBitmap;
	}
	
	public void setCpsFieldBitmap(String cpsFieldBitmap) {
		this.cpsFieldBitmap = cpsFieldBitmap;
	}
	
	/**
	 * Create Message Payload / Request message for Sign-On Message
	 * 
	 * @return String
	 */
	public String getMessagePayloadForSignOn() {
		int headerType = 0x800;
		
		MessageFactory<IsoMessage> messageFactory = getMessageFactory();
		messageFactory.setUseBinaryBitmap(true);

		IsoMessage isoMessage = messageFactory.getMessageTemplate(0x800);
		
		// Build dynamic data fields here
		isoMessage.setBinaryBitmap(true);
		isoMessage.setBinary(true);

		IsoValue<Date> trandate = new IsoValue<Date>(IsoType.DATE10, new Date());
		isoMessage.setField(7, trandate);		
		
		setRetrievalReferenceNumber(isoMessage);// Field 37

		isoMessage.setValue(70, 1, IsoType.NUMERIC, 4);
		
		setISOHeader(messageFactory, headerType, getTotalMessageLength(isoMessage));		

		String first4Bytes = getFirstFourBytes(isoMessage);
		String headerPlusMessage = messageFactory.getIsoHeader(headerType)+ isoMessage.debugString();
		String messagePayload = first4Bytes + headerPlusMessage;	
		
		return messagePayload;
	}
	/**
	 * create message payload to Keep-Alive message.
	 * 
	 * @return String
	 */
	public String getMessagePayloadForKeepAlive() {
		int headerType = 0x800;		
		MessageFactory<IsoMessage> messageFactory = getMessageFactory();
		messageFactory.setUseBinaryBitmap(true);

		IsoMessage isoMessage = messageFactory.getMessageTemplate(0x800);
		
		// Build dynamic data fields here
		isoMessage.setBinaryBitmap(true);
		isoMessage.setBinary(true);

		IsoValue<Date> trandate = new IsoValue<Date>(IsoType.DATE10, new Date());
		isoMessage.setField(7, trandate);		
		
		setRetrievalReferenceNumber(isoMessage);// Field 37

		isoMessage.setValue(70, 301, IsoType.NUMERIC, 4);
		
		setISOHeader(messageFactory, headerType, getTotalMessageLength(isoMessage));
		
		String first4Bytes = getFirstFourBytes(isoMessage);
		String headerPlusMessage = messageFactory.getIsoHeader(headerType)+ isoMessage.debugString();
		String messagePayload = first4Bytes + headerPlusMessage;	
		
		return messagePayload;
	}
	
	/**
	 * Creates Message Payload to Authorization Message
	 * 
	 * @param authRequest / mockPayment
	 * @return String
	 */
	public  String getMessagePayloadForAUTH(SaratogaAuthRequest mockPayment) {
		int messageHeaderType = 0x100;
		MessageFactory<IsoMessage> messageFactory = getMessageFactory();
		messageFactory.setUseBinaryBitmap(true);

		IsoMessage isoMessage = messageFactory.getMessageTemplate(messageHeaderType);
		
		// Build dynamic data fields here
		isoMessage.setBinaryBitmap(true);
		isoMessage.setBinary(true);

		buildAuthMessage(isoMessage, mockPayment);
		
		setISOHeader(messageFactory, messageHeaderType, getTotalMessageLength(isoMessage));

		String first4Bytes = getFirstFourBytes(isoMessage);
		String headerPlusMessage = messageFactory.getIsoHeader(messageHeaderType)+ isoMessage.debugString();
		String messagePayload = first4Bytes + headerPlusMessage;		
		
		return messagePayload;
	}

	
	private  void buildAuthMessage(IsoMessage isoMessage, SaratogaAuthRequest authRequest) {
		
		Map<String, String> cardDetails = getCardNumberExpDate(authRequest);
		String cardNumber = cardDetails.get("cardNumber");
		String cardNumberLength = Integer.toHexString(cardNumber.length());
		
		//zero pad the cardnumber if less than 16.
		if(cardNumber.length()<16){
			cardNumber = StringUtils.leftPad(cardNumber, 16, "0");
		}
		isoMessage.setValue(2, cardNumberLength+cardNumber, IsoType.NUMERIC, 18);
		
		isoMessage.setValue(4, authRequest.getAmount(), IsoType.AMOUNT, 0);
		
		IsoValue<Date> trandate = new IsoValue<Date>(IsoType.DATE10, new Date());
		isoMessage.setField(7, trandate);

		isoMessage.setValue(11, authRequest.getStanNumber(), IsoType.NUMERIC, 6);
		isoMessage.setValue(12, new Date(), IsoType.TIME, 0);
		isoMessage.setValue(13, new Date(), IsoType.DATE4, 0);
		
		isoMessage.setValue(14, cardDetails.get("expDate"), IsoType.NUMERIC, 4);
		isoMessage.setValue(18, getCategoryCode(), IsoType.NUMERIC, 4);
		
		isoMessage.setValue(32, getBin(), IsoType.LLVAR, 6);
		
		String hexEbcdicValOfRetrievalRefNumber = asciitoHexEbcdic(authRequest.getCorrelationId());
		IsoValue<String> refNumber = new IsoValue<String>(IsoType.NUMERIC, hexEbcdicValOfRetrievalRefNumber, 24);
		isoMessage.setField(37, refNumber);
		
		String cardAcceptorTerminalId = getStore() + getTerminalNumber();
		
		isoMessage.setValue(41, asciitoHexEbcdic(cardAcceptorTerminalId), IsoType.NUMERIC, 16);
		
		String hexEbcdicValOfMechantIDNumber = asciitoHexEbcdic(getMerchantIDNumber() + getStore().substring(0,3));
		IsoValue<String> isoValueOfMechantIDNumber = new IsoValue<String>(IsoType.NUMERIC, hexEbcdicValOfMechantIDNumber, 30);
		isoMessage.setField(42, isoValueOfMechantIDNumber);
		
		String cardAcceptorName = getCardAcceptorName();
		isoMessage.setValue(43, asciitoHexEbcdic(cardAcceptorName), IsoType.NUMERIC, 80);
		
		
		String NationalPOSGeoData =getNationalPOSGeoData();
		String posInformationLength = Integer.toHexString(NationalPOSGeoData.length());
		isoMessage.setValue(59, posInformationLength+asciitoHexEbcdic(NationalPOSGeoData), IsoType.NUMERIC, 22); // length should be bytes long
		isoMessage.setValue(60, getPosInformation(), IsoType.NUMERIC, 14);//length should be bytes length
		isoMessage.setValue(62, getCpsFieldBitmap() + asciitoHexEbcdic(AUTHORIZATION_CHAR_IND), IsoType.NUMERIC, 20);
		
		String zipCode = authRequest.getZip();
		if(zipCode==null){
			zipCode=BLANK_SPACE;
		}
		zipCode = StringUtils.rightPad(zipCode, 9, BLANK_SPACE);
		String zipCodeLength = Integer.toHexString(zipCode.length());
		isoMessage.setValue(123, zipCodeLength+asciitoHexEbcdic(zipCode), IsoType.NUMERIC, 20); // length should be bytes long
		
	}


	private  String getFirstFourBytes(IsoMessage isoMessage) {
		String hexString = getTotalMessageLength(isoMessage);		
		String first4Bytes = "00" + hexString + "0000";
		LOG.trace("Length of the message with header:" + hexString);
		return first4Bytes;
	}

	private  String getTotalMessageLength(IsoMessage isoMessage) {
		int length= isoMessage.debugString().length()/2;
		length=length+22;
		String hexString = Integer.toHexString(length);
		return hexString;
	}

	/**
	 * Create the Logic to generate unique retrieval reference number to each request we send in Sign-on and keep-alive messages
	 * 
	 * @param isoMessage
	 */
	private  void setRetrievalReferenceNumber(IsoMessage isoMessage) {
		
		Calendar calendar = Calendar.getInstance();
		final int julianDay = calendar.get(Calendar.DAY_OF_YEAR);
		final int year = calendar.get(Calendar.YEAR);
		final int hour = calendar.get(Calendar.HOUR_OF_DAY);
		
		String year1 = new Integer(year).toString();
		String truncatedyear = year1.substring(year1.length() - 1);

		int min=0 , max= 700;
        int randomNum = new Random().nextInt((max - min) + 1) + min;
        TRACE_AUDIT_NUMBER = "" + randomNum;
        TRACE_AUDIT_NUMBER = StringUtils.leftPad(TRACE_AUDIT_NUMBER, 6, "0");
        isoMessage.setValue(11, TRACE_AUDIT_NUMBER, IsoType.NUMERIC, 6);
        String retrievalRefNumber = truncatedyear+""+ julianDay +""+ hour + TRACE_AUDIT_NUMBER;
        String hexEbcdicValOfRetrievalRefNumber = asciitoHexEbcdic(retrievalRefNumber);
        IsoValue<String> refNumber = new IsoValue<String>(IsoType.NUMERIC, hexEbcdicValOfRetrievalRefNumber, 24);
        isoMessage.setField(37, refNumber);
       


	}

	private  void setISOHeader(MessageFactory<IsoMessage> messageFactory,
			int headerType, String messagelength) {
		Map<Integer, String> messageHeader = new TreeMap<Integer, String>();
		
		CodeValue[] cdValues = CodeSet.getStorage().find(HEADER_FIELD_CD_TYPE).getValues();
		StringBuffer headerFields1thru4 = new StringBuffer();
		StringBuffer headerFields5thru12 = new StringBuffer();
		
		for (int i = 0; i < cdValues.length; i++) {
			CodeValue cdVal = cdValues[i];
			if (i < 4){
				headerFields1thru4.append(cdVal.getName());
			}else{
				headerFields5thru12.append(cdVal.getName());
			}
		}
		
		messageHeader.put(headerType, headerFields1thru4.toString().trim() + messagelength + headerFields5thru12.toString().trim());
		messageFactory.setIsoHeaders(messageHeader);
	}

	private  String asciitoHexEbcdic(String ascii) {
		StringBuffer hexEbcdic = new StringBuffer();
		try {
			byte[] ebcdic = ascii.getBytes("IBM500");
			for (byte b: ebcdic){
				String str = Integer.toHexString((char)b);
				if(str.length()>2){
					str = str.substring(str.length()-2,str.length());
				}
				hexEbcdic.append(str);
			}
		} catch (UnsupportedEncodingException e) {
			LOG.error("SaratogaRequestBuilder ASCII to HexEbcdic convertion failed", e);
			throw new RuntimeException(e);
		}
		return hexEbcdic.toString();

	}

	/**
	 * Decrypt the request message to get the Credit Card number and expiration date from request message.
	 * 
	 * @param authRequest
	 * @return
	 */
	public Map<String, String> getCardNumberExpDate(SaratogaAuthRequest authRequest){
		String decryptedCardNumber = null;
		String decryptedExpDate = null;
		try {
			decryptedCardNumber = cryptoUtil.decrypt(authRequest.getCreditCardNumber().getEncoded(), null);
			if (LOG.isDebugEnabled()) {
				//Log the first two characters to see what the MOP should be.  This is not a security
				//risk since the first 6 char are actually an "issuer number" and not protected.
				//We were having a problem with sending the wrong MOP or Paymentech is responding
				//with the wrong MOP.
				if (decryptedCardNumber.length() >= 2) {
					LOG.debug("First two characters of card number " + decryptedCardNumber.substring(0,2));
				}
			}
			decryptedExpDate = cryptoUtil.decrypt(authRequest.getExpirationDate().getEncoded(), null);
		} catch (ClassNotFoundException e) {
			//can only happen if using formatter.  Since we're not using a formatter, ignore.
			throw new UndeclaredThrowableException(e);
		} catch (NoSuchMethodException e) {
			//can only happen if using formatter.  Since we're not using a formatter, ignore.
			throw new UndeclaredThrowableException(e);
		} catch (InstantiationException e) {
			//can only happen if using formatter.  Since we're not using a formatter, ignore.
			throw new UndeclaredThrowableException(e);
		} catch (IllegalAccessException e) {
			//can only happen if using formatter.  Since we're not using a formatter, ignore.
			throw new UndeclaredThrowableException(e);
		} catch (InvocationTargetException e) {
			//can only happen if using formatter.  Since we're not using a formatter, ignore.
			throw new UndeclaredThrowableException(e);
		}
		
		String mm = "  ";
		String yy = "  ";
		if (decryptedExpDate != null && decryptedExpDate.length() == 10) {
			mm = decryptedExpDate.substring(5,7);
			yy = decryptedExpDate.substring(2,4);
		}
		Map<String, String> cardDetails = new HashMap<String, String>();
		cardDetails.put("cardNumber", decryptedCardNumber);
		cardDetails.put("expDate", yy+mm);
		return cardDetails;
	}
}
