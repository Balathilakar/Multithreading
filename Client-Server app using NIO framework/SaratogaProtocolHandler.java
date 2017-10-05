package com.amfam.billing.acquirer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.amfam.billing.acquirer.dataaccess.businesstobusiness.AcquirerAuthorizationException;
import com.amfam.billing.acquirer.dataaccess.businesstobusiness.AcquirerConnectionException;
import com.amfam.billing.acquirer.dataaccess.businesstobusiness.MessageRejectedException;
import com.amfam.billing.acquirer.dataaccess.businesstobusiness.TimeoutException;
import com.amfam.billing.acquirer.util.CleanseCreditCardNumber;
import com.amfam.billing.reuse.Monitor;

/**
 * 
 */
public class SaratogaProtocolHandler extends Thread implements ProtocolHandler {
	private static final Log TSYS_REJECT_LOG = LogFactory.getLog( "tsys.reject");
	private static final Log LOG = LogFactory.getLog(SaratogaProtocolHandler.class);
	//To log response mismatch that held funds
	private static final Log LOGFUNDSHOLD = LogFactory.getLog("com.amfam.billing.acquirerHoldsWithFailure");
	private Timer timeoutTimer = new Timer(null, Long.MAX_VALUE);
	protected static final int RESPONSE_MESSAGE_SIZE = 84;
	protected static final int REQUEST_MESSAGE_SIZE = 84;

	private static final long STAT_LOG_INTERVAL = 15L * 60L * 1000L; // 15
	// minutes
	private static final long MIN_RETRY_CONNECT = 60000L;
	private static final long KEEP_ALIVE_FREQUENCY = 7000L;

	protected static final long MAX_RESPONSE_WAIT = 2000L;
	protected static final String KEEP_ALIVE_REQUEST_CODE = "0301";
	protected static final String TSYS_NETWORK_SUCCESS_RESPONSE_CODE = "00003";
	protected static final String KEEP_ALIVE_REQUEST_MESSAGE_TYPE = "0800";
	protected static final String KEEP_ALIVE_RESPONSE_MESSAGE_TYPE = "0810";
	protected static final int RET_REF_FIELD_NUM = 37;
	protected static final int NWK_INF_FIELD_NUM = 70;
	protected static final int RES_IND_FIELD_NUM = 93;

	private volatile boolean isRunning;
	private volatile boolean isShuttingDown;
	
	public boolean isShuttingDown() {
		return isShuttingDown;
	}

	public void setShuttingDown(boolean isShuttingDown) {
		this.isShuttingDown = isShuttingDown;
	}

	private volatile boolean isTerminated;
	private volatile boolean isKeepAliveReady;
	private boolean okToSend;
	private LinkedList<RequestItem> requests = new LinkedList<RequestItem>();
	private RequestItem currentRequest;

	private Stats stats;
	private Timer statTimer;

	private SaratogaRequestBuilder builder = (SaratogaRequestBuilder) getBean("isoBuilder");

	protected int port = 0;
	private String CURRENT_KEEP_ALIVE_ID;
	private long lastRequestSentTime = 0l;

	protected InetSocketAddress address;
	private SocketChannel channel;
	private Selector readSelector;
	private static final int INITIAL_BUF_SIZE = 4096;
	private byte[] buffer = new byte[INITIAL_BUF_SIZE];
	private ByteBuffer inBuffer = ByteBuffer.wrap(buffer);
	private SaratogaProtocolService service;

	protected SaratogaProtocolHandler(InetAddress address, int port, SaratogaProtocolService serviceCallback) {
		this(new InetSocketAddress(address, port), serviceCallback);
	}

	protected SaratogaProtocolHandler(InetSocketAddress address, SaratogaProtocolService serviceCallback) {
		this.address = address;
		statTimer = new Timer(null, STAT_LOG_INTERVAL);
		createStats();
		timeoutTimer.start();
		statTimer.setListener(stats);
		statTimer.start();
		this.service = serviceCallback;
	}

	public void run() {

		LOG.info("enter: run()" + Thread.currentThread().getName());

		isRunning = true;
		isTerminated = false;
		isKeepAliveReady = true;
		isShuttingDown = false;

		while (isRunning) {
			try {

				acquireConnection();

				if (okToSend && haveNextRequest()) {
					sendAuthMessage();
				} else {
					sendKeepAlive();
				}

				/*
				 * look at registered selection keys
				 */
				if (readSelector != null) {
					readSelector.select(1000);
					Iterator<SelectionKey> keys = readSelector.selectedKeys().iterator();
					while (keys.hasNext()) {
						SelectionKey key = keys.next();
						keys.remove();
						if (!key.isValid())
							continue;

						if (key.isReadable()) {
							SaratogaAuthResponse response = receiveResponse();
							handleResponse(response);
						}
					}
				}
			} 
			//Not re-throw any exception or error.Because we don't want to close any channel in this case.
			catch(AcquirerAuthorizationException ae){
				LOG.error("Request and response Correlation ID's did not match. Please look at the acquirerHoldswithFailure.log");
			}
			catch (Throwable x) {
				LOG.warn("exception detected in main loop. cleaning up thread", x);
				close();
				notifyNotSent();
				isShuttingDown = false;
				if (currentRequest != null) {
					currentRequest.release();
					currentRequest = null;
					okToSend = true;
					if (LOG.isDebugEnabled()) {
						LOG.debug("in catch of run() okToSend set to true");
					}
				}
			}
		}
		close();
		notifyNotSent();
		cleanup();

		isTerminated = true;
		isShuttingDown = false;
		LOG.info(getStats().toString());
		LOG.info("exit: run()");
	}

	/**
	 * check to see if there is an active connection it must not be shutting
	 * down and be an open channel
	 */
	private void acquireConnection() {
		while (allowChannelConnection()) {
			try {
				openChannel();
				okToSend = true;

				if (LOG.isDebugEnabled()) {
					LOG.debug("in while loop okToSend set to true");
				}
			} catch (Throwable x) {
				close();
				notifyNotSent();
				try {
					Thread.sleep(MIN_RETRY_CONNECT);
				} catch (InterruptedException y) {
				}
			}
			if (LOG.isTraceEnabled()) {
				LOG.trace("bottom of while loop okToSend = " + okToSend);
			}
		}
	}

	/**
	 * parse the response
	 * 
	 * @param response
	 * @throws AcquirerAuthorizationException
	 * @throws AcquirerConnectionException
	 */
	private void handleResponse(SaratogaAuthResponse response) throws AcquirerAuthorizationException, AcquirerConnectionException {
		if (response != null) {
			if (response.getRequestType().equals(RequestType.AUTH)) {
				handleAuthResponse(response);

			} else if (response.getRequestType().equals(RequestType.KEEP_ALIVE)) {

				if (response != null && !response.getDatafields().isEmpty()) {
					stats.keepAliveResponseReceived();
					if (response.getDatafields().get(new Integer(RES_IND_FIELD_NUM)) == null
							&& KEEP_ALIVE_REQUEST_CODE.equals((String) response.getDatafields().get(new Integer(NWK_INF_FIELD_NUM)))) {
						SaratogaMessageParser parser = new SaratogaMessageParser();
						String requestMessage = response.getResponseText().replaceFirst(KEEP_ALIVE_REQUEST_MESSAGE_TYPE, KEEP_ALIVE_RESPONSE_MESSAGE_TYPE);
						parser.parse(requestMessage);
						sendMessage(requestMessage);
					}
					/**
					 * Added to check the response code for keep alive. If the
					 * keep alive response code is not 0003 throwing Exception
					 */
					else if (!TSYS_NETWORK_SUCCESS_RESPONSE_CODE.equalsIgnoreCase((String) response.getDatafields().get(new Integer(RES_IND_FIELD_NUM)))) {
						LOG.error("Keep Alive Response / Response Code is Invalid");
						throw new AcquirerConnectionException("Keep Alive Failed");
					}
					if (CURRENT_KEEP_ALIVE_ID != null && CURRENT_KEEP_ALIVE_ID.equalsIgnoreCase((String) response.getDatafields().get(new Integer(RET_REF_FIELD_NUM)))) {
						isKeepAliveReady = true;
					}
				}
			} else if (response.getRequestType().equals(RequestType.SOFT_SHUT)) {
				LOG.error("not an error.. we've received a SOFT SHUT from tsys");
				isShuttingDown = true; // guard against any more use of this handler
				stats.softShutReceived();
				this.service.transferRequests(this);
			}else{
				/*
				 * just to make sure we have information if the request type was not known
				 */
				LOG.error("Unknown request type!");
				LOG.error("request type: " + response.getRequestType() + " correlation id: " + response.getCorrelationId());
			}

		}
	}

	private void handleAuthResponse(SaratogaAuthResponse response) throws AcquirerAuthorizationException {
		if(response.getRejectCode()!=null){
            TSYS_REJECT_LOG.error("Reject Code:"+response.getRejectCode());
            TSYS_REJECT_LOG.error("Cleansed reject Message:"+CleanseCreditCardNumber.replaceCcNumber(response.getRawResponse(), 
                            CleanseCreditCardNumber.findCCNumber(response.getRawResponse())));
            currentRequest.notifyError(new MessageRejectedException("TSYS Rejectd the request Message"));
    }else{
		response.mapSaratogaResponse();
		notifyResponse(response);
	}
}
	/**
	 * send a keep alive message Note the previous request send time. and make
	 * sure the current request time stamp crosses over the frequency of keep
	 * alive
	 */
	private void sendKeepAlive() {
		long duriationbwRequest = System.currentTimeMillis() - lastRequestSentTime;
		LOG.trace("time duration:" + duriationbwRequest);
		if (duriationbwRequest >= KEEP_ALIVE_FREQUENCY) {
			lastRequestSentTime = System.currentTimeMillis();
			LOG.trace("duration request time greater than keep alive frequency");
			if (isKeepAliveReady) {
				LOG.trace("isKeepAliveReady is true");
				Keepalive();
				stats.keepAliveRequestSent();
				isKeepAliveReady = false;
			}
		}
	}

	private void sendAuthMessage() {
		try {
			lastRequestSentTime = System.currentTimeMillis();
			send();
		} catch (Throwable x) {
			LOG.warn("exception detected. Reconnecting socket", x);
			currentRequest.notifyError(x);
			removeCurrentRequest();
			if (currentRequest != null) {
				currentRequest.release();
				currentRequest = null;
				okToSend = true;
				if (LOG.isDebugEnabled()) {
					LOG.debug("in catch of run() okToSend set to true");
				}
			}
		}
	}

	/**
	 * Set the interval at which connection statistics are logged.
	 * 
	 * @param dt
	 *            The interval in milliseconds.
	 */
	public void setStatLogInterval(long dt) {
		statTimer.arm(dt);
	}

	/**
	 * Clears all requests that have not yet been transmitted from the list and
	 * notifies the callers.
	 */
	private void notifyNotSent() {
		synchronized (requests) {
			for (Iterator<RequestItem> i = requests.iterator(); i.hasNext();) {
				RequestItem item = (RequestItem) i.next();
				i.remove();
				item.notifyNotSent();
			}
		}
	}

	/**
	 * Clears all requests that have not yet been transmitted from the list and
	 * notifies the callers.
	 */
	private void removeCurrentRequest() {
		synchronized (requests) {
			for (Iterator<RequestItem> i = requests.iterator(); i.hasNext();) {
				RequestItem item = (RequestItem) i.next();
				if (currentRequest.equals(item)) {
					i.remove();
					break;
				}
			}
		}
	}

	/**
	 * Terminate the thread.
	 */
	protected synchronized void terminate() {
		unregisterMbean();
		notifyNotSent();
		isRunning = false;
		isTerminated = true;
	}

	/**
	 * unregister the mbean if found
	 */
	private void unregisterMbean() {
		ObjectName oname = null;
		try {
			oname = new ObjectName("com.amfam.billing.acquirer:type=Stats,IP=" + address.getHostName() + ",port=" + address.getPort());
		} catch (MalformedObjectNameException e) {
			LOG.error("malformed " + oname.getCanonicalName(), e);
		} catch (NullPointerException e) {
			LOG.error("Null pointer", e);
		}

		// unregister of mbeans
		if (Monitor.getInstance().getMBeanServer().isRegistered(oname)) {
			try {
				Monitor.getInstance().getMBeanServer().unregisterMBean(oname);
			} catch (MBeanRegistrationException e) {
				LOG.error("Registration exception " + oname.getCanonicalName(), e);
			} catch (InstanceNotFoundException e) {
				LOG.error("Instance not found " + oname.getCanonicalName(), e);
			}
		}
	}

	/**
	 * Test if the connection thread running.
	 * 
	 * @return true if the component is running otherwise false.
	 */
	protected synchronized boolean isRunning() {
		return isRunning;
	}

	/**
	 * Test if the connection thread has finished terminating.
	 * 
	 * @return true if the component has finished terminating otherwise false.
	 */
	protected synchronized boolean isTerminated() {
		return isTerminated;
	}

	/**
	 * Test if the connection thread is available for sending messages.
	 * 
	 * @return true if the component is available otherwise false.
	 */
	protected synchronized boolean isAvailable() {

		LOG.trace("isRunning " + Thread.currentThread().getName() + " : " + isRunning);
		if (isShuttingDown) {
			LOG.error("isShuttingDown! " + Thread.currentThread().getName() + " : " + isShuttingDown);
		} else {
			LOG.trace("isShuttingDown " + Thread.currentThread().getName() + " : " + isShuttingDown);
		}

		if (channel == null) {
			LOG.trace("channel is null " + Thread.currentThread().getName());
		} else {
			LOG.trace("channel is open " + Thread.currentThread().getName() + " : " + channel.isOpen());
		}

		return !isShuttingDown && isRunning && isChannelAvailable();
	}

	protected synchronized boolean allowChannelConnection() {
		return !isShuttingDown && !isChannelAvailable();
	}

	protected synchronized boolean isChannelAvailable() {
		return channel != null && channel.isOpen();
	}

	/**
	 * @return
	 */
	LinkedList<RequestItem> getRequests() {
		return requests;
	}

	/**
	 * Retrieve the connection statistics.
	 * 
	 * @return The statistics.
	 */
	public Stats getStats() {
		return stats;
	}

	private void createStats() {
		stats = new Stats(this, statTimer);
		ObjectName oname = null;
		try {
			oname = new ObjectName("com.amfam.billing.acquirer:type=Stats,IP=" + address.getHostName() + ",port=" + address.getPort());
			if (Monitor.getInstance().getMBeanServer().isRegistered(oname)) {
				Monitor.getInstance().getMBeanServer().unregisterMBean(oname);
			}

			Monitor.getInstance().getMBeanServer().registerMBean(stats, oname);
		} catch (MalformedObjectNameException e) {
			throw new AssertionError("invalid object name");
		} catch (InstanceAlreadyExistsException e) {
			LOG.error("Failed to register Stats " + oname.getCanonicalName(), e);
		} catch (MBeanRegistrationException e) {
			LOG.error("Failed to register Stats " + oname.getCanonicalName(), e);
		} catch (NotCompliantMBeanException e) {
			AssertionError ae = new AssertionError("invalid MBean");
			ae.initCause(e);
			throw ae;
		} catch (InstanceNotFoundException e) {
			LOG.error("InstanceNotFoundException Not found" + oname.getCanonicalName(), e);
		}
	}

	/**
	 * Send a message.
	 * 
	 * <p>
	 * The request is wrapped in a RequestItem and
	 * {@link #addRequest(RequestItem ) added} to the pending request list.
	 * 
	 * @param request
	 *            The message to send.
	 * @param listener
	 *            The listener to notify on request completion.
	 * 
	 * @throws IllegalStateException
	 *             if the connection is not running.
	 */
	public synchronized void send(Message req, ProtocolHandler.Listener listener) {
		stats.startMessage();
		if (req instanceof SaratogaMessage) {
			addRequest(new RequestItem((SaratogaMessage) req, listener));
		} else {
			throw new IllegalArgumentException("Message must be a SaratogaMessage");
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("in send(Message, Listener) going to wakeup()");
		}
	}

	/**
	 * Add a request to the queue.
	 * 
	 * @param request
	 *            The request to add to the queue.
	 */
	protected void addRequest(RequestItem request) {
		synchronized (requests) {
			requests.add(request);
		}
	}

	/**
	 * Add a request to the queue.
	 * 
	 * @param request
	 *            The request to add to the queue.
	 */
	protected void receiveTransferredRequest(RequestItem request) {
		synchronized (requests) {
			LOG.trace("received transferred request");
			requests.add(request);
		}
	}

	protected LinkedList<RequestItem> transferRequests() {

		synchronized (requests) {
			LinkedList<RequestItem> localrequests = new LinkedList<RequestItem>();
			localrequests.addAll(this.requests);

			Iterator iter = this.requests.iterator();
			while (iter.hasNext()) {
				iter.next();
				iter.remove();
			}
			return localrequests;
		}
	}

	/**
	 * Set the current request to the oldest request in the request queue.
	 * 
	 * <p>
	 * On exit, currentRequest holds the current request to process or null if
	 * no requests are outstanding. The current request is removed from the
	 * queue.
	 * 
	 * @return true if a request to process is available in currentRequest.
	 */
	private boolean haveNextRequest() {
		boolean haveNextRequest = false;

		synchronized (requests) {
			LOG.trace("in haveNextRequest() requests.size() = " + requests.size());
			if (haveNextRequest = requests.size() > 0) {
				currentRequest = (RequestItem) requests.removeFirst();
				currentRequest.attachTimer(timeoutTimer);
				okToSend = false;
				if (LOG.isDebugEnabled()) {
					LOG.debug("in haveNextRequest() okToSend set to false");
				}
			}
		}

		return haveNextRequest;
	}

	/**
	 * Send the current request.
	 * 
	 * <p>
	 * If the request has already timed out the listener is notified. Otherwise
	 * the request is sent. It is assumed that currentRequest is set properly.
	 * 
	 * @throws IOException
	 *             If the request data is not successfully written on the
	 *             socket.
	 */
	private void send() throws Throwable {
		if (LOG.isDebugEnabled()) {
			LOG.debug("in SaratogaProtocolHandler send()");
		}

		if (currentRequest.isTooLateToSend()) {
			stats.sendTimeout();
			currentRequest.notifyNotSent();
			currentRequest.release();
			currentRequest = null;
			okToSend = true;
			if (LOG.isDebugEnabled()) {
				LOG.debug("in isTooLateToSend okToSend set to true");
			}
		} else {
			try {
				currentRequest.setTimeoutAt();
				doAuth((SaratogaAuthRequest) currentRequest.getMessage());
			} catch (Throwable x) {
				stats.sendError();
				LOG.error("Failed to send request", x);
				currentRequest.notifyError(x);
				currentRequest.release();
				currentRequest = null;
				okToSend = true;
				if (LOG.isDebugEnabled()) {
					LOG.debug("in catch of send() okToSend set to true");
				}
				throw x;
			} finally {
				stats.messageSent();
			}
		}
	}

	/**
	 */
	private void doNotify() {
		// check stats for number of reconnects and how often occurring
		// if exceeded our threshold, send notify
	}

	/**
	 * Stop the timeout Timer and the stat logging Timer.
	 * 
	 * <p>
	 * This method should be called during final cleanup of the Connection.
	 */
	protected void cleanup() {

		if (timeoutTimer != null) {
			timeoutTimer.terminate();
		}
		if (statTimer != null) {
			statTimer.terminate();
		}
	}

	/**
	 * Insure that resources are released.
	 */
	protected void finalize() throws Throwable {
		cleanup();
	}

	/**
	 * Test that currentRequest and response IDs match.
	 * 
	 * @param response
	 *            The response to check.
	 * 
	 * @throws AcquirerAuthorizationException
	 *             if the sequence numbers do not match.
	 */
	private void checkId(Response response) throws AcquirerAuthorizationException {
		
		try{
			LOG.debug("current request nullcheck: " + (currentRequest == null));
			LOG.debug("currentRequest.getMessage() nullcheck: " + (currentRequest.getMessage() == null));
			LOG.debug("currentRequest.getMessage().getCorrelationId nullcheck: " + (currentRequest.getMessage().getCorrelationId() == null));
			LOG.debug("currentRequest.getMessage().getCorrelationId nullcheck: " + (currentRequest.getMessage().getCorrelationId() == null));
			LOG.debug("currentRequest.getMessage().getCorrelationId: " + currentRequest.getMessage().getCorrelationId());
		}catch(Throwable e){
			LOG.error("check currentrequest", e);
		}

		try{
			LOG.debug("current request response: " + (response == null));
			LOG.debug("current Request nullcheck: " + (response.getCorrelationId() == null));
			LOG.debug("response correlation nullcheck: " + response.getCorrelationId());
		}catch(Throwable e){
			LOG.error("check currentrequest response", e);
		}

		
		if (!currentRequest.getMessage().getCorrelationId().equals(response.getCorrelationId())) {
			LOGFUNDSHOLD.error("Request and response Correlation ID's did not match: Request Correlation ID:"+ 
					currentRequest.getMessage().getCorrelationId() + " Response correlation ID:" + response.getCorrelationId()
					+" Confirmation number:" +currentRequest.getMessage().getConfirmationNumber()+" Amount:"+currentRequest.getMessage().getAmount());
			throw new AcquirerAuthorizationException(AcquirerAuthorizationException.CC_MERCHANT_ID_ERROR_CODE);
		}
	}

	/**
	 * Read the response for the currentRequest.
	 * 
	 * <p>
	 * The response is read. currentRequest is reset to null.
	 * 
	 * @return The response.
	 * @throws AcquirerAuthorizationException
	 * 
	 * @throws IOException
	 *             if the response cannot be read or properly parsed.
	 * @throws TimeoutException
	 * @throws AcquirerAuthorizationException
	 *             if the sequence number is incorrect.
	 */
	private void notifyResponse(SaratogaAuthResponse response) throws AcquirerAuthorizationException {

		checkId(response);
		stats.responseReceived(currentRequest.xmitAt);
		currentRequest.notifyResponse(response);

	}

	public SaratogaAuthResponse receiveResponse() throws Throwable {

		try {

			inBuffer.clear();
			byte[] b = new byte[256];
			ensureCapacity(inBuffer.position() + b.length);
			inBuffer.limit(inBuffer.position() + b.length);
			int count = 0;
			long initialTimeOfMsgSent = System.currentTimeMillis();
			while (true) {
				long currentTime = System.currentTimeMillis() - initialTimeOfMsgSent;
				if (count > 2 && currentTime > MAX_RESPONSE_WAIT) {
					throw new TimeoutException("Exceeded the request waiting time");
				}
				int size = channel.read(inBuffer);
				LOG.trace("inBuffer size: " + size);
				if (size > 0) {
					inBuffer.flip();
					inBuffer.get(b, 0, size);
					int length = getMessageLengthInDecimal(b[1]);
					String responseMessage = getResponseInASCII(b, length);
				
					LOG.debug("Message Received from TSYS:"
							+ CleanseCreditCardNumber.replaceCcNumber(responseMessage, CleanseCreditCardNumber.findCCNumber(responseMessage)));
					SaratogaMessageParser parser = new SaratogaMessageParser();
					SaratogaAuthResponse response = parser.parse(responseMessage);

					return response;
				}
				count++;
			}

		} catch (TimeoutException te) {
			stats.receiveTimeout();
			if (currentRequest != null) {
				currentRequest.notifyTimeout();
			}
			throw te;
		} catch (Throwable x) {
			stats.badMessage();
			// keep alive and sign-on does not go to request queue. So need to
			// have null check.
			if (currentRequest != null)
				currentRequest.notifyError(x);
			throw x;
		} finally {
			okToSend = true;
			LOG.trace("in finally of readResponse() okToSend set to true");
		}
	}

	public void sendMessage(String requestMessage) {

		if (channel != null && channel.isOpen()) {

			try {
				LOG.trace("In sendMessage");
				requestMessage = convertHexToAscii(requestMessage);
				byte[] b = new byte[requestMessage.length()];

				ByteBuffer byteBuffer = ByteBuffer.allocate(requestMessage.length());
				byteBuffer.clear();
				int len = requestMessage.length();
				for (int i = 0; i < len; i++) {
					b[i] = (byte) requestMessage.charAt(i);
					byteBuffer.put(b[i]);
				}
				byteBuffer.flip();

				channel.write(byteBuffer); // message body

			} catch (Throwable x)

			{
				stats.sendError();
				LOG.error("Failed to send request", x);
				currentRequest.notifyError(x);
				currentRequest.release();
				currentRequest = null;
				okToSend = true;
				if (LOG.isDebugEnabled()) {
					LOG.debug("in catch of send() okToSend set to true");
				}

				throw new RuntimeException(x);

			}

		}

	}

	private static String convertHexToAscii(String hex) {
		StringBuilder output = new StringBuilder();
		for (int i = 0; i < hex.length(); i += 2) {
			String str = "";
			if (i + 2 <= hex.length()) {
				str = hex.substring(i, i + 2);
			} else {
				str = hex.substring(i, i + 1);
			}
			output.append((char) Integer.parseInt(str, 16));
		}
		return output.toString();
	}

	public boolean signOn() {
		LOG.info("SIGN ON Message");
		String requestMessage = builder.getMessagePayloadForSignOn();
		LOG.info("Sign-on request message:" + requestMessage);
		SaratogaMessageParser parser = new SaratogaMessageParser();
		parser.parse(requestMessage);
		sendMessage(requestMessage);
		SaratogaAuthResponse response = null;
		try {
			response = receiveResponse();
			if (response != null && response.getDatafields() != null && !response.getDatafields().isEmpty()
					&& TSYS_NETWORK_SUCCESS_RESPONSE_CODE.equalsIgnoreCase((String) response.getDatafields().get(new Integer(RES_IND_FIELD_NUM)))) {
				return true;
			}
		} catch (Throwable e) {
			LOG.error("Sign-on receive response Failed", e);
		}

		return false;
	}

	private static Object getBean(String beanName) {
		ApplicationContext context = new ClassPathXmlApplicationContext("com/amfam/billing/acquirer/acquirerConfig.xml");
		Object bean = context.getBean(beanName);
		return bean;
	}

	public void Keepalive() {
		LOG.debug("KEEP ALIVE Message");
		String requestMessage = builder.getMessagePayloadForKeepAlive();
		LOG.trace("KeepAlive request Message:" + requestMessage);
		SaratogaMessageParser parser = new SaratogaMessageParser();
		SaratogaResponse message = parser.parse(requestMessage);
		CURRENT_KEEP_ALIVE_ID = (String) message.getDatafields().get(new Integer(RET_REF_FIELD_NUM));
		sendMessage(requestMessage);
	}

	public void doAuth(SaratogaAuthRequest authRequest) {
		LOG.debug("AUTHORIZATION Message");
		String requestMessage = builder.getMessagePayloadForAUTH(authRequest);
		LOG.trace("AuthRequestMessage:" + CleanseCreditCardNumber.replaceCcNumber(requestMessage, CleanseCreditCardNumber.findCCNumber(requestMessage)));
		SaratogaMessageParser parser = new SaratogaMessageParser();
		parser.parse(requestMessage);
		sendMessage(requestMessage);
	}

	private static String getResponseInASCII(byte[] ascii, int length) {

		RawResponseMessage responseMessage = new RawResponseMessage();

		StringBuffer hexstrBuff;
		byte[] bytesMessage;

		try {
			hexstrBuff = new StringBuffer();
			bytesMessage = new byte[4];
			for (int i = 0; i < 4; i++) {
				bytesMessage[i] = ascii[i];
				char c = (char) ascii[i];
				String hexstr = Integer.toHexString(c);
				if (hexstr.length() < 2) {
					hexstr = "0" + hexstr;
				} else if (hexstr.length() > 2) {
					hexstr = hexstr.substring(hexstr.length() - 2, hexstr.length());
				}
				hexstrBuff.append(hexstr);
			}
			responseMessage.setFirst4ByteMessage(hexstrBuff.toString());
			responseMessage.setFirst4RawBytes(bytesMessage);

			int headerLength = getMessageLengthInDecimal(ascii[4]);
			responseMessage.setHeaderLength(headerLength);

			hexstrBuff = new StringBuffer();
			bytesMessage = new byte[headerLength];
			for (int i = 4; i < headerLength + 4; i++) {
				bytesMessage[i - 4] = ascii[i];
				char c = (char) ascii[i];
				String hexstr = Integer.toHexString(c);
				if (hexstr.length() < 2) {
					hexstr = "0" + hexstr;
				} else if (hexstr.length() > 2) {
					hexstr = hexstr.substring(hexstr.length() - 2, hexstr.length());
				}
				hexstrBuff.append(hexstr);
			}
			responseMessage.setHeaderString(hexstrBuff.toString());
			responseMessage.setHeaderRawBytes(bytesMessage);

			hexstrBuff = new StringBuffer();
			bytesMessage = new byte[length - headerLength];
			for (int i = headerLength + 4; i < length + 4; i++) {

				char c = (char) ascii[i];
				String hexstr = Integer.toHexString(c);
				bytesMessage[i - (headerLength + 4)] = ascii[i];
				if (hexstr.length() < 2) {
					hexstr = "0" + hexstr;
				} else if (hexstr.length() > 2) {
					hexstr = hexstr.substring(hexstr.length() - 2, hexstr.length());
				}
				hexstrBuff.append(hexstr);
			}
			responseMessage.setMessageBody(hexstrBuff.toString());
			responseMessage.setMessageType(hexstrBuff.toString().substring(0, 4));
			responseMessage.setMessageBodyRawBytes(bytesMessage);

			responseMessage.setFullMessageRawBytes(ascii);

			return responseMessage.getFullMessage();
		} catch (NumberFormatException e) {
			LOG.error("Response Convertion to ASCII Failed:", e);
		}
		return null;
	}

	private static int getMessageLengthInDecimal(byte b) {
		char c = (char) b;
		String hexstr = Integer.toHexString(c);
		if (hexstr.length() < 2) {
			hexstr = "0" + hexstr;
		} else if (hexstr.length() > 2) {
			hexstr = hexstr.substring(hexstr.length() - 2, hexstr.length());
		}

		int temp1 = Integer.parseInt(hexstr, 16);

		return temp1;
	}

	/**
	 * Open a channel to Paymentech.
	 * 
	 * <p>
	 * The channel is opened and connected. This method will wait for up to 60
	 * seconds to complete the connection. If the connection does not complete,
	 * an exception is thrown. The channel will be null if the connection is not
	 * completed. A selector for read operation on the channel is also set up.
	 * 
	 * @throws IOException
	 */
	private void openChannel() throws IOException {
		if (!isChannelAvailable()) {
			stats.reconnect();
			try {
				LOG.info("openSocket: connecting to" + "\n\taddress: " + address.toString());

				if (readSelector == null) {
					readSelector = Selector.open();
				}

				channel = SocketChannel.open(address);

				int i = 600;
				while (!channel.finishConnect() && (i-- > 0)) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException x) {
					}
				}

				if (!channel.finishConnect()) {
					LOG.info("failed to connect");
					throw new IOException("failed to connect");
				}

				LOG.info("connected");

				channel.configureBlocking(false);
				channel.register(readSelector, SelectionKey.OP_READ);
				if (!signOn()) {
					Thread.sleep(MAX_RESPONSE_WAIT);
					if (!signOn()) {
						throw new RuntimeException("Second sign-on attempt failed");
					}
				}

			} catch (Throwable x) {
				LOG.error("openChannel failed - " + Thread.currentThread().getName(), x);
				channel = null;
				readSelector = null;
				stats.connectFailure();
				throw new RuntimeException(x);
			}
		}

	}

	/**
	 * Ensure that the buffer is large enough to hold n bytes.
	 * 
	 * <p>
	 * The buffer will be expanded if needed.
	 * 
	 * @param n
	 *            The number of bytes necessary.
	 */
	private void ensureCapacity(final int n) {
		if (buffer.length < n) {
			byte[] b = new byte[n];
			System.arraycopy(buffer, 0, b, 0, buffer.length);
			buffer = b;
			inBuffer = ByteBuffer.wrap(buffer);
		}
	}

	/**
	 * Close the comm channel.
	 */
	protected synchronized void close() {
		if (channel != null) {
			try {
				channel.socket().shutdownOutput();
				channel.socket().shutdownInput();
				channel.socket().close();
				channel.close();
			} catch (IOException x) {
				LOG.error("Failed to properly close Socket", x);
			}

			channel = null;
			readSelector = null;
		}

		doNotify();
	}
}
