package de.qyotta.axonframework.eventstore;

import static org.axonframework.serializer.MessageSerializer.serializeMetaData;
import static org.axonframework.serializer.MessageSerializer.serializePayload;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.axonframework.domain.DomainEventMessage;
import org.axonframework.eventstore.EventStoreException;
import org.axonframework.serializer.SerializedObject;
import org.axonframework.serializer.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@SuppressWarnings({ "rawtypes", "nls" })
public class EsEventStorage {
   private static final String ES_EVENT_ID_HEADER = "ES-EventId";
   private static final String ES_EVENT_TYPE_HEADER = "ES-EventType";
   private static final String CONTENT_TYPE_HEADER = "Content-Type";
   private static final Logger LOGGER = LoggerFactory.getLogger(EsEventStore.class);
   private static final String CHARSET = "charset=utf-8";
   private static final String MEDIA_TYPE_APPLICATION_JSON = "application/json; ";
   private static final MediaType MEDIA_TYPE_JSON = MediaType.parse(MEDIA_TYPE_APPLICATION_JSON + "; " + CHARSET);
   private final Gson gson = new Gson();
   private final OkHttpClient client;
   private final Settings settings;
   private final Serializer serializer;

   public EsEventStorage(final Settings settings, final Serializer serializer) {
      this.settings = settings;
      this.serializer = serializer;
      client = new OkHttpClient().newBuilder()
            .connectTimeout(settings.getConnectionTimeoutMillis(), TimeUnit.MILLISECONDS)
            .build();
   }

   public void store(final String streamName, final String type, final DomainEventMessage message) {
      // curl -i -d@/home/greg/myevent.txt "http://127.0.0.1:2113/streams/newstream"
      // -H "Content-Type:application/json"
      // -H "ES-EventType:SomeEvent"
      // -H "ES-EventId:C322E299-CB73-4B47-97C5-5054F920746E"
      final Request request = new Request.Builder().url(getStreamUrl(streamName))
            .post(RequestBody.create(MEDIA_TYPE_JSON, serialize(type, message)))
            .header(CONTENT_TYPE_HEADER, MEDIA_TYPE_APPLICATION_JSON)
            .header(ES_EVENT_TYPE_HEADER, message.getPayloadType()
                  .getSimpleName())
            .header(ES_EVENT_ID_HEADER, message.getIdentifier())
            .build();
      try {
         final Response updateResponse = client.newCall(request)
               .execute();
         if (!String.valueOf(updateResponse.code())
               .startsWith("2")) {
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

   private String serialize(String type, DomainEventMessage event) {
      final SerializedObject<String> serializedPayloadObject = serializePayload(event, serializer, String.class);
      final SerializedObject<String> serializedMetaDataObject = serializeMetaData(event, serializer, String.class);
      final EventEntry eventEntry = EventEntry.builder()
            .aggregateType(type)
            .aggregateIdentifier(event.getAggregateIdentifier()
                  .toString())
            .sequenceNumber(event.getSequenceNumber())
            .eventIdentifier(event.getIdentifier())
            .serializedPayload(serializedPayloadObject.getData())
            .payloadType(serializedPayloadObject.getType()
                  .getName())
            .payloadRevision(serializedPayloadObject.getType()
                  .getRevision())
            .serializedMetaData(serializedMetaDataObject.getData())
            .timeStamp(event.getTimestamp()
                  .toString())
            .build();
      return gson.toJson(eventEntry);
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
