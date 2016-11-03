package de.qyotta.axonframework.eventstore;

import de.qyotta.axonframework.eventstore.utils.Constants;
import de.qyotta.axonframework.eventstore.utils.EsjcEventstoreUtil;

import static de.qyotta.axonframework.eventstore.utils.EsjcEventstoreUtil.getStreamName;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.DomainEventStream;
import org.axonframework.eventstore.EventStore;
import org.axonframework.eventstore.EventStreamNotFoundException;
import org.axonframework.serializer.Revision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.msemys.esjc.EventData;
import com.github.msemys.esjc.ExpectedVersion;
import com.google.gson.Gson;

@SuppressWarnings({ "rawtypes" })
public class EsjcEventStore implements EventStore {
   private final com.github.msemys.esjc.EventStore client;
   private final Gson gson = new Gson();
   private final Logger LOGGER = LoggerFactory.getLogger(EsjcEventStore.class);

   public EsjcEventStore(final com.github.msemys.esjc.EventStore client) {
      this.client = client;
   }

   @Override
   public void appendEvents(final String type, final DomainEventStream eventStream) {
      final Map<Object, List<EventData>> identifierToEventStoreEvents = new HashMap<>();
      while (eventStream.hasNext()) {
         final DomainEventMessage message = eventStream.next();
         final Object identifier = message.getAggregateIdentifier();
         if (!identifierToEventStoreEvents.containsKey(identifier)) {
            identifierToEventStoreEvents.put(identifier, new LinkedList<EventData>());
         }
         identifierToEventStoreEvents.get(identifier).add(toEvent(message));
      }
      for (final Entry<Object, List<EventData>> entry : identifierToEventStoreEvents.entrySet()) {
         final String streamName = getStreamName(type, entry.getKey());
         final List<EventData> events = entry.getValue();
         try {
            client.appendToStream(streamName, ExpectedVersion.any(), events).get();
         } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(e.getMessage(), e);
         }
      }
   }

   @Override
   public DomainEventStream readEvents(String type, Object identifier) {
      try {
         final String streamName = EsjcEventstoreUtil.getStreamName(type, identifier);
         final EsjcEventStreamBackedDomainEventStream eventStream = new EsjcEventStreamBackedDomainEventStream(streamName, client);
         return eventStream;
      } catch (final EventStreamNotFoundException e) {
         throw new EventStreamNotFoundException(String.format("Aggregate of type [%s] with identifier [%s] cannot be found.", type, identifier), e); //$NON-NLS-1$
      }
   }

   private EventData toEvent(final DomainEventMessage message) {
      final HashMap<String, Object> metaData = new HashMap<>();
      final HashMap<String, Object> eventMetaData = new HashMap<>();
      for (final Entry<String, Object> entry : message.getMetaData().entrySet()) {
         eventMetaData.put(entry.getKey(), entry.getValue());
      }

      metaData.put(Constants.AGGREGATE_ID_KEY, message.getAggregateIdentifier());
      metaData.put(Constants.PAYLOAD_REVISION_KEY, getPayloadRevision(message.getPayloadType()));
      metaData.put(Constants.EVENT_METADATA_KEY, eventMetaData);

      return EventData.newBuilder().eventId(UUID.fromString(message.getIdentifier())) // TODO check if it is save to assume that this can always be converted to a UUID
            .jsonData(serialize(message.getPayload())).type(message.getPayloadType().getName()).metadata(serialize(metaData)).build();
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
