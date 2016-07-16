package de.qyotta.eventstore.utils;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.impl.client.cache.ehcache.EhcacheHttpCacheStorage;
import org.apache.log4j.Logger;

import de.qyotta.eventstore.EventStoreSettings;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfiguration.TransactionalMode;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration.Strategy;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy.MemoryStoreEvictionPolicyEnum;

@SuppressWarnings("nls")
public class HttpClientFactory {
   private static final Logger LOGGER = Logger.getLogger(HttpClientFactory.class.getName());

   private static final String HTTP_CLIENT_CACHE = "httpClientCache";

   public static CloseableHttpClient httpClient(final EventStoreSettings settings) {
      if (settings.isCacheResponses()) {
         return newClosableCachingHttpClient(settings);
      }
      return newClosableHttpClient(settings);
   }

   private static CloseableHttpClient newClosableHttpClient(EventStoreSettings settings) {
      final CloseableHttpClient build = HttpClientBuilder.create()

            .setDefaultRequestConfig(requestConfig(settings))
            .setDefaultCredentialsProvider(credentialsProvider(settings))
            .setRedirectStrategy(new LaxRedirectStrategy())
            .setRetryHandler(new StandardHttpRequestRetryHandler())
            .setKeepAliveStrategy(new de.qyotta.eventstore.utils.DefaultConnectionKeepAliveStrategy())
            .setConnectionManagerShared(true)

            .build();

      return build;
   }

   private static CloseableHttpClient newClosableCachingHttpClient(EventStoreSettings settings) {
      final CacheConfig cacheConfig = CacheConfig.custom()
            .build();
      final Cache cache = cacheManager(settings).getCache(HTTP_CLIENT_CACHE);
      if (cache == null) {
         throw new RuntimeException("'cache' is null. Invalid cache configuration!");
      }
      final EhcacheHttpCacheStorage ehcacheHttpCacheStorage = new EhcacheHttpCacheStorage(cache, cacheConfig);
      return CachingHttpClientBuilder.create()
            .setHttpCacheStorage(ehcacheHttpCacheStorage)
            .setCacheConfig(cacheConfig)

            .setDefaultRequestConfig(requestConfig(settings))
            .setDefaultCredentialsProvider(credentialsProvider(settings))
            .setRedirectStrategy(new LaxRedirectStrategy())
            .setRetryHandler(new StandardHttpRequestRetryHandler())
            .setKeepAliveStrategy(new de.qyotta.eventstore.utils.DefaultConnectionKeepAliveStrategy())
            .setConnectionManagerShared(true)

            .build();
   }

   private static RequestConfig requestConfig(final EventStoreSettings settings) {
      return RequestConfig.custom()
            .setConnectTimeout(settings.getConnectionTimeoutMillis())
            .setSocketTimeout(settings.getSocketTimeoutMillis())
            .build();
   }

   private static CredentialsProvider credentialsProvider(final EventStoreSettings settings) {
      final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(new AuthScope(null, -1), new UsernamePasswordCredentials(settings.getUserName(), settings.getPassword()));
      return credentialsProvider;
   }

   private static CacheManager cacheManager(final EventStoreSettings settings) {
      final DiskStoreConfiguration diskStoreConfiguration = new DiskStoreConfiguration();
      if (settings.getCacheDirectory() != null) {
         diskStoreConfiguration.setPath(settings.getCacheDirectory());
      } else {
         final String path = System.getProperty("java.io.tmpdir");
         diskStoreConfiguration.setPath(path);
      }
      // Already created a configuration object ...

      final PersistenceConfiguration persistenceConfiguration = new PersistenceConfiguration();
      persistenceConfiguration.setStrategy(Strategy.LOCALTEMPSWAP.name());

      final CacheConfiguration cacheConfiguration = new CacheConfiguration();
      cacheConfiguration.setName(HTTP_CLIENT_CACHE);
      cacheConfiguration.setMemoryStoreEvictionPolicy(MemoryStoreEvictionPolicyEnum.LRU.name());
      cacheConfiguration.setMaxEntriesLocalHeap(1000);
      cacheConfiguration.setMaxBytesLocalDisk(524288000L);
      cacheConfiguration.setEternal(true); // elements never expire
      cacheConfiguration.setTransactionalMode(TransactionalMode.OFF.name());
      cacheConfiguration.persistence(persistenceConfiguration);

      final net.sf.ehcache.config.Configuration config = new net.sf.ehcache.config.Configuration();
      config.addCache(cacheConfiguration);
      config.addDiskStore(diskStoreConfiguration);
      return net.sf.ehcache.CacheManager.newInstance(config);
   }

}
