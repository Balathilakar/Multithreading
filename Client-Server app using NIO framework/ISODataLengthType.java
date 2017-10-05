package com.amfam.billing.acquirer;

/**
 * 
 * @author Thilakar R
 *
 */
public enum ISODataLengthType {
	
	BYTES_LENGTH ("BYTES_LENGTH"),
	FIXED_LENGTH ("FIXED_LENGTH"),
	NUMERIC_LENGTH ("NUMERIC_LENGTH"), BITMAP_LENGTH ("BITMAP_LENGTH");
	
	private String value;
	
	private ISODataLengthType(String value){
		this.value = value;
	}
	
	public String toString() {
		return value;
	}
	
	public static ISODataLengthType find(String name) {
        for (ISODataLengthType value : ISODataLengthType.values()) {
        	if (value.toString().equals(name)) {
        		return value;
            }
        }
        return null;
    }

}
