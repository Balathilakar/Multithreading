package com.amfam.billing.acquirer;

import com.amfam.billing.acquirer.dataaccess.businesstobusiness.ProtocolServiceException;

/**
 * @author bxr043
 */
public interface ProtocolService
{
	/**
	 * Attempt to authorize the given Request with the acquirer using the
	 * specific protocol that the implementing class defines.
	 * 
	 * @param req The Request to authorize.
	 * @return The Response received from the acquirer.
	 */
	public Response authorize(Request req) throws ProtocolServiceException;

	/**
	 * Shuts down this ProtocolService and cleans up any resources.
	 */
	public void shutdown();
	
	/**
	 * Attempts to reconnect primary connection.
	 * @return true if successful
	 */
	public boolean reconnectPrimary();
	
	/**
	 * Attempts to reconnect secondary connection.
	 * @return true if successful
	 */
	public boolean reconnectSecondary();
	
	/**
	 * Sets the currently active connection to primary.
	 * @return true if successful
	 */
	public boolean makePrimaryActive();
	
	/**
	 * Sets the currently active connection to secondary.
	 * @return true if successful
	 */
	public boolean makeSecondaryActive();
	
	/**
	 * Returns informational string about the currently active server
	 * @return
	 */
	public String getActiveServer();
}
