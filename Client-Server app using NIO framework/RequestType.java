package com.amfam.billing.acquirer;

import java.util.HashMap;

public enum RequestType {
	
	SIGN_ON ("0800"), 
	KEEP_ALIVE("0800"),
	KEEP_ALIVE_RESPONSE("0810"),
	AUTH("0110"), AUTH_REQUEST("0100"),
	SOFT_SHUT("0401"), OTHER("OTHER");
	
	private static final String SIGN_ON_INF_CODE = "0001";
	private static final String KEEP_ALIVE_INF_CODE = "0301";
	
	private String value;
	
	private RequestType(String value){
		this.value = value;
	}
	
	public String toString() {
		return value;
	}
	
	public static RequestType find(HashMap<Integer, Object> dataFields, String msgType) {
        for (RequestType value : RequestType.values()) {
        	if (value.toString().equals(msgType)) {
        		String infCode = (String)dataFields.get(70);
        		if(infCode!= null && infCode.equals(SIGN_ON_INF_CODE))
        		return SIGN_ON;
        		else if (infCode!= null && infCode.equals(KEEP_ALIVE_INF_CODE))
        		return KEEP_ALIVE;
        		else if (infCode!= null && infCode.equals(SOFT_SHUT.value))
            	return SOFT_SHUT;
        		else if(value==AUTH || value==AUTH_REQUEST)
        		return AUTH;
            }
        }
        return OTHER;
    }
	

}
