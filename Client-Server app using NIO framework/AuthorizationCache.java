package com.amfam.billing.acquirer;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amfam.billing.reuse.util.Strings;

/**
 * @author bxr043
 * class that keeps a list of previous authorizations
 */
public class AuthorizationCache {
	private final Log LOG = LogFactory.getLog( AuthorizationCache.class );
	/**
	 * if the correlation id is found, it returns the response that was saved with it
	 * @param correlationId
	 * @return Response or null
	 */
	public Response checkDuplicateCorrelationId(String correlationId){
		//Cache cache = CacheManager.getInstance().getCache("ConfirmationNumberCache");
		Cache cache = CacheSingleton.getInstance().getCache();
		
		Response response = null;
		char c[] = {' ','.'};
		if (cache.get(correlationId)!=null){
			LOG.info("pulling from cache: " + Strings.stripNonAlphaNumericAndAllowCharset(correlationId,c));
			Element element = cache.get(correlationId);
			response = (Response)element.getValue();
		}
		return response;
	}
	
	/**
	 * puts the item in the cache
	 * @param correlationId
	 * @param response
	 */
	public void setCorrelationId(String correlationId, Response response){
		//Cache cache = CacheManager.getInstance().getCache("ConfirmationNumberCache");
		Cache cache = CacheSingleton.getInstance().getCache();
		if(response.isSuccessful()){   // PBR 1853 -For declines/failures, this fix would allow PM to re-use the correlationId.
			LOG.debug("setting in AuthorizationCache: " + correlationId);
			cache.put(new Element(correlationId, response));				
		}else{
			LOG.debug("Bad Response - skiping to set in AuthorizationCache : " + correlationId);
		}
	}	
}
