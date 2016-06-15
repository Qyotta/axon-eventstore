package de.qyotta.axonframework.eventstore;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

import org.axonframework.domain.DomainEventMessage;
import org.axonframework.eventstore.EventStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@SuppressWarnings({ "rawtypes", "nls" })
public class EsEventSerializer {
   private static final String ES_EVENT_ID_HEADER = "ES-EventId";
   private static final String ES_EVENT_TYPE_HEADER = "ES-EventType";
   private static final String CONTENT_TYPE_HEADER = "Content-Type";
   private static final Logger LOGGER = LoggerFactory.getLogger(EsEventStore.class);
   private static final String CHARSET = "charset=utf-8";
   private static final String MEDIA_TYPE_APPLICATION_JSON = "application/json; ";
   private static final MediaType MEDIA_TYPE_JSON = MediaType.parse(MEDIA_TYPE_APPLICATION_JSON + "; " + CHARSET);
   private final OkHttpClient client;
   private final Settings settings;
   private final Gson gson;

   public EsEventSerializer(final Settings settings) {
      this.settings = settings;
      client = new OkHttpClient().newBuilder()
            .connectTimeout(settings.getConnectionTimeoutMillis(), TimeUnit.MILLISECONDS)
            .build();
      gson = new Gson();
   }

   public void serialize(final String stream, final DomainEventMessage message) {
      // curl -i -d@/home/greg/myevent.txt "http://127.0.0.1:2113/streams/newstream"
      // -H "Content-Type:application/json"
      // -H "ES-EventType:SomeEvent"
      // -H "ES-EventId:C322E299-CB73-4B47-97C5-5054F920746E"

      final Type type = domainEventMessageTypeToken(message.getPayloadType());
      final Request request = new Request.Builder().url(getStreamUrl(stream))
            .post(RequestBody.create(MEDIA_TYPE_JSON, gson.toJson(message, type)))
            .header(CONTENT_TYPE_HEADER, MEDIA_TYPE_APPLICATION_JSON)
            .header(ES_EVENT_TYPE_HEADER, message.getPayloadType()
                  .getSimpleName())
            .header(ES_EVENT_ID_HEADER, message.getIdentifier())
            .build();
      try {
         final Response updateResponse = client.newCall(request)
               .execute();
         if (updateResponse.code() != 200) {
            final String errorMessage = "Failed to serialize event to '" + request.url()
                  .toString() + "' Http status code is: " + updateResponse.code();
            throw new EventStoreException(errorMessage);
         }
      } catch (final IOException e) {
         final String errorMessage = "Failed to serialize event to '" + request.url()
               .toString() + "' HttpClient threw an exception.";
         if (LOGGER.isDebugEnabled()) {
            LOGGER.error(errorMessage, e);
         }
         throw new EventStoreException(errorMessage, e);
      }

   }

   @SuppressWarnings({ "unchecked", "serial" })
   static Type domainEventMessageTypeToken(final Class<?> classOfT) {
      return new TypeToken<DomainEventMessage<?>>() {
         //
      }.where(new TypeParameter() {
         //
      }, classOfT)
            .getType();

   }

   private String getStreamUrl(String stream) {

      final StringBuilder sb = new StringBuilder(settings.getHost());
      if (!settings.getHost()
            .endsWith("/")) {
         sb.append("/");
      }
      sb.append("streams/")
            .append(stream);
      return sb.toString();
   }

}
