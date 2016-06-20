package de.qyotta.axonframework.eventstore;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.eventstore.EventStore;
import org.axonframework.eventstore.EventStreamNotFoundException;

import de.qyotta.eventstore.EventStoreClient;
import de.qyotta.eventstore.EventStoreSettings;
import de.qyotta.eventstore.EventStream;
import de.qyotta.eventstore.model.Event;
import de.qyotta.eventstore.model.SerializableEventData;

@SuppressWarnings({ "nls", "rawtypes" })
public class EsEventStore implements EventStore {
   private final EventStoreClient client;

   public EsEventStore(final EventStoreSettings settings) {
      this.client = new EventStoreClient(settings);
   }

   @Override
   public void appendEvents(final String type, final DomainEventStream events) {
      final Map<Object, List<Event>> identifierToEventStoreEvents = new HashMap<>();
      while (events.hasNext()) {
         final DomainEventMessage message = events.next();
         final Object identifier = message.getAggregateIdentifier();
         if (!identifierToEventStoreEvents.containsKey(identifier)) {
            identifierToEventStoreEvents.put(identifier, new LinkedList<Event>());
         }
         identifierToEventStoreEvents.get(identifier)
               .add(toEvent(message));
      }
      for (final Object identifier : identifierToEventStoreEvents.keySet()) {
         client.appendEvents(getStreamName(type, identifier), identifierToEventStoreEvents.get(identifier));
      }
   }

   @Override
   public DomainEventStream readEvents(String type, Object identifier) {
      final EventStream eventStoreEventStream = client.readEvents(getStreamName(type, identifier));
      final DomainEventStream stream = new EsEventStreamBackedDomainEventStream(eventStoreEventStream);
      if (!stream.hasNext()) {
         throw new EventStreamNotFoundException(type, identifier);
      }
      return stream;
   }

   private Event toEvent(DomainEventMessage message) {
      final HashMap<String, Object> metaData = new HashMap<>();
      for (final Entry<String, Object> entry : message.getMetaData()
            .entrySet()) {
         metaData.put(entry.getKey(), entry.getValue());
      }
      return Event.builder()
            .eventId(message.getIdentifier())
            .eventType(message.getPayloadType()
                  .getSimpleName())
            .data(SerializableEventData.of(SerializableDomainEvent.builder()
                  .aggregateIdentifier(message.getAggregateIdentifier())
                  .payload(SerializableEventData.of(message.getPayload()))
                  .timestamp(message.getTimestamp()
                        .toString())
                  .metaData(metaData)
                  .build()))
            .build();
   }

   private String getStreamName(String type, Object identifier) {
      return type.toLowerCase() + "-" + identifier.toString();
   }
}
