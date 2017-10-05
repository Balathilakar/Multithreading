/*
 * Created on Sep 13, 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.amfam.billing.acquirer;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

	/**
	 * Connection statistics.
	 */
	public class Stats
		implements StatsMBean, 
			Timer.Listener
	{
		
		private static final Log LOG = LogFactory.getLog( Stats.class );
		private Timer statTimer;
		
		private int reconnects = 0;
		private int connectFailures = 0;
		private int messagesSent = 0;
		private int responsesReceived = 0;
		private int badHeaders = 0;
		private int badMessages = 0;
		private int junkMessages = 0;
		private int sendTimeouts = 0;
		private int receiveTimeouts = 0;
		private int sendErrors = 0;
		private int requestsMade = 0;
		private long minimumResponseTime = 0;
		private long maximumResponseTime = 0;
		private long totalResponseTime = 0;
		private int totalProblems = 0; //only for verification mode of the mbean
		private int waitingMessageCount; //only for verification mode of the mbean
		private int testMode = 0; //only for verification mode of the mbean
		private SaratogaProtocolHandler saratogaProtocolHandler = null;
		
		private int keepAliveRequestsSent = 0;
		private int keepAliveResponsesReceived = 0;
		private int softShutReceived = 0;
		
		
		protected Stats(SaratogaProtocolHandler ph, Timer statTimer) {
			this.saratogaProtocolHandler = ph;
			this.statTimer = statTimer;
		}
		
		public Stats() {
			
		}
		
		public int getWaitingMessageCount()
		{
			//only for verification mode of the mbean
			if (testMode == 1){
				return waitingMessageCount;
			}
			
			if (saratogaProtocolHandler != null) {
				List requests = saratogaProtocolHandler.getRequests();
				synchronized( requests )
				{
					return requests.size();
				}
			}
			else {
				return 0;
			}
		}

		public synchronized int getBadHeaders() {
			return badHeaders;
		}

		public synchronized int getBadMessages() {
			return badMessages;
		}

		public synchronized int getConnectFailures() {
			return connectFailures;
		}

		public synchronized int getKeepAliveResponsesReceived() {
			return keepAliveRequestsSent;
		}

		public synchronized int getKeepAliveRequestsSent() {
			return keepAliveResponsesReceived;
		}

		public synchronized int getJunkMessages() {
			return junkMessages;
		}

		public synchronized long getMaximumResponseTime() {
			return maximumResponseTime;
		}

		public synchronized int getMessagesSent() {
			return messagesSent;
		}

		public synchronized long getMinimumResponseTime() {
			return minimumResponseTime;
		}

		public synchronized int getReconnects() {
			return reconnects;
		}

		public synchronized int getResponsesReceived() {
			return responsesReceived;
		}

		public synchronized long getTotalResponseTime() {
			return totalResponseTime;
		}

		public synchronized int getSendTimeouts() {
			return sendTimeouts;
		}

		public synchronized int getReceiveTimeouts() {
			return receiveTimeouts;
		}

		public synchronized int getSendErrors() {
			return sendErrors;
		}

		public synchronized int getTotalProblems() {
			if (testMode == 1){
				return totalProblems;
			}
			return getConnectFailures()
				+ getBadHeaders()
				+ getBadMessages()
				+ getJunkMessages()
				+ getSendTimeouts()
				+ getReceiveTimeouts()
				+ getSendErrors();
		}

		protected synchronized void badHeader()
		{
			if( LOG.isDebugEnabled() )
			{
				LOG.debug( "badHeader" );
			}
			
			badHeaders++;
		}

		protected synchronized void sendTimeout()
		{
			if( LOG.isDebugEnabled() )
			{
				LOG.debug( "sendTimeout" );
			}
			
			sendTimeouts++;
		}

		protected synchronized void receiveTimeout()
		{
			if( LOG.isDebugEnabled() )
			{
				LOG.debug( "receiveTimeout" );
			}
			
			receiveTimeouts++;
		}

		protected synchronized void sendError()
		{
			if( LOG.isDebugEnabled() )
			{
				LOG.debug( "sendError" );
			}
			
			sendErrors++;
		}

		protected synchronized void badMessage()
		{
			if( LOG.isDebugEnabled() )
			{
				LOG.debug( "badMessage" );
			}
			
			badMessages++;
		}

		protected synchronized void connectFailure() 
		{
			if( LOG.isDebugEnabled() )
			{
				LOG.debug( "connectFailure" );
			}
			
			connectFailures++;
		}

		protected synchronized void keepAliveResponseReceived()
		{
			if( LOG.isDebugEnabled() )
			{
				LOG.debug( "keepAliveResponseReceived" );
			}
			
			keepAliveResponsesReceived++;
		}
		
		protected synchronized void keepAliveRequestSent()
		{
			if( LOG.isDebugEnabled() )
			{
				LOG.debug( "keepAliveRequestSent" );
			}
			
			keepAliveRequestsSent++;
		}
		
		protected synchronized void softShutReceived()
		{
			if( LOG.isDebugEnabled() )
			{
				LOG.debug( "softShutReceived" );
			}
			
			softShutReceived++;
		}

		protected synchronized void junkMessage()
		{
			if( LOG.isDebugEnabled() )
			{
				LOG.debug( "junkMessage" );
			}
			
			junkMessages++;
		}

		protected synchronized void messageSent()
		{
			if( LOG.isDebugEnabled() )
			{
				LOG.debug( "messageSent" );
			}
			
			messagesSent++;
		}

		protected synchronized void reconnect()
		{
			if( LOG.isDebugEnabled() )
			{
				LOG.debug( "reconnect" );
			}
			
			reconnects++;
		}

		protected synchronized void responseReceived( long t0 )
		{
			if( LOG.isDebugEnabled() )
			{
				LOG.debug( "responseReceived" );
			}
			
			long t1 = ( System.currentTimeMillis() - t0 );
			totalResponseTime += t1;
			
			if( minimumResponseTime > t1 )
			{
				minimumResponseTime = t1;
			}
			
			if( maximumResponseTime < t1 )
			{
				maximumResponseTime = t1;
			}
			
			responsesReceived++;
		}

		protected synchronized void startMessage()
		{
			if( LOG.isDebugEnabled() )
			{
				LOG.debug( "startMessage" );
			}
			
			requestsMade++;
		}

		public synchronized String toString()
		{
			StringBuffer b = new StringBuffer();
			
			b.append( "Connection statistics" )
				.append( "\n\tWaiting Messages:           " ).append( getWaitingMessageCount() )
				.append( "\n\tReconnects:                 " ).append( reconnects )
				.append( "\n\tConnection Failures:        " ).append( connectFailures )
				.append( "\n\tKeepAlive Request Sent:   " ).append( keepAliveRequestsSent )
				.append( "\n\tKeepAlive Response Received:        " ).append( keepAliveResponsesReceived )
				.append( "\n\tSoftshut Received:        " ).append( softShutReceived )
				.append( "\n\tMessages Sent:              " ).append( messagesSent )
				.append( "\n\tResponses Received:         " ).append( responsesReceived )
				.append( "\n\tBad Headers:                " ).append( badHeaders )
				.append( "\n\tBad Messages:               " ).append( badMessages )
				.append( "\n\tJunk Messages:              " ).append( junkMessages )
				.append( "\n\tSend Timeouts:              " ).append( sendTimeouts )
				.append( "\n\tReceive Timeouts:           " ).append( receiveTimeouts )
				.append( "\n\tSend Errors:                " ).append( sendErrors )
				.append( "\n\tRequests Made:              " ).append( requestsMade )
				.append( "\n\tMinimum Response Time (ms): " ).append( minimumResponseTime )
				.append( "\n\tMaximum Response Time (ms): " ).append( maximumResponseTime )
				.append( "\n\tMean Response Time (ms):    " );

			if( responsesReceived > 0 )
			{
				b.append( totalResponseTime / responsesReceived );
			}
			else
			{
				b.append( 0 );
			}
							
			return b.toString();
		}
		
		public synchronized void reset()
		{
			reconnects = 0;
			connectFailures = 0;
			keepAliveRequestsSent = 0;
			keepAliveResponsesReceived = 0;
			softShutReceived = 0;
			messagesSent = 0;
			responsesReceived = 0;
			badHeaders = 0;
			badMessages = 0;
			junkMessages = 0;
			sendTimeouts = 0;
			receiveTimeouts = 0;
			sendErrors = 0;
			requestsMade = 0;
			minimumResponseTime = 0;
			maximumResponseTime = 0;
			totalResponseTime = 0;
		}
		
		public void expire()
		{
			LOG.info( toString() );
			if (statTimer != null) {
				statTimer.arm();
			}
		}

		public int getTestMode() {			
			return testMode;
		}

		public void setTestMode(int testMode) {
			this.testMode = testMode;
			if (testMode == 1){
				LOG.error("MBEAN RUNNING IN TEST MODE");
			}
		}

		public void setTotalProblems(int totalProblems) {
			if (testMode == 1){
				this.totalProblems = totalProblems;
			}
			
		}

		public void setWaitingMessageCount(int waitingMessageCount) {
			if (testMode == 1){
				this.waitingMessageCount = waitingMessageCount;
			}
			
		}

		public synchronized int getSoftShutReceived() {
			return softShutReceived;
		}

		public void setSoftShutReceived(int softShutReceived) {
			this.softShutReceived = softShutReceived;
		}
	}
