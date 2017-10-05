package com.amfam.billing.acquirer;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amfam.billing.acquirer.util.CleanseCreditCardNumber;
import com.solab.iso8583.util.HexCodec;

/**
 * 
 * 
 * Parser to parser the request and Response Messages to identify the IP Header, Request Header, 
 * Message type , Message Length, Header Length, Actual Message / Data fields .
 * Also it converts request and response messages to different formats like ASCII, EBCDIC, HEXDECIMAL, BINARY
 * where ever it is required.
 */
public class SaratogaMessageParser {
	private static final Log LOG = LogFactory.getLog( SaratogaMessageParser.class );
	private static final int IP_HEADER_BEGIN_INDEX = 0;
	private static final int IP_HEADER_END_INDEX = 8;
	private static final int HEADER_LENGTH_BEGIN_INDEX = 8;
	private static final int HEADER_LENGTH_END_INDEX = 10;
	private static final int HEADER_VALUE_BEGIN_INDEX = 8;
	private static final int MESSAGE_TYPE_BEGIN_INDEX = 0;
	private static final int MESSAGE_TYPE_END_INDEX = 4;
	
	private HashMap<Integer,ISODataField> authResponseDataFieldMap;
	
//	String rMessage = "008b0000160102008b109900640313061000428c1010060000000110722220810ed080041044444444444444480000000000000153690914123420355631091508400806478930f7f2f5f7f1f2f3f5f5f6f3f1f0f8f3f1f5c2f0f0f0f0f0f1f1f0f0f3f0f0f0f0f0f7f0f0f3f4f9f5f0f0f002f5c3084017e000020000000000e50587257632608342f4e5e3f8c240";
//	String rMessage ="008b0000160102008b109900640313031000428c1011030000000110722220810ed080041051110051110511280000000000000139110915132539174365091608400806478930f7f2f5f8f1f3f1f7f4f3f6f5f0f2f6f3f0f1f0f0f0f0f0f1f1f0f0f3f0f0f0f0f0f7f0f0f3f4f9f5f0f0f002f5c3084017e000020000000000e50467258663396416f3c2c2c3c740";
	//String rMessage ="008b0000160102008b10990064031306100042881011060000000110722220810ed0800410XXXXXXXXXXXXXXXX0020000000000423890915132650364676091608400806478930f7f2f5f8f1f3f3f6f4f6f7f6f1f0f8f0f7f9f0f0f0f0f0f1f1f0f0f3f0f0f0f0f0f7f0f0f3f4f9f5f0f0f002f5c3084017e000020000000000e50587258664105002d4e2f2c4c640";
//	String rMessage ="16010200b9666666000000000000000000000000005a0110f22220810ed1810400000000010000001055067500000044220000000000000034110617211757116000121208400806500000f2f2f6f3f2f1f1f1f6f0f0f0e5e3d3d4c3f1f0f0f0f0f0f1f0f0f0f1f9f9f9f9f9f9f9f9f9f9f1f1f0f0f00bf5e84040404040404040d4045af0f1f008400f01000c010af1f2f3f4c1c2c3c4c5c6188000800000000000e8f1f2f1f2d4c3c3f1f1f1f0f0f24040065600030101c6";
//	String rMessage ="008b0000160102008b1099006403130610004288100b060000000110722220810ed0800410XXXXXXXXXXXXXXXX0000000000000348360918102225375907091908400806478930f7f2f6f1f1f0f3f7f5f9f0f7f0f3f1f2f2f3f0f0f0f0f0f1f1f0f0f3f0f0f0f0f0f7f0f0f3f4f9f5f0f0f002f5c3084017e000020000000000e50587261553456953c4d7f5f9c640";
	String rMessage ="009f0000160102009f10990064031300000000000000000000000110767a28810ed0a00410XXXXXXXXXXXXXXXX00000000000002934400000002934409200907456100000038998309074509200920084008400806478930f7f2f6f3f0f9f3f8f9f9f8f3f6f7f6f1f9f2f0f0f0f0f0f1f1f0f0f3f0f0f0f0f0f7f0f0f3f4f9f5f0f0f002f5e408400840188000800000000000e8f0f9f2f0d4c4d1d1c4f7e9c5c64040";
//	String rMessage56 ="009f0000160102009f10990064031300000000000000000000000110767a28810ed0a00410XXXXXXXXXXXXXXXX00000000000002934400000002934409200907456100000038998309074509200920084008400806478930f7f2f6f3f0f9f3f8f9f9f8f3f6f7f6f1f9f2f0f0f0f0f0f1f1f0f0f3f0f0f0f0f0f7f0f0f3f4f9f5f0f0f002f5e4084008400f01000c010af1f2f3f4c1c2c3c4c5c6188000800000000000e8f0f9f2f0d4c4d1d1c4f7e9c5c64040";

	/**
	 * Added to parse the request and response fields in the message
	 * @param message
	 * @return
	 */
	public SaratogaAuthResponse parse(String message) {
		RawResponseMessage rawMessage = getResponseInASCII(message);
		String messageBody = rawMessage.getMessageBody();
		HashMap<Integer,Object> datafields = new HashMap<Integer,Object>();
		SaratogaAuthResponse response = new SaratogaAuthResponse();
		
		response.setRejectCode(rawMessage.getRejectCode());
		response.setRawResponse(message);
		String bitmap1;
		String binaryBitMap;

		StringBuffer remainingMessage = new StringBuffer(messageBody);
		bitmap1 = getSubString(remainingMessage,0, 16);
		binaryBitMap = getBinaryBitMap(bitmap1);
		if("1".equals(""+binaryBitMap.charAt(0))){
			/*
			 * second 16 chars if
			 */
			String bitmap2 = getSubString(remainingMessage,0, 16);
			binaryBitMap+= getBinaryBitMap(bitmap2);
		}
		
		StringBuffer cleansedMessageBody = new StringBuffer();
		response.setBinaryBitmap(binaryBitMap);
		LOG.trace("BitMap #1:"+binaryBitMap);
		char[] bitMapArray = binaryBitMap.toCharArray();
		
		for(int i=1; i<=bitMapArray.length; i++){
			if(i>1 && "1".equals(""+bitMapArray[i-1])){
				Object dataValue = getDataFieldValue(remainingMessage, i);
				if(getAuthResponseDataFieldMap().get(new Integer(i)).equals("BITMAP")){
					/*
					 * Do nothing
					 */
				}
				else{
					datafields.put(new Integer(i), dataValue);
				}
				//replace the card number and expiry date with XXX 
				printSanitizeDataFields(i, dataValue);
				cleansedMessageBody.append(dataValue);
			}
		}
		/*
		 *  cc number is in datafield #2
		 */
		LOG.trace("cleansed Message Body #: "+CleanseCreditCardNumber.replaceCcNumber(cleansedMessageBody.toString(), (String)datafields.get(new Integer(2))));

		response.setResponseText(CleanseCreditCardNumber.replaceCcNumber(cleansedMessageBody.toString(), (String)datafields.get(new Integer(2))));
		response.setDatafields(datafields);
		response.setRequestType(RequestType.find(datafields, rawMessage.getMessageType()));
		
		return response;
	}

	/**
	 * @param i
	 * @param dataValue
	 * 
	 * Santize's / Clean the Credit card number and expiration date in the LOG message's printing.
	 */
	private void printSanitizeDataFields(int i, Object dataValue) {
		String santizedFieldValue = null;
		if(i==2){
			String ccNumber = dataValue.toString();
			 santizedFieldValue = StringUtils.overlay(ccNumber, StringUtils.repeat("X", ccNumber.length()-4), 0, ccNumber.length()-4);
		}else if(i==14){
			String expDate = dataValue.toString();
			santizedFieldValue = StringUtils.overlay(expDate, StringUtils.repeat("X", expDate.length()), 0, expDate.length());
		}
		if(santizedFieldValue!=null){
			LOG.trace("Field #"+i +":"+santizedFieldValue);
		}else{
			LOG.trace("Field #"+i +":"+dataValue);
		}
	}		
	/**
	 * Defines each field need position, length and ISODataType as per the TSYS spec's
	 * @return
	 */
	public  HashMap<Integer, ISODataField> getAuthResponseDataFieldMap() {		
		if(authResponseDataFieldMap==null){
			authResponseDataFieldMap = new HashMap<Integer, ISODataField>();
			authResponseDataFieldMap.put(new Integer(2),  new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.NONE, ISODataLengthType.NUMERIC_LENGTH));
			authResponseDataFieldMap.put(new Integer(3), new ISODataField(ISODataType.NUMERIC, 6, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(4), new ISODataField(ISODataType.AMOUNT, 12, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(5), new ISODataField(ISODataType.AMOUNT, 12, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(6), new ISODataField(ISODataType.AMOUNT, 12, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(7),  new ISODataField(ISODataType.DATE10, 10, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			
			authResponseDataFieldMap.put(new Integer(9), new ISODataField(ISODataType.NUMERIC, 8, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(10), new ISODataField(ISODataType.NUMERIC, 8, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(11), new ISODataField(ISODataType.NUMERIC, 6, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(12), new ISODataField(ISODataType.NUMERIC, 6, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(13),  new ISODataField(ISODataType.DATE4, 4, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(14), new ISODataField(ISODataType.NUMERIC, 4, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(15),  new ISODataField(ISODataType.DATE4, 4, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(16),  new ISODataField(ISODataType.DATE4, 4, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));			
			authResponseDataFieldMap.put(new Integer(18), new ISODataField(ISODataType.NUMERIC, 4, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));			
			authResponseDataFieldMap.put(new Integer(19), new ISODataField(ISODataType.NUMERIC, 4, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(20), new ISODataField(ISODataType.NUMERIC, 4, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(21), new ISODataField(ISODataType.NUMERIC, 4, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(22), new ISODataField(ISODataType.NUMERIC, 4, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(23), new ISODataField(ISODataType.NUMERIC, 4, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(25), new ISODataField(ISODataType.NUMERIC, 2, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(26), new ISODataField(ISODataType.NUMERIC, 2, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(28), new ISODataField(ISODataType.ALPHA, 18, DataTranslationType.EBCIDIC, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(32), new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.NONE, ISODataLengthType.NUMERIC_LENGTH));
			authResponseDataFieldMap.put(new Integer(33),  new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.NONE, ISODataLengthType.NUMERIC_LENGTH));
			authResponseDataFieldMap.put(new Integer(34), new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.NONE, ISODataLengthType.NUMERIC_LENGTH));
			authResponseDataFieldMap.put(new Integer(35), new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.NONE, ISODataLengthType.NUMERIC_LENGTH));
			authResponseDataFieldMap.put(new Integer(37), new ISODataField(ISODataType.NUMERIC, 24, DataTranslationType.EBCIDIC, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(38), new ISODataField(ISODataType.ALPHA, 12, DataTranslationType.EBCIDIC, ISODataLengthType.FIXED_LENGTH));			
			authResponseDataFieldMap.put(new Integer(39), new ISODataField(ISODataType.NUMERIC, 4, DataTranslationType.EBCIDIC, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(41), new ISODataField(ISODataType.ALPHA,16, DataTranslationType.EBCIDIC, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(42), new ISODataField(ISODataType.ALPHA, 30, DataTranslationType.EBCIDIC, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(43), new ISODataField(ISODataType.ALPHA, 80, DataTranslationType.EBCIDIC, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(44), new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.EBCIDIC, ISODataLengthType.BYTES_LENGTH));
			authResponseDataFieldMap.put(new Integer(4402), new ISODataField(ISODataType.ALPHA, 1, DataTranslationType.EBCIDIC, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(4413), new ISODataField(ISODataType.ALPHA, 1, DataTranslationType.EBCIDIC, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(4414), new ISODataField(ISODataType.ALPHA, 1, DataTranslationType.EBCIDIC, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(45), new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.EBCIDIC, ISODataLengthType.BYTES_LENGTH));
			//authResponseDataFieldMap.put(new Integer(47), new DataFieldType("LLLVAR", 0, DataTranslationType.EBCIDIC, ISODataLengthType.BYTES_LENGTH));
			authResponseDataFieldMap.put(new Integer(48), new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.EBCIDIC, ISODataLengthType.BYTES_LENGTH));
			authResponseDataFieldMap.put(new Integer(49), new ISODataField(ISODataType.NUMERIC, 4, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(50), new ISODataField(ISODataType.NUMERIC, 4, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(51), new ISODataField(ISODataType.NUMERIC, 4, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			//authResponseDataFieldMap.put(new Integer(52), new DataFieldType(ISODataType.NUMERIC, 16, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			
			authResponseDataFieldMap.put(new Integer(54), new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.EBCIDIC, ISODataLengthType.BYTES_LENGTH));
			//authResponseDataFieldMap.put(new Integer(56), new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.EBCIDIC, ISODataLengthType.BYTES_LENGTH));
			authResponseDataFieldMap.put(new Integer(59), new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.EBCIDIC, ISODataLengthType.BYTES_LENGTH));
			authResponseDataFieldMap.put(new Integer(60), new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.NONE, ISODataLengthType.BYTES_LENGTH));
			
			authResponseDataFieldMap.put(new Integer(62), new ISODataField(ISODataType.BITMAP, 0, DataTranslationType.NONE, ISODataLengthType.BITMAP_LENGTH));
			authResponseDataFieldMap.put(new Integer(6201), new ISODataField(ISODataType.ALPHA, 2, DataTranslationType.EBCIDIC, ISODataLengthType.FIXED_LENGTH));//subfield 1 of 62nd data field
			authResponseDataFieldMap.put(new Integer(6202), new ISODataField(ISODataType.NUMERIC, 16, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));//subfield 2 of 62nd data field
			authResponseDataFieldMap.put(new Integer(6203), new ISODataField(ISODataType.ALPHA, 8, DataTranslationType.EBCIDIC, ISODataLengthType.FIXED_LENGTH));//subfield 3 of 62nd data field
			authResponseDataFieldMap.put(new Integer(6217), new ISODataField(ISODataType.ALPHA, 30, DataTranslationType.EBCIDIC, ISODataLengthType.FIXED_LENGTH));//subfield 17 of 62nd data field
			authResponseDataFieldMap.put(new Integer(6223), new ISODataField(ISODataType.NUMERIC, 4, DataTranslationType.EBCIDIC, ISODataLengthType.FIXED_LENGTH));//subfield 23 of 62nd data field
			authResponseDataFieldMap.put(new Integer(6225), new ISODataField(ISODataType.ALPHA, 2, DataTranslationType.EBCIDIC, ISODataLengthType.FIXED_LENGTH));//subfield 25 of 62nd data field
			
			authResponseDataFieldMap.put(new Integer(70), new ISODataField(ISODataType.NUMERIC, 4, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(82), new ISODataField(ISODataType.ALPHA, 24, DataTranslationType.EBCIDIC, ISODataLengthType.FIXED_LENGTH));//subfield 23 of 62nd data field
			authResponseDataFieldMap.put(new Integer(83), new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.EBCIDIC, ISODataLengthType.BYTES_LENGTH));
			authResponseDataFieldMap.put(new Integer(84), new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.EBCIDIC, ISODataLengthType.BYTES_LENGTH));
			authResponseDataFieldMap.put(new Integer(90), new ISODataField(ISODataType.NUMERIC, 42, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(93), new ISODataField(ISODataType.ALPHA, 10, DataTranslationType.EBCIDIC, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(95), new ISODataField(ISODataType.ALPHA, 84, DataTranslationType.EBCIDIC, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(100), new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.NONE, ISODataLengthType.BYTES_LENGTH));
			authResponseDataFieldMap.put(new Integer(102), new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.EBCIDIC, ISODataLengthType.BYTES_LENGTH));
			authResponseDataFieldMap.put(new Integer(103), new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.EBCIDIC, ISODataLengthType.BYTES_LENGTH));
			authResponseDataFieldMap.put(new Integer(104), new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.NONE, ISODataLengthType.BYTES_LENGTH));
			authResponseDataFieldMap.put(new Integer(115), new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.EBCIDIC, ISODataLengthType.BYTES_LENGTH));
			//authResponseDataFieldMap.put(new Integer(116), new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.NONE, ISODataLengthType.NUMERIC_LENGTH)); //Need to confirm if the length is in bytes or hexa digits
			authResponseDataFieldMap.put(new Integer(121), new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.EBCIDIC, ISODataLengthType.BYTES_LENGTH));
			authResponseDataFieldMap.put(new Integer(123), new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.EBCIDIC, ISODataLengthType.BYTES_LENGTH));
			authResponseDataFieldMap.put(new Integer(125), new ISODataField(ISODataType.LLVAR, 0, DataTranslationType.EBCIDIC, ISODataLengthType.BYTES_LENGTH));
			authResponseDataFieldMap.put(new Integer(126), new ISODataField(ISODataType.BITMAP, 0, DataTranslationType.NONE, ISODataLengthType.BITMAP_LENGTH));
			
			authResponseDataFieldMap.put(new Integer(53), new ISODataField(ISODataType.NUMERIC, 16, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(55), new ISODataField(ISODataType.LLVAR, 256, DataTranslationType.NONE, ISODataLengthType.BYTES_LENGTH));
			authResponseDataFieldMap.put(new Integer(61), new ISODataField(ISODataType.LLVAR, 19, DataTranslationType.NONE, ISODataLengthType.BYTES_LENGTH));
			authResponseDataFieldMap.put(new Integer(63), new ISODataField(ISODataType.LLVAR, 256, DataTranslationType.NONE, ISODataLengthType.BYTES_LENGTH));
			authResponseDataFieldMap.put(new Integer(73), new ISODataField(ISODataType.NUMERIC, 6, DataTranslationType.NONE, ISODataLengthType.FIXED_LENGTH));
			authResponseDataFieldMap.put(new Integer(81), new ISODataField(ISODataType.LLVAR, 256, DataTranslationType.NONE, ISODataLengthType.BYTES_LENGTH));
		}
		return authResponseDataFieldMap;
	}

	/**
	 * Get the each field 
	 * @param remainingMessage
	 * @param fieldNumber
	 * @return
	 */
	private Object getDataFieldValue(StringBuffer remainingMessage, int fieldNumber) {

		int fieldLength = 0;
		if(fieldNumber == 42){
			System.out.println();
		}
		ISODataField dataField = getAuthResponseDataFieldMap().get(new Integer(fieldNumber));
		if(dataField == null){
			throw new UnexpectedDataFieldException("Unexpected data field appeared in TSYS AUTH response at field #:"+fieldNumber);
		}
		if(ISODataLengthType.FIXED_LENGTH.equals(dataField.getDataLengthType())){
			fieldLength = dataField.getFieldLength();
		}
		else{
			if(ISODataType.LLVAR.equals(dataField.getDataType())){
				if(ISODataLengthType.BYTES_LENGTH.equals(dataField.getDataLengthType())){
					fieldLength = getMessageLengthInDecimal(getSubString(remainingMessage, 0, 2));
					fieldLength= (fieldLength*2);
				}
				else{
					fieldLength = getMessageLengthInDecimal(getSubString(remainingMessage, 0, 2));
				}
			}
			else if(ISODataType.LLLVAR.equals(dataField.getDataType())){
				fieldLength = getMessageLengthInDecimal(getSubString(remainingMessage, 0, 3));
			}
			else if(ISODataType.BITMAP.equals(dataField.getDataType())){
				fieldLength = getMessageLengthInDecimal(getSubString(remainingMessage, 0, 2));
				fieldLength= (fieldLength*2);
			}
		}
		
		String fieldValue;
		// for AMEX
		if(fieldNumber == 2 && fieldLength == 15 ){
			fieldValue = getSubStringForAmex(remainingMessage, 0, fieldLength);
		}else{
			fieldValue = getSubString(remainingMessage, 0, fieldLength);
		}
		
		if(DataTranslationType.EBCIDIC.equals(dataField.getTranslationType())){			 
			fieldValue = convertFromEBCDIC(fieldValue);
		}
		if(ISODataType.BITMAP.equals(dataField.getDataType())){
			HashMap<Integer,Object> datafields = new HashMap<Integer,Object>();
			String bitmap = fieldValue.substring(0, 16);
			StringBuffer remainingSubField = new StringBuffer(fieldValue.substring(16));
			String binaryBitMap = getBinaryBitMap(bitmap);
			char[] bitMapArray = binaryBitMap.toCharArray();
			for(int i=1; i<=bitMapArray.length; i++){
				if("1".equals(""+bitMapArray[i-1])){
					
					String dataValue = (String) getDataFieldValue(remainingSubField, (fieldNumber*100)+i);
					datafields.put(new Integer(i), dataValue);
				}
			}
			return datafields;
		}

		return fieldValue;
	}


	private String getSubStringForAmex(StringBuffer remainingMessage, int beginIndex, int endIndex) {
		String stringMsg =  remainingMessage.toString();		
		String returnString = stringMsg.substring(beginIndex, endIndex+1);
		remainingMessage.replace(0, stringMsg.length()+1,stringMsg.substring(endIndex+1));
		return returnString;
	}

	public String convertFromEBCDIC(String fieldValue) {
		byte[] bytes = HexCodec.hexDecode(fieldValue);
		try {
			fieldValue = new String(bytes, "Cp500");
		} catch (UnsupportedEncodingException e) {
			LOG.error("CovertFromEBCDIC failed:", e);
			throw new RuntimeException(e);
		}
		return fieldValue;
	}

	private static int getMessageLengthInDecimal(String hexstr) {		
		int temp1 = Integer.parseInt(hexstr, 16 );        
        return temp1;
	}

	private String getSubString(StringBuffer remainingMessage, int beginIndex, int endIndex) {
		String stringMsg =  remainingMessage.toString();		
		String returnString = stringMsg.substring(beginIndex, endIndex);
		remainingMessage.replace(0, stringMsg.length(),stringMsg.substring(endIndex));
		return returnString;
	}

	private String getBinaryBitMap(String bitmap1) {
		StringBuffer binaryBitMap= new StringBuffer();
		char[] charArray = bitmap1.toCharArray();
		for(int i=0; i<charArray.length; i++){
			String charString = ""+charArray[i];
			String binary = Integer.toBinaryString(Integer.parseInt(charString, 16));
			int length = binary.length();
			if(length<4){
				for(int j=0; j<4-length; j++){
					binary="0"+binary;
				}
			}
			binaryBitMap.append(binary);
		}
		return binaryBitMap.toString();
	}
	
	private static RawResponseMessage getResponseInASCII(String message) {
		RawResponseMessage responseMessage = new RawResponseMessage();
		responseMessage.setFullMessage(message);
		responseMessage.setFirst4ByteMessage(message.substring(IP_HEADER_BEGIN_INDEX, IP_HEADER_END_INDEX));			
		int headerLength = getMessageLengthInDecimal(message.substring(HEADER_LENGTH_BEGIN_INDEX, HEADER_LENGTH_END_INDEX));
		//If there is rejected Header length will 26 to 36 bytes
		if(headerLength!=22){
			int requestHeaderLengthBeginIndex = 2*(headerLength+4);
			int requestHeaderLengthEndIndex = 2*(headerLength+4)+2;
			int rejectCodeBeginIndex = 2*(headerLength+2);
			int rejectCodeEndIndex = 2*(headerLength+4);
			int orginalHeaderLen= getMessageLengthInDecimal(message.substring(requestHeaderLengthBeginIndex, requestHeaderLengthEndIndex));
			responseMessage.setRejectCode(message.substring(rejectCodeBeginIndex, rejectCodeEndIndex));
			headerLength = orginalHeaderLen + headerLength;
		}
		int HEADER_VALUE_END_INDEX = 2*(headerLength+4);
		responseMessage.setHeaderLength(headerLength);
		responseMessage.setHeaderString(message.substring(HEADER_VALUE_BEGIN_INDEX, HEADER_VALUE_END_INDEX));
		String transactionTypeAndMessageBody = message.substring(HEADER_VALUE_END_INDEX);
		responseMessage.setMessageType(transactionTypeAndMessageBody.substring(MESSAGE_TYPE_BEGIN_INDEX, MESSAGE_TYPE_END_INDEX));
		responseMessage.setMessageBody(transactionTypeAndMessageBody.substring(MESSAGE_TYPE_END_INDEX));

		return responseMessage;
	}
}
