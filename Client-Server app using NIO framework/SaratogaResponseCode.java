package com.amfam.billing.acquirer;

public class SaratogaResponseCode implements PaymentCardResponseCode{

	private String code;

	/**
	 * 
	 */
	public SaratogaResponseCode() {
		super();
	}

	/**
	 * 
	 */
	public SaratogaResponseCode(String responseCode) {
		super();
		code = responseCode;
	}

	/**
	 * @return
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @param string
	 */
	public void setCode(String string) {
		code = string;
	}

}


