package de.qyotta.axonframework.eventstore;

import static de.qyotta.axonframework.eventstore.utils.EsEventStoreUtils.getStreamName;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.eventstore.EventStore;
import org.axonframework.eventstore.EventStreamNotFoundException;
import org.axonframework.serializer.Revision;

import com.google.gson.Gson;

import de.qyotta.axonframework.eventstore.utils.SerializableDomainEvent;
import de.qyotta.eventstore.EventStoreClient;
import de.qyotta.eventstore.EventStream;
import de.qyotta.eventstore.model.Event;

@SuppressWarnings({ "rawtypes" })
public class EsEventStore implements EventStore {
   private final EventStoreClient client;
   private final Gson gson = new Gson();

   public EsEventStore(final EventStoreClient client) {
      this.client = client;
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
      DomainEventStream stream;
      try {
         final EventStream eventStoreEventStream = client.readEvents(getStreamName(type, identifier));
         stream = new EsEventStreamBackedDomainEventStream(eventStoreEventStream);
         if (!stream.hasNext()) {
            throw new EventStreamNotFoundException(type, identifier);
         }
      } catch (final de.qyotta.eventstore.model.EventStreamNotFoundException e) {
         throw new EventStreamNotFoundException(type, identifier);
      }
      return stream;
   }

   private Event toEvent(final DomainEventMessage message) {
      final HashMap<String, Object> metaData = new HashMap<>();
      for (final Entry<String, Object> entry : message.getMetaData()
            .entrySet()) {
         metaData.put(entry.getKey(), entry.getValue());
      }
      final String serialize = serialize(message.getPayload());
      return Event.builder()
            .eventId(message.getIdentifier())
            .eventType(message.getPayloadType()
                  .getName())
            .data(serialize(SerializableDomainEvent.builder()
                  .aggregateIdentifier(message.getAggregateIdentifier())
                  .payload(serialize)
                  .payloadRevision(getPayloadRevision(message.getPayloadType()))
                  .build()))
            .metadata(serialize(message.getMetaData()))
            .build();
   }

   private String getPayloadRevision(Class<?> payloadType) {
      final Revision revision = payloadType.getDeclaredAnnotation(Revision.class);
      if (revision != null) {
         return revision.value();
      }
      return null;
   }

   private String serialize(Object payload) {
      return gson.toJson(payload);
   }
}
