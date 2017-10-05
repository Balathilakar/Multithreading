package com.amfam.billing.acquirer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amfam.billing.acquirer.dataaccess.businesstobusiness.ProtocolServiceException;

/**
 * This class is called when there is disconnect in existing connection.
 * It has a logic to make either primary or secondary or all connections active.
 * 
 */
public class SaratogaProtocolServiceController implements
		SaratogaProtocolServiceControllerMBean {
	
	private static Log LOG = LogFactory.getLog(SaratogaProtocolServiceController.class);
	private int numberOfAutoReconnects = 0;
	public boolean makePrimaryActive() {
		try {
			LOG.error("Make Primary active fired");
			SaratogaProtocolService.getInstance().makePrimaryActive();
		} catch (ProtocolServiceException e) {
			LOG.error(e, e);
			return false;
		}
		return true;

	}

	public boolean makeSecondaryActive() {
		try {
			LOG.error("Make secondary active fired");
			SaratogaProtocolService.getInstance().makeSecondaryActive();
		} catch (ProtocolServiceException e) {
			LOG.error(e, e);
			return false;
		}
		return true;

	}

	public boolean reconnectAll() {
		try {
			LOG.error("Reconnect All fired");
			SaratogaProtocolService.getInstance().reset();
			//reinitialize
			SaratogaProtocolService.getInstance();
		} catch (ProtocolServiceException e) {
			LOG.error(e, e);
			return false;
		}
		return true;
	}

	public boolean reconnectPrimary() {
		try {
			LOG.error("Reconnect Primary fired");
			SaratogaProtocolService.getInstance().reconnectPrimary();
		} catch (ProtocolServiceException e) {
			LOG.error(e, e);
			return false;
		}
		return true;
	}

	public boolean reconnectSecondary() {
		try {
			LOG.error("Reconnect secondary fired");
			SaratogaProtocolService.getInstance().reconnectSecondary();
		} catch (ProtocolServiceException e) {
			LOG.error(e, e);
			return false;
		}
		return true;
	}

	public String getActiveServer() {
		try {
			return SaratogaProtocolService.getInstance().getActiveServer();
		} catch (ProtocolServiceException e) {
			LOG.error(e, e);
			return "Failed to determine Active Server:";// + e.getMessage();
		}
	}

	public int getNumberOfAutoReconnects() {
		return this.numberOfAutoReconnects;
	}

	public void setNumberOfAutoReconnects(int number) {
		this.numberOfAutoReconnects = number;
	}
	public void incrementNumberOfAutoReconnects(){
		this.numberOfAutoReconnects++;
	}
	
	/**
	 * mbean facade to display the status for all the protocol handlers
	 * @return String of protocol handler status
	 */
	public String listProtocolHandlerStatus(){
		try {
			return SaratogaProtocolService.getInstance().listProtocolHandlerStatus();
		} catch (Throwable e) {
			LOG.error("Failed to list protocol handlers", e);
			return "Failed to list protocol handlers";
		}
	}
	

	/**
	 * mbean facade to set the shutting down attribute on the specific handler
	 * @param int index of the handler in the handler array
	 * @param boolean whether or not to shut it down
	 * @return boolean on success or fail
	 */
	public boolean setIsShuttingDown(int index, boolean shuttingdown) {
		try {
			return SaratogaProtocolService.getInstance().setIsShuttingDown(index, shuttingdown);
		} catch (Throwable e) {
			LOG.error("controller mbean failed to shutdown", e);
			return false;
		}
	}


}
