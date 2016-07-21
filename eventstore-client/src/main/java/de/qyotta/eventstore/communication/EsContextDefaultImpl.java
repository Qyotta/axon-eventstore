package de.qyotta.eventstore.communication;

import org.apache.http.impl.client.CloseableHttpClient;

import de.qyotta.eventstore.EventStoreSettings;
import de.qyotta.eventstore.utils.HttpClientFactory;

public class EsContextDefaultImpl implements ESContext {

   private final CloseableHttpClient httpclient;
   private final ESReader reader;
   private final EventStoreSettings settings;
   private final ESWriter writer;

   public EsContextDefaultImpl(final EventStoreSettings settings) {
      this.settings = settings;
      httpclient = HttpClientFactory.httpClient(settings);

      reader = new EsReaderDefaultImpl(httpclient);
      writer = new EsWriterDefaultImpl(httpclient);
   }

   @Override
   public ESReader getReader() {
      return reader;
   }

   @Override
   public EventStoreSettings getSettings() {
      return settings;
   }

   @Override
   public ESWriter getWriter() {
      return writer;
   }

}
