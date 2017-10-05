package com.amfam.billing.acquirer;

/**
 * This is the interface for an acquirer debit/credit card authorization response.
 * 
 */
public interface Response {
	/**
	 * @return the correlation ID associated with this specific authorization request/response
	 */
	public String getCorrelationId();
	/**
	 * @return true if this authorization response contains a successful response code
	 */
	public boolean isSuccessful();
	/**
	 * @return the response code associated with this response
	 */
	public PaymentCardResponseCode getResponseCode();

}
