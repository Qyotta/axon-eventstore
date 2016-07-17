package de.qyotta.eventstore.utils;

import org.apache.http.client.cache.CacheResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpCacheLoggingUtil {
   private static final Logger LOGGER = LoggerFactory.getLogger(HttpCacheLoggingUtil.class.getName());

   @SuppressWarnings("nls")
   public static void logCacheResponseStatus(String prefix, CacheResponseStatus cacheResponseStatus) {
      switch (cacheResponseStatus) {
         case CACHE_HIT:
            LOGGER.debug(prefix + ":A response was generated from the cache with no requests sent upstream");
            break;
         case CACHE_MODULE_RESPONSE:
            LOGGER.debug(prefix + ":The response was generated directly by the caching module");
            break;
         case CACHE_MISS:
            LOGGER.debug(prefix + ":The response came from an upstream server");
            break;
         case VALIDATED:
            LOGGER.debug(prefix + ":The response was generated from the cache after validating the entry with the origin server");
            break;
         default:
            break;
      }
   }

}
