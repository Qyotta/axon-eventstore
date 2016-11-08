package de.qyotta.axonframework.eventstore;

import de.qyotta.axonframework.eventstore.utils.Constants;
import de.qyotta.eventstore.EventStoreClient;
import de.qyotta.eventstore.EventStream;
import de.qyotta.eventstore.model.Event;

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

/**
 * @author satan
 *
 */
@SuppressWarnings({ "rawtypes" })
public class EsEventStore implements EventStore {
   private final EventStoreClient client;
   private final Gson gson = new Gson();
   @SuppressWarnings("nls")
   private String prefix = "domain";

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
         client.appendEvents(getStreamName(type, identifier, prefix), identifierToEventStoreEvents.get(identifier));
      }
   }

   @Override
   public DomainEventStream readEvents(String type, Object identifier) {
      DomainEventStream stream;
      try {
         final EventStream eventStoreEventStream = client.readEvents(getStreamName(type, identifier, prefix));
         stream = new EsEventStreamBackedDomainEventStream(eventStoreEventStream);
         if (!stream.hasNext()) {
            throw new EventStreamNotFoundException(type, identifier);
         }
      } catch (final de.qyotta.eventstore.model.EventStreamNotFoundException e) {
         throw new EventStreamNotFoundException(String.format("Aggregate of type [%s] with identifier [%s] cannot be found.", type, identifier), e); //$NON-NLS-1$
      }
      return stream;
   }

   /**
    * Set the prefix to use for domain-event-streams. This defaults to 'domain'.
    *
    * @param prefix
    */
   public void setPrefix(final String prefix) {
      this.prefix = prefix;
   }

   private Event toEvent(final DomainEventMessage message) {
      final HashMap<String, Object> metaData = new HashMap<>();
      final HashMap<String, Object> eventMetaData = new HashMap<>();
      for (final Entry<String, Object> entry : message.getMetaData()
            .entrySet()) {
         eventMetaData.put(entry.getKey(), entry.getValue());
      }

      metaData.put(Constants.AGGREGATE_ID_KEY, message.getAggregateIdentifier());
      metaData.put(Constants.PAYLOAD_REVISION_KEY, getPayloadRevision(message.getPayloadType()));
      metaData.put(Constants.EVENT_METADATA_KEY, eventMetaData);

      return Event.builder()
            .eventId(message.getIdentifier())
            .eventType(message.getPayloadType()
                  .getName())
            .data(serialize(message.getPayload()))
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
