package de.qyotta.eventstore;

public class EventStoreClient {
   private final EsContext context;

   public EventStoreClient(EventStoreSettings settings) {
      context = new EsContext(settings);
   }

   /**
    * @param streamName
    * @return
    */
   public EventStream readEvents(final String streamName) {
      return new EventStream(streamName, context);
   }
}
