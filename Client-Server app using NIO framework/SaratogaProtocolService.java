/**
 * 
 */
package com.amfam.billing.acquirer;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amfam.billing.acquirer.dataaccess.businesstobusiness.AcquirerConnectionException;
import com.amfam.billing.acquirer.dataaccess.businesstobusiness.ProtocolServiceException;
import com.amfam.billing.acquirer.dataaccess.businesstobusiness.TimeoutException;
import com.amfam.billing.acquirer.dataaccess.jdbc.AcquirerJdbcDAO;
import com.amfam.billing.reuse.Monitor;
import com.amfam.billing.reuse.util.Strings;
import com.amfam.receipt.finacct.CreditCardNumber;

/**
 * 
 * Service to call the DB and get the Alias names / IP Address of the host we need to connect and creates the connections.
 * Doe's validations of all input parameters we are passing. Register the MBean's to Monitor the stat's.
 * Send's the authorization request to handler and wait for the response to be notified through listener.
 * 
 */
public class SaratogaProtocolService implements ProtocolService {
	private static final Log LOG = LogFactory.getLog(SaratogaProtocolService.class);
	private static final Log LOGRESPONSE = LogFactory.getLog("com.amfam.billing.acquirerCommResponse");

	/**
	 * max wait time on a send.
	 * 
	 * <p>
	 * This is just over two minutes as the connection wait time is one minute
	 * so the largest possible wait is 2 minutes plus some slop.
	 */
	public static final long MAX_SEND_WAIT_TIME = RequestItem.MAX_WAIT_TIME * 2L + 1000L;
	public static final long MAX_CONNECTION_START_TIME = 60000;
	public static final long MAX_CONNECTION_TERMINATE_TIME = 15000;

	private static SaratogaProtocolService INSTANCE;

	private static AcquirerJdbcDAO dao;

	public static final int X_UNEXPECTED_TYPE = 4110;
	public static final String X_GENERIC_EXCEPTION = "4110";

	static {
		staticInit();
	}

	private SaratogaProtocolHandler[] ph = null;

	private static int nextPh = 0;

	SaratogaMessageParser parser = new SaratogaMessageParser();

	/**
	 * looks up list of address/port items gets arraylist, splits address and
	 * port, creates handler array, init
	 * 
	 * @param addressAndPorts
	 */
	public SaratogaProtocolService() throws ProtocolServiceException {
		try {
			ObjectName objNm = new ObjectName("com.amfam.billing.acquirer:type=SaratogaProtocolServiceController");
			Monitor.getInstance().getMBeanServer().registerMBean(new SaratogaProtocolServiceController(), objNm);
		} catch (InstanceAlreadyExistsException e) {
			LOG.info("SaratogaProtocolServiceController already registered", e);
		} catch (MBeanRegistrationException e) {
			LOG.error(e, e);
		} catch (NotCompliantMBeanException e) {
			LOG.error(e, e);
		} catch (MalformedObjectNameException e) {
			LOG.error(e, e);
		} catch (NullPointerException e) {
			LOG.error(e, e);
		}

		init();
	}

	private void init() throws ProtocolServiceException {

		List<AddressPort> addressAndPortList = dao.getDefinedConnections();
		ph = new SaratogaProtocolHandler[addressAndPortList.size()];
		nextPh = 0;
		init(addressAndPortList);
	}

	/**
	 * This constructor was made public so Spring could use it.
	 * 
	 * @param addresses
	 *            A comma-separated list of IPs this service should use.
	 * @param ports
	 *            A comma-separated list of ports this service should use
	 *            corresponding to the list of addresses.
	 * @throws ProtocolServiceException
	 */
	public SaratogaProtocolService(String addresses, String ports) throws ProtocolServiceException {

		StringTokenizer addrs = new StringTokenizer(addresses, ",");
		StringTokenizer pts = new StringTokenizer(ports, ",");
		if (addrs.countTokens() != pts.countTokens()) {
			throw new IllegalArgumentException("Different number of addresses: " + addresses + " and ports: " + ports);
		}
		ph = new SaratogaProtocolHandler[addrs.countTokens()];
		init(addrs, pts);
	}

	/**
	 * The getInstance() of this singleton uses Spring to create itself.
	 * 
	 * @return The singleton instance of SaratogaProtocolService
	 * @throws ProtocolServiceException
	 */
	public static SaratogaProtocolService getInstance() throws ProtocolServiceException {
		if (INSTANCE == null) {
			synchronized (SaratogaProtocolService.class) {
				INSTANCE = new SaratogaProtocolService();
			}
		}

		return INSTANCE;
	}

	private static void staticInit() {
		try {
			dao = new AcquirerJdbcDAO();
		} catch (NamingException x) {
			throw new ExceptionInInitializerError(x);
		}

	}

	/**
	 * init off the arraylist of address and ports
	 * 
	 * @param addressAndPortObjList
	 * @throws ProtocolServiceException
	 */
	private void init(List<AddressPort> addressAndPortObjList) throws ProtocolServiceException {
		Iterator<AddressPort> iter = addressAndPortObjList.iterator();
		int i = 0;
		while (iter.hasNext()) {
			AddressPort ap = iter.next();
			try {
				connectProtocolHandler(i, InetAddress.getByName(ap.getAddress()), Integer.parseInt(ap.getPort()));
			} catch (NumberFormatException e) {
				LOG.error("Error - port could not be parsed: port = " + ap.getPort() + "\n" + e);
				throw new ProtocolServiceException(e);
			} catch (UnknownHostException e) {
				LOG.error("Error - address not recognized: address = " + ap.getAddress() + "\n" + e);
				throw new ProtocolServiceException(e);
			} catch (AcquirerConnectionException e) {
				LOG.error("Socket connection failed on the Address:" + ap.getAddress() + " and Port:" + ap.getPort());
				throw new ProtocolServiceException(e);
			} finally {
				i++;
			}
		}

	}

	private void init(StringTokenizer addresses, StringTokenizer ports) throws ProtocolServiceException {

		String address = null;
		String port = null;
		int i = 0;
		while (addresses.hasMoreTokens()) {
			try {
				address = addresses.nextToken().trim();
				port = ports.nextToken().trim();
				connectProtocolHandler(i, InetAddress.getByName(address), Integer.parseInt(port));
			} catch (NumberFormatException e) {
				LOG.error("Error - port could not be parsed: port = " + port + "\n" + e);
				throw new ProtocolServiceException(e);
			} catch (UnknownHostException e) {
				LOG.error("Error - address not recognized: address = " + address + "\n" + e);
				throw new ProtocolServiceException(e);
			} finally {
				i++;
			}
		}

	}

	/**
	 * Authorize the given request with the acquirer. Return the response from
	 * the acquirer in the given response.
	 * 
	 * @param req
	 *            The Request object that needs to be authorized.
	 * @return The Response from the acquirer.
	 * @throws ProtocolServiceException
	 */
	public Response authorize(Request req) throws ProtocolServiceException {
		if (!(req instanceof SaratogaAuthRequest))
			throw new IllegalArgumentException("request must be of type StratusAuthRequest");

		assertValid((SaratogaAuthRequest) req);
		// TODO: put all validation in setters on this class and
		// an isValid to make sure everything is set that needs to be

		Response response = send((SaratogaAuthRequest) req);
		LOGRESPONSE.error("This is not an error. Acquirer Response: correlationId:" + response.getCorrelationId() + " confirmation Number:"
				+req.getConfirmationNumber() + " amount: " + req.getAmount() + " respCode:"
				+ response.getResponseCode().getCode() + " success:" + response.isSuccessful());
		return response;
	}

	private void assertValid(SaratogaAuthRequest req) {

		// amount must be > 0
		if (req.getAmount() == null || req.getAmount().compareTo(new BigDecimal("0")) <= 0) {
			throw new IllegalArgumentException("Amount must be greater than 0");
		}

		if (req.getCreditCardNumber() == null || req.getCreditCardNumber().equals(CreditCardNumber.NO_NUMBER)) {
			throw new IllegalArgumentException("Credit Card Number must not be null");
		}

		// ExpDate must not be null
		if (req.getExpirationDate() == null) {
			throw new IllegalArgumentException("Expiration Date must not be null");
		}

		// ID must be equal to 12 digits in length, CorrelationID length is 12
		// as per TSYS
		if (req.getCorrelationId() == null || req.getCorrelationId().length() != 12) {
			throw new IllegalArgumentException("ID must be equal to 12 in length");
		}

	}

	/**
	 * Wait for a response to a Saratoga (TSYS) Message and pre-process it.
	 * 
	 * <p>
	 * This method will wait for upto {@link #MAX_SEND_WAIT_TIME} milliseconds.
	 * 
	 * <p>
	 * The response is returned and all the response values are also set on the
	 * parameter response. These values include Authorization Code, Response
	 * Reason Code, and Response Date.
	 * 
	 * @param req
	 *            The Seratoga request.
	 * @param resp
	 *            The Seratoga response.
	 * 
	 * @return The SeratogaAuthResponse that was recieved.
	 * 
	 * @throws ProtocolServiceException
	 *             from the listener's error attribute or if a response type
	 *             other than SeratogaAuthResponse was received.
	 * @throws TimeoutException
	 *             if the request timed out, was not sent or the listener was
	 *             not notified by {@link #MAX_SEND_WAIT_TIME}.
	 */
	private Response send(SaratogaAuthRequest req) throws ProtocolServiceException {

		SaratogaProtocolHandler ph = getProtocolHandler();
		Object monitor = new Object();
		NotifyingListener l = new NotifyingListener(monitor);
		char c[] = { ' ', '.' };
		LOG.debug("SaratogaProtocolService send correlation:" + Strings.stripNonAlphaNumericAndAllowCharset(req.getCorrelationId(), c));
		LOG.debug("SaratogaProtocolService send amount:" + req.getAmount());

		ph.send(req, l);

		l.waitForResponse();

		if (l.isResponse()) {
			return (SaratogaAuthResponse) l.getResponse();
		} else if (l.isTimeout() || l.isNotSent() || !l.isNotified()) {
			LOG.debug("isTimeOut:" + l.isTimeout() + "isNotSent:" + l.isNotSent() + "isNotified:" + l.isNotified);
			ph.getStats().connectFailure();
			LOG.error("SaratogaProtocolService timeout: CorrelationID:" + Strings.stripNonAlphaNumericAndAllowCharset(req.getCorrelationId(), c)
					+" Confirmation Number:"+req.getConfirmationNumber());
		
			throw new TimeoutException();
		} else if (l.isError()) {
			Throwable x = l.getError();
			if (x instanceof ProtocolServiceException) {
				ph.getStats().sendError();
				LOG.error("ProtocolServiceException 1: CorrleationID: " + Strings.stripNonAlphaNumericAndAllowCharset(req.getCorrelationId(), c)
						+" Confirmation Number:"+req.getConfirmationNumber());
				
				throw (ProtocolServiceException) x;
			} else {
				ph.getStats().sendError();
				LOG.error("ProtocolServiceException 2: CorrelationID: " + Strings.stripNonAlphaNumericAndAllowCharset(req.getCorrelationId(), c)
						+" Confirmation Number:"+req.getConfirmationNumber());
				
				throw new ProtocolServiceException("Exception processing authorization", x);
			}
		} else {
			ph.getStats().sendError();
			LOG.error("AssertionError: CorrelationID: " + Strings.stripNonAlphaNumericAndAllowCharset(req.getCorrelationId(), c)
					+" Confirmation Number:"+req.getConfirmationNumber());
			
			throw new AssertionError("Invalid listener state");
		}

	}

	/**
	 * this determines which protocol handler from the list of handlers will be
	 * used
	 * 
	 * @return
	 * @throws AcquirerConnectionException
	 */
	private SaratogaProtocolHandler getProtocolHandler() throws AcquirerConnectionException {
		SaratogaProtocolHandler saratogaPH = null;

		if (ph[nextPh] == null || !ph[nextPh].isAvailable()) {
			// nextPh = 0 using Primary - nextPh = 1 using Secondary
			LOG.error("Current protocol handler is down ... going to alternate. Index=" + nextPh);
			int originalPh = nextPh;
			nextPh = (nextPh + 1) % ph.length;
			if (ph[nextPh] == null || !ph[nextPh].isAvailable()) {
				LOG.error("Alternate protocol handler is down .. attempting to reconnect. Index=" + nextPh);
				try {
					reconnectHandler(nextPh);
					saratogaPH = ph[nextPh];
					LOG.error("Successfully reconnected to alternate handler.");
					
					/*
					 * using a different handler.. try to transfer requests
					 */
					transferRequests(ph[originalPh], saratogaPH);
				} catch (ProtocolServiceException ex) {
					nextPh = (nextPh + 1) % ph.length;
					try {
						LOG.error("Alternate protocol handler reconnect failed .. attempting to reconnect primary. Index=" + nextPh);
						reconnectHandler(nextPh);
						saratogaPH = ph[nextPh];
					} catch (ProtocolServiceException e) {
						// We're done - no connections available
						throw new AcquirerConnectionException("Tried alternating.. no connections available");
					}
				}
			} else {
				LOG.error("Using alternate protocol handler. Index=" + nextPh);
				saratogaPH = ph[nextPh];
				
				/*
				 * using a different handler.. try to transfer requests
				 */
				transferRequests(ph[originalPh], saratogaPH);
			}
		} else {
			saratogaPH = ph[nextPh];
		}
		if (saratogaPH != null) {
			LOG.debug("ProtocolHandler selected is name:" + saratogaPH.getName() + " hostname: " + saratogaPH.address.getHostName()
					+ " port: " + saratogaPH.address.getPort());
		}

		// if no connections are available
		if (saratogaPH == null) {
			throw new AcquirerConnectionException("No connections are available");
		}

		return saratogaPH;
	}

	private void connectProtocolHandler(int index, InetAddress addr, int port) throws AcquirerConnectionException {
		if (ph[index] == null) {
			LOG.debug("ph is null");

		} else if (!ph[index].isAvailable()) {
			LOG.debug("ph is not avalialble");
		}

		if (ph[index] == null || !ph[index].isAvailable()) {
			synchronized (this) {
				ph[index] = new SaratogaProtocolHandler(addr, port, this);
				ph[index].start();

				LOG.debug("current Thread : " + ph[index].getName());
				for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
					LOG.debug(ste);
				}

			}

		}

	}
	
	
	protected void transferRequests(SaratogaProtocolHandler handler, SaratogaProtocolHandler receiverHandler ){
		LOG.debug("protocol service fired transfer of requests across two handlers");
		try {
			synchronized(this){
				if(handler==null){
					LOG.error("transferring handler was null.. giving up");
					return;
				}
				if(receiverHandler==null){
					LOG.error("receiving handler was null.. giving up");
					return;
				}
				LinkedList<RequestItem> transferRequests = handler.transferRequests();
				for(RequestItem request : transferRequests){
					receiverHandler.receiveTransferredRequest(request);						
				}
					
			}
		} catch (Exception e) {
			LOG.error("transfer failed with error for transfer from (1)" + handler.getName(), e);
		}
		
	}
	
	/**
	 * transfer requests in the request queue from a specific handler to the next available handler if one is found
	 * this is fired from a handler that has received a soft shut request from tsys
	 * @param handler
	 */
	protected void transferRequests(SaratogaProtocolHandler handler ){
		LOG.debug("protocol service fired transfer of requests from a handler");
		try {
			synchronized(this){
				boolean hasARecipient = false;
				for(int i = 0; i<ph.length;i++){
					if(handler.equals(ph[i])){
						LOG.trace("found this handler in the list");
					}else{
						
						LOG.debug("found a different handler for the transfer: " + ph[i].getName());
						if(ph[i]!=null && ph[i].isAvailable()){							
							transferRequests(handler, ph[i]);
							hasARecipient = true;
						}else{
							//LOG.error("the found potential receiving handler was not available.  tranfer could not be performed");
							//attempt to reconnect this handler
							try {
								reconnectHandler(i);
								hasARecipient = true;
								transferRequests(handler, ph[i]);
							} catch (Exception e) {
								LOG.error("failure in attempting reconnect during transfer",e);
							}
							
						}
						
						return;
					}
				}
				if(!hasARecipient){
					LOG.error("the transfer from " + handler.getName() + " could not be performed");
				}
			}
		} catch (Exception e) {
			LOG.error("transfer failed with error for transfer from " + handler.getName(), e);
		}
		
	}

	/**
	 * Resets the service state and free resources.
	 * 
	 * <p>
	 * Forces the connection to be recreated on the next
	 * {@link #getProtocolHandler()} call. Any requests already submitted but
	 * not yet processed on the connection will not be completed; not sent
	 * notifications will be delivered.
	 */
	public synchronized void reset() {
		LOG.info("resetting connections");

		for (int i = 0; i < ph.length; i++) {
			terminateProtocolHandler(i);
		}
	}

	private void terminateProtocolHandler(int index) {

		if (ph[index] != null && ph[index].isRunning()) {
			ph[index].terminate();
			ph[index] = null;
		}
	}

	protected void finalize() {
		LOG.info("destroying Serive INSTANCE");
		reset();
	}

	private static class NotifyingListener implements ProtocolHandler.Listener {
		private volatile boolean isNotified = false;
		private boolean isNotSent = false;
		private boolean isTimeout = false;
		private Message request = null;
		private Throwable error = null;
		private Response response = null;
		private Object monitor;

		public NotifyingListener(Object monitor) {
			if (monitor == null) {
				throw new NullPointerException();
			}

			this.monitor = monitor;
		}

		public boolean isNotified() {
			synchronized (monitor) {
				return isNotified;
			}
		}

		public boolean isError() {
			return error != null;
		}

		public boolean isNotSent() {
			return isNotSent;
		}

		public boolean isTimeout() {
			return isTimeout;
		}

		public boolean isResponse() {
			return response != null;
		}

		public Throwable getError() {
			return error;
		}

		public Response getResponse() {
			return response;
		}

		public void error(Message request, Throwable error) {
			this.request = request;
			this.error = error;
			notifyMonitor();
		}

		public void notSent(Message request) {
			this.request = request;
			notifyMonitor();
		}

		public void response(Message request, Response response) {
			this.request = request;
			this.response = response;
			notifyMonitor();
		}

		public void timeout(Message request) {
			this.request = request;
			this.isTimeout = true;
			notifyMonitor();
		}

		private void notifyMonitor() {
			synchronized (monitor) {
				isNotified = true;
				monitor.notifyAll();
			}
		}

		public void waitForResponse() {
			long now = System.currentTimeMillis();
			long t1 = now + MAX_SEND_WAIT_TIME;
			while (!isNotified() && now < t1) {
				try {
					synchronized (monitor) {
						if (isNotified())
							break;
						monitor.wait(t1 - now);
					}
				} catch (InterruptedException x) {
				}

				now = System.currentTimeMillis();
			}
		}

	}

	public void shutdown() {
		reset();
	}

	public void reconnectAll() throws ProtocolServiceException {
		reset();
		init();
	}

	private void reconnectHandler(int index) throws ProtocolServiceException {
		SaratogaProtocolHandler handler = ph[index];
		if (handler != null) {
			handler.terminate();
		}
		ph[index] = null;

		List<AddressPort> addressPortList = dao.getDefinedConnections();
		AddressPort ap = addressPortList.get(index);

		try {
			connectProtocolHandler(index, InetAddress.getByName(ap.getAddress()), Integer.parseInt(ap.getPort()));
		} catch (NumberFormatException e) {
			throw new ProtocolServiceException(e);
		} catch (UnknownHostException e) {
			LOG.error("Error - address not recognized: address = " + ap.getAddress() + "\n" + e);
			throw new ProtocolServiceException(e);
		}
	}

	public boolean reconnectPrimary() {
		LOG.error("Reconnecting primary.");
		try {
			reconnectHandler(0);
			return true;
		} catch (ProtocolServiceException e) {
			LOG.error(e, e);
			return false;
		}
	}

	public boolean reconnectSecondary() {
		LOG.error("Reconnecting Secondary.");
		try {
			reconnectHandler(1);
			return true;
		} catch (ProtocolServiceException e) {
			LOG.error(e, e);
			return false;
		}
	}

	public boolean makePrimaryActive() {
		nextPh = 0;
		LOG.error("Making Primary Active");
		return true;
	}

	public boolean makeSecondaryActive() {
		nextPh = 1;
		LOG.error("Making Secondary Active");
		return true;
	}

	public String getActiveServer() {
		SaratogaProtocolHandler statusPh = ph[nextPh];
		if (statusPh != null && statusPh.isAvailable()) {
			return " hostname: " + statusPh.address.getHostName() + " port: " + statusPh.address.getPort();
		} else {
			return "Active server is not available.";
		}
	}
	
	/**
	 * mbean facade to display the status for all the protocol handlers
	 * @return string displaying the attributes on the handler
	 */
	public String listProtocolHandlerStatus() {

		LOG.debug("listing protocol handler status:");
		StringBuilder builder = new StringBuilder();
		synchronized(this){
			for(int i=0;i<ph.length;i++){
				SaratogaProtocolHandler handler = ph[i];
				if(i==nextPh){
					builder.append("PRIMARY: " + handler.address.getHostName() + " port:" + handler.address.getPort() + " INDEX:" + i + " handleravailable: " + handler.isAvailable() + " channelavailable:" + handler.isChannelAvailable() + " isshuttingdown:" + handler.isShuttingDown() + " terminated:" + handler.isTerminated() + ";\n");
				}else{
					builder.append(handler.address.getHostName() + " port:" + handler.address.getPort() + " INDEX:" + i + " handleravailable: " + handler.isAvailable() + " channelavailable:" + handler.isChannelAvailable() + " isshuttingdown:" + handler.isShuttingDown() + " terminated:" + handler.isTerminated() + ";\n");
				}
			}
		}
		LOG.debug(builder.toString());
		return builder.toString();
	}
	
	/**
	 * set the shutting down attribute on the specific handler
	 * @param int index of the handler in the handler array
	 * @param boolean whether or not to shut it down
	 * @return boolean on success or fail
	 */
	public boolean setIsShuttingDown(int index, boolean shuttingdown){
		try{
			SaratogaProtocolHandler handler = ph[index];
			handler.setShuttingDown(shuttingdown);
			return true;
		}catch(Throwable e){
			LOG.error("setting shutdown through mbean failed", e);
			return false;
		}
		
	}
}
