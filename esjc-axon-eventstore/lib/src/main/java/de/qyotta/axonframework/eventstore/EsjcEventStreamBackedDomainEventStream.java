package de.qyotta.axonframework.eventstore;

import java.util.LinkedList;
import java.util.concurrent.ExecutionException;

import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.DomainEventStream;
import org.slf4j.LoggerFactory;

import com.github.msemys.esjc.EventStore;
import com.github.msemys.esjc.ResolvedEvent;
import com.github.msemys.esjc.StreamEventsSlice;

import de.qyotta.axonframework.eventstore.utils.EsjcEventstoreUtil;

@SuppressWarnings({ "rawtypes" })
public class EsjcEventStreamBackedDomainEventStream implements DomainEventStream {
   private final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(EsjcEventStore.class);
   private static final int NUMBER_OF_EVENTS_PER_SLICE = 4000;
   private int currentEventNumber = 0;
   private final LinkedList<ResolvedEvent> events = new LinkedList<>();

   public EsjcEventStreamBackedDomainEventStream(String streamName, EventStore client) {

      boolean hasNext = true;
      int from = 0;
      while (hasNext) {
         try {
            final StreamEventsSlice streamEventsSlice = client.readStreamEventsForward(streamName, from, NUMBER_OF_EVENTS_PER_SLICE, true)
                  .get();
            events.addAll(streamEventsSlice.events);
            hasNext = !streamEventsSlice.isEndOfStream;
            from = streamEventsSlice.nextEventNumber;
         } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(e.getMessage(), e);
         }
      }
   }

   @Override
   public boolean hasNext() {
      return !events.isEmpty() && currentEventNumber < events.size();
   }

   @Override
   public DomainEventMessage next() {
      final ResolvedEvent resolvedEvent = events.get(currentEventNumber);
      currentEventNumber++;
      return EsjcEventstoreUtil.domainEventMessageOf(resolvedEvent);
   }

   @Override
   public DomainEventMessage peek() {
      final ResolvedEvent resolvedEvent = events.get(currentEventNumber);
      return EsjcEventstoreUtil.domainEventMessageOf(resolvedEvent);
   }

}
