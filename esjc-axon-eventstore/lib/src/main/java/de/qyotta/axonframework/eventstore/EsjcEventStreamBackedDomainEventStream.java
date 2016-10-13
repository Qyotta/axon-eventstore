package de.qyotta.axonframework.eventstore;

import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.DomainEventStream;

import de.qyotta.axonframework.eventstore.utils.EsEventStoreUtils;
import de.qyotta.eventstore.EventStream;

@SuppressWarnings({ "rawtypes" })
public class EsjcEventStreamBackedDomainEventStream implements DomainEventStream {

   private final EventStream eventStream;

   public EsjcEventStreamBackedDomainEventStream(final EventStream eventStream) {
      this.eventStream = eventStream;
   }

   @Override
   public boolean hasNext() {
      return eventStream.hasNext();
   }

   @Override
   public DomainEventMessage next() {
      return EsEventStoreUtils.domainEventMessageOf(eventStream.next());
   }

   @Override
   public DomainEventMessage peek() {
      return EsEventStoreUtils.domainEventMessageOf(eventStream.peek());
   }

}
