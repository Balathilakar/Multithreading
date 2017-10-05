package com.amfam.billing.acquirer;

/**
 * Internal class used to hold state for a single request.
 * 
 */
public class RequestItem implements Timer.Listener {
	public static final long MAX_WAIT_TIME = 60000L;
	SaratogaMessage message;
	long sentAt;
	long xmitAt;
	private ProtocolHandler.Listener listener;
	boolean isTimedOut = false;
	private Timer timeoutTimer;

	RequestItem(SaratogaMessage message, ProtocolHandler.Listener listener) {
		this.message = message;
		this.listener = listener;
		isTimedOut = false;
		sentAt = System.currentTimeMillis();
	}
	public void attachTimer(Timer timer){
		this.timeoutTimer = timer;
	}
	public void setTransmitAt() {
		xmitAt = System.currentTimeMillis();
	}

	public void release() {
		timeoutTimer.setListener(null);
		timeoutTimer.arm(Long.MAX_VALUE);
	}

	public synchronized void expire() {
		isTimedOut = true;
	}

	boolean isTimedOut() {
		return isTimedOut;
	}

	boolean isTooLateToSend() {
		return System.currentTimeMillis() - sentAt > MAX_WAIT_TIME;
	}

	void setTimeoutAt() {
		setTransmitAt();
		isTimedOut = false;
		timeoutTimer.setListener(this);
		timeoutTimer.arm(MAX_WAIT_TIME);
	}

	SaratogaMessage getMessage() {
		return message;
	}

	void notifyError(Throwable x) {
		if (listener != null) {
			listener.error(message, x);
		}
	}

	void notifyTimeout() {
		if (listener != null) {
			listener.timeout(message);
		}
	}

	void notifyNotSent() {
		if (listener != null) {
			listener.notSent(message);
		}
	}

	void notifyResponse(Response r) {
		if (listener != null) {
			listener.response(message, r);
		}
	}
}
