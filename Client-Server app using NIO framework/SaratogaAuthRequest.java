package com.amfam.billing.acquirer;

import java.math.BigDecimal;
import java.util.Date;

import com.amfam.receipt.finacct.CreditCardNumber;
import com.amfam.receipt.finacct.ExpirationDate;
/**
 * 
 * SaratogaAuthRequest is a transfer object.
 *
 */
public class SaratogaAuthRequest  implements SaratogaMessage, Request {
	
	
	private BigDecimal amount ; 
	private String stateCode;
	private Date transactionDate;
	private String zip;
	private String merchantId;
	private CreditCardNumber creditCardNumber;
	private ExpirationDate expirationDate;
	private String correlationId;
	private String confirmationNumber;
	private String stanNumber;
	
	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public Date getTransactionDate() {
		return transactionDate;
	}

	public void setTransactionDate(Date transactionDate) {
		this.transactionDate = transactionDate;
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public String getMerchantId() {
		return merchantId;
	}

	public void setMerchantId(String merchantId) {
		this.merchantId = merchantId;
	}

	public CreditCardNumber getCreditCardNumber() {
		return creditCardNumber;
	}

	public void setCreditCardNumber(CreditCardNumber creditCardNumber) {
		this.creditCardNumber = creditCardNumber;
	}

	public ExpirationDate getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(ExpirationDate expirationDate) {
		this.expirationDate = expirationDate;
	}

	public String getStateCode() {
		return stateCode;
	}

	public void setStateCode(String stateCode) {
		this.stateCode = stateCode;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}

	public String getStanNumber() {
		return stanNumber;
	}

	public void setStanNumber(String stanNumber) {
		this.stanNumber = stanNumber;
	}

	public String getConfirmationNumber() {
		return confirmationNumber;
	}

	public void setConfirmationNumber(String confirmationNumber) {
		this.confirmationNumber = confirmationNumber;
	}


}
