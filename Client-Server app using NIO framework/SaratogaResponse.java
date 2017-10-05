/**
 * 
 */
package com.amfam.billing.acquirer;

import java.io.Serializable;
import java.util.HashMap;

/**
 *
 */
public abstract class SaratogaResponse implements Response, Serializable{

	/**
	 * Generated serialVersionUID
	 */
	private static final long serialVersionUID = 4648213646357733457L;
	protected HashMap<Integer,Object> datafields = new HashMap<Integer,Object>();
	protected String binaryBitmap;
	
	public HashMap<Integer, Object> getDatafields() {
		return datafields;
	}

	public void setDatafields(HashMap<Integer, Object> datafields) {
		this.datafields = datafields;
	}

	public String getBinaryBitmap() {
		return binaryBitmap;
	}

	public void setBinaryBitmap(String binaryBitamp) {
		this.binaryBitmap = binaryBitamp;
	}

}
