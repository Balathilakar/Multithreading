/**
 * 
 */
package com.amfam.billing.acquirer;

import java.math.BigDecimal;


/**
 * This contains base functionality for messages in the Saratoga protocol.
 * 
 */
public interface SaratogaMessage extends Message {
	
	public String getCorrelationId();
	
	public String getConfirmationNumber();
	
	public BigDecimal getAmount();
}

