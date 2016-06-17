package de.qyotta.axonframework.eventstore;

import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.domain.MetaData;
import org.joda.time.DateTime;

import de.qyotta.eventstore.EventStream;
import de.qyotta.eventstore.model.Event;

@SuppressWarnings({ "nls", "rawtypes" })
public class EsEventStreamBackedDomainEventStream implements DomainEventStream {

   private final EventStream eventStream;

   public EsEventStreamBackedDomainEventStream(final EventStream eventStream) {
      this.eventStream = eventStream;
   }

   @Override
   public boolean hasNext() {
      return eventStream.hasNext();
   }

   @Override
   public DomainEventMessage next() {
      return domainEventMessageOf(eventStream.next()
            .getContent());
   }

   @Override
   public DomainEventMessage peek() {
      return domainEventMessageOf(eventStream.peek()
            .getContent());
   }

   private DomainEventMessage domainEventMessageOf(final Event event) {
      if (!(event.getData()
            .getData() instanceof SerializableDomainEvent)) {
         throw new IllegalArgumentException("Event was not an instance of '" + SerializableDomainEvent.class.getName());
      }
      final SerializableDomainEvent data = (SerializableDomainEvent) event.getData()
            .getData();
      return DeserializedDomainEventMessage.builder()
            .aggregateIdentifier(data.getAggregateIdentifier())
            .identifier(event.getEventId())
            .timestamp(new DateTime(data.getTimestamp()))
            .payloadType(data.getPayload()
                  .getData()
                  .getClass())
            .payload(data.getPayload()
                  .getData())
            .metaData(new MetaData(data.getMetaData()))
            .sequenceNumber(event.getEventNumber())
            .build();
   }
}
