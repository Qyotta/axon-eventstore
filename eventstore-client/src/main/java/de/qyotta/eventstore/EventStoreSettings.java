package de.qyotta.eventstore;

import java.io.File;

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
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("nls")
public class EventStoreSettings {
   private static final String DEFAULT_PASSWORD = "changeit";
   private static final String DEFAULT_USERNAME = "admin";
   private static final String DEFAULT_SCHEME = "Basic";
   private static final String DEFAULT_REALM = "ES";
   private static final String DEFAULT_HOST = "http://127.0.0.1:2113";
   private static final int DEFAULT_CONNECTION_TIMEOUT_MILLIS = 10000;
   private static final int DEFAULT_SOCKET_TIMEOUT_MILLIS = 10000;

   private String host;
   private String realm;
   private String scheme;
   private String userName;
   private String password;
   private Integer connectionTimeoutMillis;
   private Integer socketTimeoutMillis;
   private File cacheDirectory;
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
            .cacheDirectory(new File(System.getProperty("java.io.tmpdir") + "/es"))
            .cacheResponses(true);
   }

}
