package com.amfam.billing.acquirer;

import java.math.BigDecimal;
import com.amfam.receipt.finacct.CreditCardNumber;
import com.amfam.receipt.finacct.ExpirationDate;

/**
 * This is the interface for an acquirer debit/credit card authorization request.
 * 
 */
public interface Request {
	/**
	 * @return the card number for which the authorization request is being made
	 */
	public CreditCardNumber getCreditCardNumber();
	/**
	 * @return the expiration date for the card for which the authorization request is being made
	 */
	public ExpirationDate getExpirationDate();
	/**
	 * @return the amount of the debit/credit card authorization request
	 */
	public BigDecimal getAmount();
	/**
	 * @return true if this authorization request should be processed as a debit authorization
	 */
	/*public boolean getProcessAsDebit();*/
	/**
	 * @return the correlation ID associated with this specific authorization request
	 */
	public String getCorrelationId();
	/**
	 * @param amount The amount of the debit/credit card authorization request
	 */
	public void setAmount(BigDecimal amount);
	/**
	 * @param creditCardNumber The card number for which the authorization request is being made
	 */
	public void setCreditCardNumber(CreditCardNumber creditCardNumber);
	/**
	 * @param date The expiration date for the card for which the authorization request is being made
	 */
	public void setExpirationDate(ExpirationDate date);
	/**
	 * @param processAsDebit Set to true if this authorization request should be processed as a debit authorization
	 */
	/*public void setProcessAsDebit(boolean processAsDebit);*/
	/**
	 * @param correlationId The correlation ID associated with this specific authorization request
	 */
	public void setCorrelationId(String correlationId);
	
	/**
	 * @return confirmation Number
	 */
	public String getConfirmationNumber();
	
}
