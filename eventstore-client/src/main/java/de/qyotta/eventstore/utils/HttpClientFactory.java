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

import de.qyotta.eventstore.EventStoreSettings;

public class HttpClientFactory {

   public static CloseableHttpClient httpClient(final EventStoreSettings settings) {
      if (settings.isCacheResponses()) {
         return newClosableCachingHttpClient(settings);
      }
      return newClosableHttpClient(settings);
   }

   private static CloseableHttpClient newClosableHttpClient(EventStoreSettings settings) {
      return HttpClientBuilder.create()

            .setDefaultRequestConfig(requestConfig(settings))
            .setDefaultCredentialsProvider(credentialsProvider(settings))
            .setRedirectStrategy(new LaxRedirectStrategy())
            .setRetryHandler(new StandardHttpRequestRetryHandler())
            .setKeepAliveStrategy(new de.qyotta.eventstore.utils.DefaultConnectionKeepAliveStrategy())
            .setConnectionManagerShared(true)

            .build();
   }

   private static CloseableHttpClient newClosableCachingHttpClient(EventStoreSettings settings) {
      final CacheConfig cacheConfig = CacheConfig.custom()
            .setMaxCacheEntries(Integer.MAX_VALUE)
            .setMaxObjectSize(Integer.MAX_VALUE)
            .build();

      settings.getCacheDirectory()
            .mkdirs();

      return CachingHttpClientBuilder.create()
            .setHttpCacheStorage(new FileCacheStorage(cacheConfig, settings.getCacheDirectory()))
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

}
