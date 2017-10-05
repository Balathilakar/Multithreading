package com.amfam.billing.acquirer;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Response returned after authorization 
 * 
 * @author bxl028
 *
 */

public class SaratogaAuthResponse extends SaratogaResponse {
	
	/**
	 * generated serialVersionUID
	 */
	private static final long serialVersionUID = 1854603544177620429L;
		
	String creditCardNumber;
	String expirationDate;
	BigDecimal amount;
	String correlationId;
	String methodOfPayment;
	SaratogaResponseCode responseCode;
	String recordType;
	Date responseDate;
	int responseDateNum;
	int responseTimeNum;
	String authorizationCode;
	String avsResponseCode;
	String csvResponse;
	String recurringPaymentAdviceCode;
	String cavvResponseCode;
	String responseText;
	RequestType requestType;
	String rejectCode;
	/*
	 * Variable is transient because there is no need to return the raw response over the network to Payment Manager
	 */
	transient String rawResponse;
	
	
	 /**
	 * @param authRequest
	 * 
	 * This method is used to map the Authorization response received from TSYS to SaratogaAuthResponse.
	 */
	void mapSaratogaResponse(){
		 
	 this.creditCardNumber = (String)datafields.get(2);
	 this.expirationDate = (String)datafields.get(14);
	 BigDecimal amountwithOutFraction =  new BigDecimal ((String)datafields.get(4));
	 if(amountwithOutFraction!=null)
	 this.amount = amountwithOutFraction.divide(new BigDecimal(100));
	 this.correlationId =  (String)datafields.get(37); 
	 this.responseCode = new SaratogaResponseCode( "0"+(String)datafields.get(39));
	 this.authorizationCode = (String)datafields.get(38);
	 String avsData = (String)datafields.get(44);
	 if(avsData!=null && avsData.length()>=2){
		 this.avsResponseCode = avsData.substring(1, 2);
		 if(avsData.length()>=13)
		 this.cavvResponseCode = avsData.substring(12, 13);
		 if(avsData.length()>=14)
		 this.recurringPaymentAdviceCode = avsData.substring(13, 14);
	 }
	 this.responseDate = new Date();
	 
	 SimpleDateFormat sd = new SimpleDateFormat("yyMMdd HHmmss", Locale.US);
	 String dateTime = sd.format(this.responseDate);
	 this.responseDateNum = Integer.parseInt(dateTime.substring(0, 6));
	 this.responseTimeNum = Integer.parseInt(dateTime.substring(7, 13));
	}

	
	public String getCorrelationId() {
		return correlationId;
	}

	public PaymentCardResponseCode getResponseCode() {
		return responseCode;
	}
	
	public boolean isSuccessful() {
		if(responseCode.getCode().equals("000")){
			 return true;
		 }
		return false;
	}

	/**
	 * @return creditCardNumber
	 */
	public String getCreditCardNumber() {
		return creditCardNumber;
	}

	/**
	 * @param creditCardNumber
	 */
	public void setCreditCardNumber(String creditCardNumber) {
		this.creditCardNumber = creditCardNumber;
	}

	/**
	 * @return expirationDate
	 */
	public String getExpirationDate() {
		return expirationDate;
	}

	/**
	 * @param expirationDate
	 */
	public void setExpirationDate(String expirationDate) {
		this.expirationDate = expirationDate;
	}

	/**
	 * @return amount
	 */
	public BigDecimal getAmount() {
		return amount;
	}

	/**
	 * @param amount
	 */
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	/**
	 * @return methodOfPayment
	 */
	public String getMethodOfPayment() {
		return methodOfPayment;
	}

	/**
	 * @param methodOfPayment
	 */
	public void setMethodOfPayment(String methodOfPayment) {
		this.methodOfPayment = methodOfPayment;
	}

	/**
	 * @return recordType
	 */
	public String getRecordType() {
		return recordType;
	}

	/**
	 * @param recordType
	 */
	public void setRecordType(String recordType) {
		this.recordType = recordType;
	}


	/**
	 * @return responseDate
	 */
	public Date getResponseDate() {
		return responseDate;
	}

	/**
	 * @param responseDate
	 */
	public void setResponseDate(Date responseDate) {
		this.responseDate = responseDate;
	}


	/**
	 * @return authorizationCode
	 */
	public String getAuthorizationCode() {
		return authorizationCode;
	}

	/**
	 * @param authorizationCode
	 */
	public void setAuthorizationCode(String authorizationCode) {
		this.authorizationCode = authorizationCode;
	}

	public String getAvsResponseCode() {
		return avsResponseCode;
	}

	public void setAvsResponseCode(String avsResponseCode) {
		this.avsResponseCode = avsResponseCode;
	}

	public String getCsvResponse() {
		return csvResponse;
	}

	public void setCsvResponse(String csvResponse) {
		this.csvResponse = csvResponse;
	}

	public String getRecurringPaymentAdviceCode() {
		return recurringPaymentAdviceCode;
	}

	public void setRecurringPaymentAdviceCode(String recurringPaymentAdviceCode) {
		this.recurringPaymentAdviceCode = recurringPaymentAdviceCode;
	}

	public String getCavvResponseCode() {
		return cavvResponseCode;
	}

	public void setCavvResponseCode(String cavvResponseCode) {
		this.cavvResponseCode = cavvResponseCode;
	}

	public String getResponseText() {
		return responseText;
	}

	public void setResponseText(String responseText) {
		this.responseText = responseText;
	}

	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}

	public void setResponseCode(SaratogaResponseCode responseCode) {
		this.responseCode = responseCode;
	}

	public RequestType getRequestType() {
		return requestType;
	}

	public void setRequestType(RequestType requestType) {
		this.requestType = requestType;
	}

	public int getResponseDateNum() {
		return responseDateNum;
	}


	public void setResponseDateNum(int responseDateNum) {
		this.responseDateNum = responseDateNum;
	}


	public int getResponseTimeNum() {
		return responseTimeNum;
	}


	public void setResponseTimeNum(int responseTimeNum) {
		this.responseTimeNum = responseTimeNum;
	}


	public String getRejectCode() {
		return rejectCode;
	}


	public void setRejectCode(String rejectCode) {
		this.rejectCode = rejectCode;
	}


	public String getRawResponse() {
		return rawResponse;
	}


	public void setRawResponse(String rawResponse) {
		this.rawResponse = rawResponse;
	}
	
}
