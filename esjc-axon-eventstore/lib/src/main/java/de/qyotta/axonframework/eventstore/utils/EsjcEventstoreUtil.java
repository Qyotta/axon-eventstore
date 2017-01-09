package de.qyotta.axonframework.eventstore.utils;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Map;

import org.axonframework.domain.DomainEventMessage;
import org.axonframework.domain.GenericDomainEventMessage;
import org.axonframework.domain.MetaData;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.github.msemys.esjc.RecordedEvent;
import com.github.msemys.esjc.ResolvedEvent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings("nls")
public final class EsjcEventstoreUtil {
   private static final Type METADATA_TYPE = new TypeToken<Map<String, ?>>() {
      //
   }.getType();

   private static final Gson gson = new Gson();
   private static final Charset UTF_8 = Charset.forName("UTF-8");

   public static final String getStreamName(final String type, final Object identifier, final String prefix) {
      return prefix + "-" + type.toLowerCase() + "-" + identifier.toString();
   }

   @SuppressWarnings({ "rawtypes", "unchecked" })
   public static DomainEventMessage domainEventMessageOf(final ResolvedEvent event) {
      try {
         final RecordedEvent originalEvent = event.originalEvent();
         final Class<?> payloadType = Class.forName(originalEvent.eventType);
         final Object payload = gson.fromJson(new String(originalEvent.data, UTF_8), payloadType);
         final Map<String, ?> metaData = gson.fromJson(new String(originalEvent.metadata, UTF_8), METADATA_TYPE);

         final Map<String, ?> eventMetadata = (Map<String, ?>) metaData.get(Constants.EVENT_METADATA_KEY);
         final int sequenceNumber = originalEvent.eventNumber;
         final DateTime dateTime = new DateTime(originalEvent.created.toEpochMilli(), DateTimeZone.UTC);
         final String identifier = String.valueOf(originalEvent.eventId);
         final Object aggregateIdentifier = metaData.get(Constants.AGGREGATE_ID_KEY);
         return new GenericDomainEventMessage(identifier, dateTime, aggregateIdentifier, sequenceNumber, payload, new MetaData(eventMetadata));
      } catch (final ClassNotFoundException e) {
         throw new RuntimeException(e);
      }

   }
}
