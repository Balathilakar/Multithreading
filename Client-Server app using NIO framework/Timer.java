package com.amfam.billing.acquirer;

import org.apache.commons.logging.LogFactory;

/**
 * A timer that notifies after a given period of time has elapsed.
 */
public class Timer
	extends Thread
{
	private long duration;
	private long t1;
	private boolean isRunning;
	private Listener listener;
	private boolean isExpired;
	
	public Timer( Listener t, long dt )
	{
		listener = t;
		duration = dt;
	}
	
	/**
	 * This thread runs until duration milliseconds have passed from the 
	 * time of entry to this method or terminate is called.
	 * 
	 * <p>When duration ms have elapsed, the Listener is notified.
	 * 
	 * <p>The Timer thread continues to run until terminate is called.
	 */
	public void run()
	{
		synchronized( this )
		{
			isRunning = true;
		}
		
		arm();
		
		while( isRunning() )
		{
			try
			{
				if( isExpired() )
				{
					Thread.sleep( Long.MAX_VALUE );
				}
				else
				{
					long dt = getTimeoutAt() - System.currentTimeMillis();
					if( dt > 0 )
					{
						Thread.sleep( dt );
					}
					expire();
				}
			}
			catch( InterruptedException x )
			{
			}
		}
		
		synchronized( this )
		{
			isRunning = false;
		}
	}
	
	/**
	 * Test if the timer has expired.
	 * 
	 * @return true if the timer has expired.
	 */
	public synchronized boolean isExpired()
	{
		return isExpired;
	}
	
	/**
	 * Expire the timer and notify the listener.
	 */
	private synchronized void expire()
	{
		LogFactory.getLog( getClass() ).debug( "expire" );
		isExpired = true;
		
		Listener l = getListener();
		if( l != null )
		{
			l.expire();
		}
	}

	/** Arm the time using the current duration.
	 * 
	 * <p>On exit, the timer is setup to expire duration milisecondes from now.
	 */
	public synchronized void arm()
	{
		long now = System.currentTimeMillis();
		long dt = Long.MAX_VALUE - now;
		if( dt > duration )
		{
			dt = duration;
		}
		t1 = now + dt;
		isExpired = false;
		interrupt();
	}
	
	/** Arm the time using the current duration.
	 * 
	 * <p>On exit, the timer is setup to expire duration milisecondes from now.
	 * 
	 * @param duration 
	 */
	public synchronized void arm( long duration )
	{
		this.duration = duration;
		arm();
	}
	
	public synchronized long getTimeoutAt()
	{
		return t1;
	}
	
	/**
	 * Test if the time is running.
	 * 
	 * @return true if terminate has been called or the thread has completed.
	 */
	public synchronized boolean isRunning()
	{
		return isRunning;
	}
	
	/**
	 * Terminate the timer without sending an interrupt to the held thread. 
	 */
	public synchronized void terminate()
	{
		isRunning = false;
		interrupt();
	}

	private synchronized Listener getListener() 
	{
		return listener;
	}

	public synchronized void setListener( Listener listener )
	{
		this.listener = listener;
	}

	public interface Listener
	{
		public void expire();
	}
	
}
