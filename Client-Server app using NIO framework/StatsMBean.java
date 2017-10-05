package com.amfam.billing.acquirer;

public interface StatsMBean 
{
	public int getWaitingMessageCount();
	public int getBadHeaders();
	public int getBadMessages();
	public int getConnectFailures();
	public int getKeepAliveResponsesReceived();
	public int getKeepAliveRequestsSent();
	public int getJunkMessages();
	public long getMaximumResponseTime();
	public int getMessagesSent();
	public long getMinimumResponseTime();
	public int getReconnects();
	public int getResponsesReceived();
	public long getTotalResponseTime();
	public int getSendTimeouts();
	public int getReceiveTimeouts();
	public int getSendErrors();
	public int getTotalProblems();
	public void reset();
	public int getSoftShutReceived();

	//only for verification mode of the mbean
	public void setTotalProblems(int totalProblems);
	public void setWaitingMessageCount(int waitingMessageCount);
	public int getTestMode();
	
	/*
	 * only for verification mode of the mbean 0 is default, 1 is the test mode
	 */
	public void setTestMode(int testMode);
}
