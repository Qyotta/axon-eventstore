package de.qyotta.eventstore;

import static de.qyotta.eventstore.utils.Constants.STREAMS_PATH;

import java.util.Collection;

import de.qyotta.eventstore.model.Event;

public class EventStoreClient {
   private final EsContext context;

   public EventStoreClient(EventStoreSettings settings) {
      context = new EsContext(settings);
   }

   public EventStream readEvents(final String streamName) {
      return new EventStream(streamUrlForName(streamName), context);
   }

   public void appendEvent(final String streamName, final Event event) {
      context.getWriter()
            .appendEvent(streamUrlForName(streamName), event);
   }

   public void appendEvents(final String streamName, final Collection<Event> collection) {
      context.getWriter()
            .appendEvents(streamUrlForName(streamName), collection);
   }

   private String streamUrlForName(final String streamName) {
      return context.getSettings()
            .getHost() + STREAMS_PATH + streamName;
   }
}
