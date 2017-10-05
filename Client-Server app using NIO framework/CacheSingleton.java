package com.amfam.billing.acquirer;

import java.net.URL;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

/**
 * solve conflicts with ehcache
 * singleton that wraps an ehcache cachemanager instance
 */
public class CacheSingleton {
	private static final String ConfirmationNumberCache = "ConfirmationNumberCache";
	private static CacheSingleton singleton = new CacheSingleton( );
	private static CacheManager cacheManager = null;
	private CacheSingleton(){ }
	   
	public static CacheSingleton getInstance( ) {
		if(cacheManager == null){
			
			URL url = Thread.currentThread().getContextClassLoader().getResource("/authorizationCache.xml");
			cacheManager = new CacheManager(url);
		}
	    return singleton;
	}
	
	protected Cache getCache(){
		return cacheManager.getCache(ConfirmationNumberCache);
	}
}
