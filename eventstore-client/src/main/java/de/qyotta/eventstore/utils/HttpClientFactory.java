package de.qyotta.eventstore.utils;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import de.qyotta.eventstore.EventStoreSettings;

public class HttpClientFactory {

   public static CloseableHttpClient newClosableHttpClient(EventStoreSettings settings) {
      final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(new AuthScope(null, -1), new UsernamePasswordCredentials(settings.getUserName(), settings.getPassword()));
      final HttpClientConnectionManager poolingConnManager = new PoolingHttpClientConnectionManager();
      return HttpClientBuilder.create()
            .setConnectionManager(poolingConnManager)
            .setConnectionManagerShared(true)
            .setDefaultCredentialsProvider(credentialsProvider)
            .build();
   }

}
