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
      return "domain-" + type.toLowerCase() + "-" + identifier.toString();
   }

   @SuppressWarnings({ "rawtypes", "unchecked" })
   public static DomainEventMessage domainEventMessageOf(final EventResponse eventResponse) {
      try {
         final Gson gson = createGson();
         final Class<?> payloadType = Class.forName(eventResponse.getContent()
               .getEventType());
         final Object payload = gson.fromJson(eventResponse.getContent()
               .getData(), payloadType);

         final Map<String, ?> metaData = gson.fromJson(eventResponse.getContent()
               .getMetadata(), METADATA_TYPE);

         final Map<String, ?> eventMetadata = (Map<String, ?>) metaData.get(Constants.EVENT_METADATA_KEY);
         return new GenericDomainEventMessage(eventResponse.getTitle(), new DateTime(eventResponse.getUpdated(), DateTimeZone.UTC), metaData.get(Constants.AGGREGATE_ID_KEY), eventResponse.getContent()
               .getEventNumber(), payload, new MetaData(eventMetadata));
      } catch (final ClassNotFoundException e) {
         throw new RuntimeException(e);
      }

   }

   private static Gson createGson() {
      //// final RuntimeTypeAdapterFactory<Entity> typeFactory = RuntimeTypeAdapterFactory.of(Entity.class, "type")
      //// .registerSubtype(UserEntity.class);
      // final Gson gson = new GsonBuilder().registerTypeAdapterFactory(typeFactory)
      // .create();
      // return gson;
      return new Gson();
   }

}
