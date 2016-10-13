package de.qyotta.axonframework.eventstore;

import static de.qyotta.axonframework.eventstore.utils.EsEventStoreUtils.getStreamName;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.eventstore.EventStore;
import org.axonframework.eventstore.EventStreamNotFoundException;
import org.axonframework.serializer.Revision;

import com.github.msemys.esjc.EventData;
import com.github.msemys.esjc.ExpectedVersion;
import com.google.gson.Gson;

import de.qyotta.axonframework.eventstore.utils.Constants;
import de.qyotta.eventstore.EventStream;

@SuppressWarnings({ "rawtypes" })
public class EsjcEventStore implements EventStore {
   private final com.github.msemys.esjc.EventStore client;
   private final Gson gson = new Gson();

   public EsjcEventStore(final com.github.msemys.esjc.EventStore client) {
      this.client = client;
   }

   @Override
   public void appendEvents(final String type, final DomainEventStream events) {
      final Map<Object, List<EventData>> identifierToEventStoreEvents = new HashMap<>();
      while (events.hasNext()) {
         final DomainEventMessage message = events.next();
         final Object identifier = message.getAggregateIdentifier();
         if (!identifierToEventStoreEvents.containsKey(identifier)) {
            identifierToEventStoreEvents.put(identifier, new LinkedList<EventData>());
         }
         identifierToEventStoreEvents.get(identifier)
               .add(toEvent(message));
      }
      for (final Entry<Object, List<EventData>> entry : identifierToEventStoreEvents.entrySet()) {
         client.appendToStream(getStreamName(type, entry.getKey()), ExpectedVersion.any(), identifierToEventStoreEvents.get(entry.getValue()));
      }
   }

   @Override
   public DomainEventStream readEvents(String type, Object identifier) {
      DomainEventStream stream;
      try {
         client.readStreamEventsBackward(stream, start, count, resolveLinkTos)
         final EventStream eventStoreEventStream = client.readEvents(getStreamName(type, identifier));
         stream = new EsjcEventStreamBackedDomainEventStream(eventStoreEventStream);
         if (!stream.hasNext()) {
            throw new EventStreamNotFoundException(type, identifier);
         }
      } catch (final de.qyotta.eventstore.model.EventStreamNotFoundException e) {
         throw new EventStreamNotFoundException(String.format("Aggregate of type [%s] with identifier [%s] cannot be found.", type, identifier), e); //$NON-NLS-1$
      }
      return stream;
   }

   private EventData toEvent(final DomainEventMessage message) {
      final HashMap<String, Object> metaData = new HashMap<>();
      final HashMap<String, Object> eventMetaData = new HashMap<>();
      for (final Entry<String, Object> entry : message.getMetaData()
            .entrySet()) {
         eventMetaData.put(entry.getKey(), entry.getValue());
      }

      metaData.put(Constants.AGGREGATE_ID_KEY, message.getAggregateIdentifier());
      metaData.put(Constants.PAYLOAD_REVISION_KEY, getPayloadRevision(message.getPayloadType()));
      metaData.put(Constants.EVENT_METADATA_KEY, eventMetaData);

      return EventData.newBuilder()
            .eventId(UUID.fromString(message.getIdentifier())) // TODO check if it is save to assume that this can always be converted to a UUID
            .jsonData(serialize(message.getPayload()))
            .type(message.getPayloadType()
                  .getName())
            .metadata(serialize(metaData))
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
