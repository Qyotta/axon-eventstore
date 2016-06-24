package de.qyotta.eventstore;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

import de.qyotta.eventstore.model.SerializableEventData;
import de.qyotta.eventstore.utils.EventDataSerializer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@SuppressWarnings("nls")
public class EventStoreSettings {
   private static final String DEFAULT_PASSWORD = "changeit";
   private static final String DEFAULT_USERNAME = "admin";
   private static final String DEFAULT_SCHEME = "Basic";
   private static final String DEFAULT_REALM = "ES";
   private static final String DEFAULT_HOST = "http://127.0.0.1:2113";
   private static final int DEFAULT_CONNECTION_TIMEOUT_MILLIS = 2000;
   private static final int DEFAULT_SOCKET_TIMEOUT_MILLIS = 2000;

   private String host;
   private String realm;
   private String scheme;
   private String userName;
   private String password;
   private Integer connectionTimeoutMillis;
   private Integer socketTimeoutMillis;
   private JsonDeserializer<SerializableEventData> eventDataDeserializer;
   private JsonSerializer<SerializableEventData> eventDataSerializer;
   private boolean cacheResponses;

   public static EventStoreSettings.EventStoreSettingsBuilder withDefaults() {
      return EventStoreSettings.builder()
            .host(DEFAULT_HOST)
            .realm(DEFAULT_REALM)
            .scheme(DEFAULT_SCHEME)
            .userName(DEFAULT_USERNAME)
            .password(DEFAULT_PASSWORD)
            .connectionTimeoutMillis(DEFAULT_CONNECTION_TIMEOUT_MILLIS)
            .socketTimeoutMillis(DEFAULT_SOCKET_TIMEOUT_MILLIS)
            .eventDataDeserializer(new EventDataSerializer())
            .eventDataSerializer(new EventDataSerializer())
            .cacheResponses(true);
   }

}
