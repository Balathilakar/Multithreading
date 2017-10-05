/**
 * 
 */
package com.amfam.billing.acquirer;

/**
 *
 */
public interface SaratogaProtocolServiceControllerMBean {

	public boolean makePrimaryActive();
	public boolean makeSecondaryActive();
	public boolean reconnectAll();
	public boolean reconnectPrimary();
	public boolean reconnectSecondary();
	public String getActiveServer();
	public int getNumberOfAutoReconnects();
	public void setNumberOfAutoReconnects(int number);
	public void incrementNumberOfAutoReconnects();
	public String listProtocolHandlerStatus();
	public boolean setIsShuttingDown(int index, boolean shuttingdown);
}
