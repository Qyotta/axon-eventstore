package de.qyotta.axonframework.eventstore.utils;

import java.lang.reflect.Type;
import java.util.Map;

import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.GenericDomainEventMessage;
import org.axonframework.domain.MetaData;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.qyotta.eventstore.model.EventResponse;
import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings("nls")
public final class EsEventStoreUtils {
   private static final Type METADATA_TYPE = new TypeToken<Map<String, ?>>() {
      //
   }.getType();

   public static final String getStreamName(String type, Object identifier) {
      return type.toLowerCase() + "-" + identifier.toString();
   }

   @SuppressWarnings({ "rawtypes", "unchecked" })
   public static DomainEventMessage domainEventMessageOf(final EventResponse eventResponse) {
      try {
         final Gson gson = new Gson();
         final SerializableDomainEvent data = gson.fromJson(eventResponse.getContent()
               .getData(), SerializableDomainEvent.class);

         final Class<?> payloadType = Class.forName(eventResponse.getContent()
               .getEventType());
         final Object payload = gson.fromJson(data.getPayload(), payloadType);

         final Map<String, ?> metaData = gson.fromJson(eventResponse.getContent()
               .getMetadata(), METADATA_TYPE);
         return new GenericDomainEventMessage(eventResponse.getContent().getEventId(), new DateTime(eventResponse.getUpdated(), DateTimeZone.UTC), data.getAggregateIdentifier(), eventResponse.getContent()
               .getEventNumber(), payload, new MetaData(metaData));
      } catch (final ClassNotFoundException e) {
         throw new RuntimeException(e);
      }

   }

}
