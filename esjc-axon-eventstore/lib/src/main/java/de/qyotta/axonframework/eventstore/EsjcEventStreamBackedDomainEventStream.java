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
   private static final int DEFAULT_NUMBER_OF_EVENTS_PER_SLICE = 4000;
   private int currentEventNumber = 0;
   private final LinkedList<ResolvedEvent> events = new LinkedList<>();

   public EsjcEventStreamBackedDomainEventStream(String streamName, EventStore client) {
      this(streamName, client, 0);
   }

   public EsjcEventStreamBackedDomainEventStream(final String streamName, final EventStore client, final long firstSequenceNumber) {
      boolean hasNext = true;
      long from = firstSequenceNumber;
      while (hasNext) {
         try {

            final StreamEventsSlice streamEventsSlice = client.readStreamEventsForward(streamName, from, DEFAULT_NUMBER_OF_EVENTS_PER_SLICE, true)
                  .get();
            events.addAll(streamEventsSlice.events);

            hasNext = !streamEventsSlice.isEndOfStream;
            from = streamEventsSlice.nextEventNumber;
         } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(e.getMessage(), e);
         }
      }
   }

   public EsjcEventStreamBackedDomainEventStream(String streamName, EventStore client, long firstSequenceNumber, long lastSequenceNumber) {

      long from = firstSequenceNumber;

      long numberOfEventsTotal = lastSequenceNumber - firstSequenceNumber;
      int numberOfEventsPerSlice = DEFAULT_NUMBER_OF_EVENTS_PER_SLICE;
      if (numberOfEventsTotal < DEFAULT_NUMBER_OF_EVENTS_PER_SLICE) {
         numberOfEventsPerSlice = Math.toIntExact(numberOfEventsTotal);
      }

      while (numberOfEventsTotal > 0) {
         try {
            final StreamEventsSlice streamEventsSlice = client.readStreamEventsForward(streamName, from, numberOfEventsPerSlice, true)
                  .get();
            events.addAll(streamEventsSlice.events);
            from = streamEventsSlice.nextEventNumber;
            numberOfEventsTotal = numberOfEventsTotal - numberOfEventsPerSlice;
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
