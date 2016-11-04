package de.qyotta.axonframework.eventstore;

import de.qyotta.axonframework.eventstore.utils.EsjcEventstoreUtil;

import java.util.concurrent.ExecutionException;

import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.DomainEventStream;
import org.slf4j.LoggerFactory;

import com.github.msemys.esjc.EventStore;
import com.github.msemys.esjc.ResolvedEvent;
import com.github.msemys.esjc.SliceReadStatus;
import com.github.msemys.esjc.StreamEventsSlice;

@SuppressWarnings({ "rawtypes" })
public class EsjcEventStreamBackedDomainEventStream implements DomainEventStream {

   private static final int NUMBER_OF_EVENTS_PER_SLICE = 4000;
   private final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(EsjcEventStore.class);
   private StreamEventsSlice currentSlice;
   private int currentEventNumber = 0;

   public EsjcEventStreamBackedDomainEventStream(String streamName, EventStore client) {
      try {
         this.currentSlice = client.readStreamEventsForward(streamName, 0, NUMBER_OF_EVENTS_PER_SLICE, true)
               .get();
         int i = 0;
         i++;
      } catch (InterruptedException | ExecutionException e) {
         LOGGER.error(e.getMessage(), e);
      }
   }

   @Override
   public boolean hasNext() {
      return currentSlice.events != null && !currentSlice.events.isEmpty() && currentEventNumber < currentSlice.events.size() && currentSlice.status.equals(SliceReadStatus.Success);
   }

   @Override
   public DomainEventMessage next() {
      final ResolvedEvent resolvedEvent = currentSlice.events.get(currentEventNumber);
      currentEventNumber++;
      return EsjcEventstoreUtil.domainEventMessageOf(resolvedEvent);
   }

   @Override
   public DomainEventMessage peek() {
      final ResolvedEvent resolvedEvent = currentSlice.events.get(currentEventNumber);
      return EsjcEventstoreUtil.domainEventMessageOf(resolvedEvent);
   }

}
