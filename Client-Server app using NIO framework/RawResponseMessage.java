package com.amfam.billing.acquirer;

/**
 * 
 * @author Raghu Devulapally
 *
 */
public class RawResponseMessage {
	
	private String headerString;
	private byte[] headerRawBytes;
	private String first4ByteMessage;
	private byte[] first4RawBytes;
	private String messageBody;
	private byte[] messageBodyRawBytes;
	private int headerLength;
	private int messageLength;
	private String fullMessage;
	private byte[] fullMessageRawBytes;
	private String messageType;
	private String rejectCode;
	
	
	public String getMessageType() {
		return messageType;
	}
	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}
	public byte[] getHeaderRawBytes() {
		return headerRawBytes;
	}
	public void setHeaderRawBytes(byte[] headerRawBytes) {
		this.headerRawBytes = headerRawBytes;
	}
	public byte[] getFirst4RawBytes() {
		return first4RawBytes;
	}
	public void setFirst4RawBytes(byte[] first4RawBytes) {
		this.first4RawBytes = first4RawBytes;
	}
	public byte[] getMessageBodyRawBytes() {
		return messageBodyRawBytes;
	}
	public void setMessageBodyRawBytes(byte[] messageBodyRawBytes) {
		this.messageBodyRawBytes = messageBodyRawBytes;
	}
	public byte[] getFullMessageRawBytes() {
		return fullMessageRawBytes;
	}
	public void setFullMessageRawBytes(byte[] fullMessageRawBytes) {
		this.fullMessageRawBytes = fullMessageRawBytes;
	}
	public String getHeaderString() {
		return headerString;
	}
	public void setHeaderString(String headerString) {
		this.headerString = headerString;
	}
	public String getFirst4ByteMessage() {
		return first4ByteMessage;
	}
	public void setFirst4ByteMessage(String first4ByteMessage) {
		this.first4ByteMessage = first4ByteMessage;
	}
	public String getMessageBody() {
		return messageBody;
	}
	@Override
	public String toString() {
		return this.getFullMessage();
	}
	public void setMessageBody(String messageBody) {
		this.messageBody = messageBody;
	}
	public int getHeaderLength() {
		return headerLength;
	}
	public void setHeaderLength(int headerLength) {
		this.headerLength = headerLength;
	}
	public int getMessageLength() {
		return messageLength;
	}
	public void setMessageLength(int messageLength) {
		this.messageLength = messageLength;
	}
	public String getFullMessage() {
		return this.first4ByteMessage+this.headerString+this.messageBody;
	}
	public void setFullMessage(String fullMessage) {
		this.fullMessage = fullMessage;
	}
	public String getRejectCode() {
		return rejectCode;
	}
	public void setRejectCode(String rejectedCode) {
		this.rejectCode = rejectedCode;
	}
	
	 

}
