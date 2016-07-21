package de.qyotta.eventstore;

import static de.qyotta.eventstore.utils.Constants.STREAMS_PATH;

import java.util.Collection;

import de.qyotta.eventstore.communication.ESContext;
import de.qyotta.eventstore.model.Event;
import de.qyotta.eventstore.utils.EventStreamReader;
import de.qyotta.eventstore.utils.EventStreamReaderImpl;
import de.qyotta.eventstore.utils.EventStreamReaderImpl.EventStreamReaderCallback;

public class EventStoreClient {
   private final ESContext context;

   public EventStoreClient(ESContext context) {
      this.context = context;
   }

   /**
    * Creates a new {@link EventStreamReaderImpl}. Catch up is scheduled at the given interval. If the given interval is 0 or negative it will not be scheduled but can be invoked manually.
    *
    * @param streamName
    * @param intervalMillis
    * @param callback
    * @return
    */
   public EventStreamReader newEventStreamReader(final String streamName, final int intervalMillis, final EventStreamReaderCallback callback) {
      return new EventStreamReaderImpl(streamUrlForName(streamName), context, intervalMillis, callback);
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
