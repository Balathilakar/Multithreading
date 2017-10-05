package com.amfam.billing.acquirer;

/**
 * @author bxr043
 */
public interface ProtocolHandler
{
	/**
	 * Send a message to the acquirer using the specific protocol that the
	 * implementing class defines.
	 * 
	 * @param req The Message to send
	 * @param l The Listener to notify on the condition of the send.
	 */
	public void send(Message req, Listener l);

	/**
	 * Listener defines the callback methods that a ProtocolHandler client must
	 * supply in order be notified of message completion.
	 * 
	 * @author ajm045
	 */
	interface Listener
	{
		public void timeout( Message request );
		public void error( Message request, Throwable x );
		public void response( Message request, Response x );
		public void notSent( Message request );
	}

}
