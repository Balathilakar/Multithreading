package com.amfam.billing.acquirer;

import java.util.Date;
import java.util.List;

import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amfam.billing.acquirer.dataaccess.businesstobusiness.ProtocolServiceException;
import com.amfam.billing.acquirer.dataaccess.businesstobusiness.ProtocolServiceFactoryException;
import com.amfam.billing.acquirer.dataaccess.jdbc.AcquirerJdbcDAO;
import com.amfam.receipt.finacct.CreditCardNumber;

/**
 * This class contains the public methods for the Acquirer Communications component.  This component
 * handles communications with the acquirer for Debit/Credit Card authorizations.
 * 
 * init function used to determine whether to set up connection and use mock
 */
public class Service
{
	private static final Log LOG = LogFactory.getLog( Service.class );
	private static boolean _useDuplicateAuthorizationCache = false;
	private static long _lastFetchOfCacheVar = new Date().getTime();
	private static long TIME_BETWEEN_CACHE_VAR_CHECK = 5L * 60L * 1000L; //five minutes
	private static boolean _firstHitCompleted = false;
	private static AcquirerJdbcDAO dao;
	
	public static void init(boolean initAcquirerCommAtStartup){
		
		dao = getDao();

		useDuplicateAuthorizationCache();
		if (initAcquirerCommAtStartup){
			try {
				ProtocolServiceFactory.getInstance().getProtocolService(new SaratogaAuthRequest());					
			} catch (ProtocolServiceException e) {
				e.printStackTrace();
				LOG.error(e);
			}
		}
	}
	private static AcquirerJdbcDAO getDao(){
		if (dao == null){
			try
			{
				return new AcquirerJdbcDAO();
			}catch (NamingException x){
				throw new ExceptionInInitializerError( x );
			}
		}
		return dao;		
	}
	public static void shutdown() {
		try {
			List<ProtocolService> services = ProtocolServiceFactory.getInstance().getAllProtocolServices();
			for(ProtocolService p: services){
				p.shutdown();
			}
		} catch (ProtocolServiceFactoryException ignore) {
			LOG.error(ignore, ignore);
		}
	}
	
	/**
	 * looks up to see if should be using cache
	 * @return boolean
	 */
	private static boolean useDuplicateAuthorizationCache(){
		if (!isLastFetchOfCacheVarValid()){
			if ( getDao().isCacheConfigSettingOn()){
				LOG.info("Correlation Id cache will be turned on");
				_useDuplicateAuthorizationCache = true;
			}else{
				LOG.info("Correlation Id cache will be turned false");
				_useDuplicateAuthorizationCache = false;				
			}
		}
		return _useDuplicateAuthorizationCache;
	}
	
	/**
	 * check to see if the cache var should be read from the db again
	 * @return
	 */
	private static boolean isLastFetchOfCacheVarValid(){
		
		if (!_firstHitCompleted){
			_firstHitCompleted=true;
			return false;
		}
		
		long now = new Date().getTime();
		if ((_lastFetchOfCacheVar + TIME_BETWEEN_CACHE_VAR_CHECK )<now){
			_lastFetchOfCacheVar=now;
			return false;
		}
		return true;
	}
	
	
	/**
	 * This method sends an authorization request to the acquirer.  The Request parameter must
	 * be populated with all necessary information to make the authorization request.  The
	 * Response parameter is populated with all information received from the acquirer.
	 * 
	 * if using the mock, it calls the mock service
	 * 
	 * @param req The protocol-specific authorization request
	 * @param resp The protocol-specific authorization response
	 * @throws ProtocolServiceException
	 */
	public static final Response authorize( Request req )
		throws ProtocolServiceException
	{
		
		Response response = null;
		AuthorizationCache cache = new AuthorizationCache();
		String correlationId = req.getCorrelationId();
		
		/*
		 * in the case of using the cache, set the resposne to the cache value if found
		 */
		if (useDuplicateAuthorizationCache()){
			//check for duplicate before trying authorization
			response = cache.checkDuplicateCorrelationId(correlationId);
			if (response!=null){
				return response;
			}
		}
		
		//if the response wasn't found, authorize it and save it with the correlation id
		ProtocolService ps = ProtocolServiceFactory.getInstance().getProtocolService(req);
		response = ps.authorize(req);

		/*
		 * in the case of using the cache, set any response in it
		 */
		if (useDuplicateAuthorizationCache()){
			cache.setCorrelationId(correlationId,response);
		}
		
		return response;	
	}
	
	

	/**
	 * This method returns an instance of a protocol-specific Request class.
	 * 
	 * For now, StrautsAuthRequest is the only implemented request class.  In the future,
	 * there may be others based on CreditCardNumber.  For example, if the card is AMEX,
	 * we may return an AmericanExpressRequest if we are authorizing through AmEx.
	 * 
	 * @param c
	 * @return an instance of a protocol-specific Request class
	 */
	public static final Request getRequest(CreditCardNumber c) {
		return new SaratogaAuthRequest();
	}
	
	public static boolean reconnectPrimary(){
		try {
			ProtocolServiceFactory.getInstance().getProtocolService(null).reconnectPrimary();
			return true;
		} catch (ProtocolServiceFactoryException e) {
			LOG.error(e, e);
			return false;
		} catch (ProtocolServiceException e) {
			LOG.error(e, e);
			return false;
		}
	}
	
	public static boolean reconnectSecondary(){
		try {
			ProtocolServiceFactory.getInstance().getProtocolService(null).reconnectSecondary();
			return true;
		} catch (ProtocolServiceFactoryException e) {
			LOG.error(e, e);
			return false;
		} catch (ProtocolServiceException e) {
			LOG.error(e, e);
			return false;
		}
	}
	
	public static boolean makePrimaryActive(){
		try {
			ProtocolServiceFactory.getInstance().getProtocolService(null).makePrimaryActive();
			return true;
		} catch (ProtocolServiceFactoryException e) {
			LOG.error(e, e);
			return false;
		} catch (ProtocolServiceException e) {
			LOG.error(e, e);
			return false;
		}
	}
	
	public static boolean makeSecondaryActive(){
		try {
			ProtocolServiceFactory.getInstance().getProtocolService(null).makeSecondaryActive();
			return true;
		} catch (ProtocolServiceFactoryException e) {
			LOG.error(e, e);
			return false;
		} catch (ProtocolServiceException e) {
			LOG.error(e, e);
			return false;
		}
	}
	
	public static String getActiveServer(){
		try {
			return ProtocolServiceFactory.getInstance().getProtocolService(null).getActiveServer();
		} catch (ProtocolServiceFactoryException e) {
			LOG.error(e, e);
			return "Failed to determine active server: " + e.getMessage();
		} catch (ProtocolServiceException e) {
			LOG.error(e, e);
			return "Failed to determine active server: " + e.getMessage();
		}
	}

}