package com.amfam.billing.acquirer;

public class UnexpectedDataFieldException extends RuntimeException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4446878349084807364L;

	public UnexpectedDataFieldException(String field){
		super(field);
	}

}
