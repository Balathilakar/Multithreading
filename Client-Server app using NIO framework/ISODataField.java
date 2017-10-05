package com.amfam.billing.acquirer;

/**
 * 
 * @author Thilakar R
 *
 */
public class ISODataField {
	
	private ISODataType dataType;
	private int fieldLength;
	private DataTranslationType translationType;
	private ISODataLengthType dataLengthType;
	private String length;
	

	public ISODataField(ISODataType llvar, int length, DataTranslationType ebcidic, ISODataLengthType numericLength) {
		this.fieldLength=length;
		this.dataType=llvar;
		this.translationType=ebcidic;
		this.dataLengthType=numericLength;
	}


	public ISODataType getDataType() {
		return dataType;
	}


	public void setDataType(ISODataType dataType) {
		this.dataType = dataType;
	}


	public int getFieldLength() {
		return fieldLength;
	}


	public void setFieldLength(int fieldLength) {
		this.fieldLength = fieldLength;
	}


	public DataTranslationType getTranslationType() {
		return translationType;
	}


	public void setTranslationType(DataTranslationType translationType) {
		this.translationType = translationType;
	}


	public ISODataLengthType getDataLengthType() {
		return dataLengthType;
	}


	public void setDataLengthType(ISODataLengthType dataLengthType) {
		this.dataLengthType = dataLengthType;
	}


	public String getLength() {
		return length;
	}


	public void setLength(String length) {
		this.length = length;
	}

}
