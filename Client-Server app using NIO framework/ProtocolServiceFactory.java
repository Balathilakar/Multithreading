package com.amfam.billing.acquirer;

import java.util.ArrayList;
import java.util.List;

import javax.naming.InitialContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amfam.billing.acquirer.dataaccess.businesstobusiness.ProtocolServiceException;
import com.amfam.billing.acquirer.dataaccess.businesstobusiness.ProtocolServiceFactoryException;

/**
 * The class provides utility services for getting an
 * implementation of ProtocolService.
 * Usage: call the static method
 * <code>getInstance()</code> to obtain a <code>ProtocolServiceFactory
 * </code> object. Then call the <code>getProtocolService</code>
 * method to obtain the correct ProtocolService.
 */
public class ProtocolServiceFactory
{

	private static final Log logger = LogFactory.getLog(ProtocolServiceFactory.class);

	public static final long MAX_CONNECTION_START_TIME = 30000;

	private static ProtocolServiceFactory SINGLETON;

	// one object for each ProtocolService that is available
	//XXX - removed instance var for StratusProtocolService
	//private StratusProtocolService stratusProtocolService;
	//private AmexProtocolService amexProtocolService;
	//XXX - added vars for mock and initAtStartup
	boolean useMock = false;
	//XXX - added List of ProtocolService 
	List<ProtocolService> services = new ArrayList<ProtocolService>();
	
	/**
	 * Default constructor.
	 */
	private ProtocolServiceFactory()
	{
		try {
			InitialContext ctx = new InitialContext();
			String s = (String) ctx.lookup("props/cbsconfig/pymtmgr/useServiceMock");
			// add debug statements
			if (logger.isInfoEnabled())
				logger.info("useServiceMock is " + s.trim());

			useMock = Boolean.parseBoolean(s.trim());
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}
	}
	/**
	 * 
	 * @return An implementation of ProtocolService for the given Request
	 */
	//XXX - changed implementation
	public ProtocolService getProtocolService(Request req) throws ProtocolServiceException
	{
		if(useMock){
			if(services.isEmpty()){
				services.add(new MockProtocolService());
			}
		} else {
		if(services.isEmpty()){
				services.add(SaratogaProtocolService.getInstance());
			}
		}
		return services.get(0);
		
	}
	
	/**
	 * Returns a list of 
	 * @return
	 */
	//XXX - added a way to get all ProtocolServices
	public List<ProtocolService> getAllProtocolServices(){
		return services;
		
	}
	/**
	 * Retrieves an instance of a ProtocolServiceFactory
	 * Creation date: (03/06/2007 3:01:30 PM)
	 * @return ProtocolServiceFactory
	 */
	public synchronized static ProtocolServiceFactory getInstance() throws ProtocolServiceFactoryException
	{
		try
		{
			if (SINGLETON == null)
			{
				SINGLETON = new ProtocolServiceFactory();
			}
		}
		catch (Throwable ex)
		{
			logger.fatal("Failed to create ProtocolServiceFactory", ex);
			SINGLETON = null;
			throw new ProtocolServiceFactoryException("Failed to create ProtocolServiceFactory", ex);
		}
		return SINGLETON;
	}
}
