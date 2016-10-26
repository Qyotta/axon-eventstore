package de.qyotta.axonframework.eventstore;

import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.DomainEventStream;

import com.github.msemys.esjc.EventStore;
import com.github.msemys.esjc.StreamEventsSlice;

import de.qyotta.axonframework.eventstore.utils.EsEventStoreUtils;

@SuppressWarnings({ "rawtypes" })
public class EsjcEventStreamBackedDomainEventStream implements DomainEventStream {

   private static final int NUMBER_OF_EVENTS_PER_SLICE = 10;
   private final EventStore client;
   private StreamEventsSlice currentSlice;
   private final int currentEventNumber = 0;

   public EsjcEventStreamBackedDomainEventStream(String streamName, com.github.msemys.esjc.EventStore client) {
      this.client = client;
      client.readStreamEventsForward(streamName, 0, NUMBER_OF_EVENTS_PER_SLICE, true).thenAccept(s -> {
         this.currentSlice = s;
      });
   }

   @Override
   public boolean hasNext() {
      return currentSlice != null && !(currentEventNumber == currentSlice.lastEventNumber);
   }

   @Override
   public DomainEventMessage next() {
      return EsEventStoreUtils.domainEventMessageOf(currentSlice.);
   }

   @Override
   public DomainEventMessage peek() {
      return EsEventStoreUtils.domainEventMessageOf(eventStream.peek());
   }

}
