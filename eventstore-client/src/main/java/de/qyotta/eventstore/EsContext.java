package de.qyotta.eventstore;

import org.apache.http.impl.client.CloseableHttpClient;

import de.qyotta.eventstore.communication.EsReader;
import de.qyotta.eventstore.communication.EsWriter;
import de.qyotta.eventstore.utils.HttpClientFactory;

public class EsContext {

   private final CloseableHttpClient httpclient;
   private final EsReader reader;
   private final EventStoreSettings settings;
   private final EsWriter writer;

   public EsContext(final EventStoreSettings settings) {
      this.settings = settings;
      httpclient = HttpClientFactory.httpClient(settings);

      reader = new EsReader(httpclient);
      writer = new EsWriter(httpclient);
   }

   public EsReader getReader() {
      return reader;
   }

   public EventStoreSettings getSettings() {
      return settings;
   }

   public EsWriter getWriter() {
      return writer;
   }

}
