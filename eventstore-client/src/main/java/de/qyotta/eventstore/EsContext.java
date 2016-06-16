package de.qyotta.eventstore;

import org.apache.http.impl.client.CloseableHttpClient;

import de.qyotta.eventstore.communication.EsReader;

public class EsContext {

   private final CloseableHttpClient httpclient;
   private final EsReader reader;
   private final EventStoreSettings settings;

   public EsContext(EventStoreSettings settings) {
      this.settings = settings;
      httpclient = HttpClientFactory.newClosableHttpClient(settings);
      reader = new EsReader(httpclient);
   }

   public EsReader getReader() {
      return reader;
   }

   public EventStoreSettings getSettings() {
      return settings;
   }
}
