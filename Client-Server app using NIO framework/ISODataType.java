package com.amfam.billing.acquirer;

/**
 * 
 * @author Thilakar R
 *
 */
public enum ISODataType {
	
	NUMERIC ("NUMERIC"),
	DATE10 ("DATE10"),
	LLVAR ("LLVAR"),
	LLLVAR ("LLLVAR"),
	BITMAP ("BITMAP"),
	AMOUNT ("AMOUNT"), DATE4 ("DATE4"), ALPHA ("ALPHA");
	
	private String value;
	
	private ISODataType(String value){
		this.value = value;
	}
	
	public String toString() {
		return value;
	}
	
	public static ISODataType find(String name) {
        for (ISODataType value : ISODataType.values()) {
        	if (value.toString().equals(name)) {
        		return value;
            }
        }
        return null;
    }

}
