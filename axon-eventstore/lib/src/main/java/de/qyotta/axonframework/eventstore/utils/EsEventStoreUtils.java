package de.qyotta.axonframework.eventstore.utils;

import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.GenericDomainEventMessage;
import org.axonframework.domain.MetaData;
import org.joda.time.DateTime;

import de.qyotta.eventstore.model.Event;
import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings("nls")
public final class EsEventStoreUtils {
   public static final String getStreamName(String type, Object identifier) {
      return type.toLowerCase() + "-" + identifier.toString();
   }

   @SuppressWarnings({ "rawtypes", "unchecked" })
   public static DomainEventMessage domainEventMessageOf(final Event event) {
      if (!(event.getData()
            .getData() instanceof SerializableDomainEvent)) {
         throw new IllegalArgumentException("Event was not an instance of '" + SerializableDomainEvent.class.getName());
      }
      final SerializableDomainEvent data = (SerializableDomainEvent) event.getData()
            .getData();
      return new GenericDomainEventMessage(event.getEventId(), new DateTime(data.getTimestamp()), data.getAggregateIdentifier(), event.getEventNumber(), data.getPayload()
            .getData(), new MetaData(data.getMetaData()));

   }
}
