package de.qyotta.eventstore;

import static de.qyotta.eventstore.utils.Constants.STREAMS_PATH;

import java.util.Collection;

import de.qyotta.eventstore.model.Event;
import de.qyotta.eventstore.utils.EventStreamReader;
import de.qyotta.eventstore.utils.EventStreamReader.EventStreamReaderCallback;

public class EventStoreClient {
   private final EsContext context;

   public EventStoreClient(EventStoreSettings settings) {
      context = new EsContext(settings);
   }

   /**
    * Creates a new {@link EventStreamReader}. Catch up is scheduled at the given interval. If the given interval is 0 or negative it will not be scheduled but can be invoked manually.
    *
    * @param streamName
    * @param intervalMillis
    * @param callback
    * @return
    */
   public EventStreamReader newEventStreamReader(final String streamName, final int intervalMillis, final EventStreamReaderCallback callback) {
      return new EventStreamReader(streamUrlForName(streamName), context, intervalMillis, callback);
   }

   public EventStream readEvents(final String streamName) {
      return new EventStreamImpl(streamUrlForName(streamName), context);
   }

   public void appendEvent(final String streamName, final Event event) {
      context.getWriter()
            .appendEvent(streamUrlForName(streamName), event);
   }

   public void appendEvents(final String streamName, final Collection<Event> collection) {
      context.getWriter()
            .appendEvents(streamUrlForName(streamName), collection);
   }

   public void deleteStream(final String streamName, final boolean deletePermanently) {
      context.getWriter()
            .deleteStream(streamUrlForName(streamName), deletePermanently);
   }

   private String streamUrlForName(final String streamName) {
      return context.getSettings()
            .getHost() + STREAMS_PATH + streamName;
   }
}
