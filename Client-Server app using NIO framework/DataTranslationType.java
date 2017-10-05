package com.amfam.billing.acquirer;

/**
 * 
 * @author Thilakar R
 *
 */
public enum DataTranslationType {
	
	EBCIDIC ("EBCIDIC"), 
	NONE("NONE");
	
	private String value;
	
	private DataTranslationType(String value){
		this.value = value;
	}
	
	public String toString() {
		return value;
	}
	
	public static DataTranslationType find(String name) {
        for (DataTranslationType value : DataTranslationType.values()) {
        	if (value.toString().equals(name)) {
        		return value;
            }
        }
        return null;
    }

}
