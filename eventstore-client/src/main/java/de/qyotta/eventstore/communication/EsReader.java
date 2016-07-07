package de.qyotta.eventstore.communication;

import static de.qyotta.eventstore.utils.Constants.ACCEPT_EVENTSTORE_ATOM_JSON;
import static de.qyotta.eventstore.utils.Constants.ACCEPT_HEADER;
import static de.qyotta.eventstore.utils.Constants.EMBED_PARAM;
import static de.qyotta.eventstore.utils.Constants.TRY_HARDER;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import de.qyotta.eventstore.model.Event;
import de.qyotta.eventstore.model.EventDeletedException;
import de.qyotta.eventstore.model.EventResponse;
import de.qyotta.eventstore.model.EventStreamFeed;
import de.qyotta.eventstore.model.EventStreamNotFoundException;

@SuppressWarnings("nls")
public class EsReader {
   private static final Logger LOGGER = Logger.getLogger(EsReader.class.getName());
   private final Gson gson;
   private final CloseableHttpClient httpclient;

   public EsReader(final CloseableHttpClient httpclient) {
      this.httpclient = httpclient;
      final GsonBuilder gsonBuilder = new GsonBuilder();
      gsonBuilder.registerTypeAdapter(Event.class, new JsonDeserializer<Event>() {

         @Override
         public Event deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            final Event.EventBuilder e = Event.builder();
            final JsonObject object = json.getAsJsonObject();
            final JsonElement dataElement = object.get("data");
            final JsonElement metadataElement = object.get("metadata");

            final String eventStreamId = object.get("eventStreamId")
                  .getAsString();
            final String eventId = object.get("eventId")
                  .getAsString();
            final Long eventNumber = object.get("eventNumber")
                  .getAsLong();
            final String eventType = object.get("eventType")
                  .getAsString();

            final String data = gson.toJson(dataElement);
            final String metadata = gson.toJson(metadataElement);

            return e.eventStreamId(eventStreamId)
                  .eventId(eventId)
                  .eventNumber(eventNumber)
                  .eventType(eventType)
                  .data(data)
                  .metadata(metadata)
                  .build();
         }
      });
      gson = gsonBuilder.create();
   }

   public EventStreamFeed readStream(final String url) {
      try {
         return loadFeed(url);
      } catch (final IOException e) {
         throw new RuntimeException("Could not initialize EventStreamImpl from url: '" + url + "'.", e);
      }
   }

   public EventResponse readEvent(String url) {
      try {
         return loadEvent(url + "?" + EMBED_PARAM + "=" + TRY_HARDER);
      } catch (final IOException e) {
         throw new RuntimeException("Could not load EventResponse from url: '" + url + "'.", e);
      }

   }

   private EventStreamFeed loadFeed(final String url) throws IOException {
      try {
         final HttpGet httpget = new HttpGet(url);
         httpget.addHeader(ACCEPT_HEADER, ACCEPT_EVENTSTORE_ATOM_JSON);

         LOGGER.info("Executing request " + httpget.getRequestLine());
         final CloseableHttpResponse response = httpclient.execute(httpget);
         try {
            final int statusCode = response.getStatusLine()
                  .getStatusCode();
            if (HttpStatus.SC_NOT_FOUND == statusCode || HttpStatus.SC_NO_CONTENT == statusCode) {
               throw new EventStreamNotFoundException();
            }
            if (!(HttpStatus.SC_OK == statusCode)) {
               throw new RuntimeException("Could not load stream feed from url: " + url);
            }
            final EventStreamFeed result = gson.fromJson(new BufferedReader(new InputStreamReader(response.getEntity()
                  .getContent())), EventStreamFeed.class);
            EntityUtils.consume(response.getEntity());
            return result;
         } finally {
            response.close();
         }
      } finally {
         httpclient.close();
      }
   }

   private EventResponse loadEvent(final String url) throws IOException {
      try {
         final HttpGet httpget = new HttpGet(url);
         httpget.addHeader(ACCEPT_HEADER, ACCEPT_EVENTSTORE_ATOM_JSON);

         LOGGER.info("Executing request " + httpget.getRequestLine());
         final CloseableHttpResponse response = httpclient.execute(httpget);
         try {
            final int statusCode = response.getStatusLine()
                  .getStatusCode();
            if (HttpStatus.SC_GONE == statusCode) {
               throw new EventDeletedException();
            }
            if (!(HttpStatus.SC_OK == statusCode)) {
               throw new RuntimeException("Could not load stream feed from url: " + url);
            }
            final String read = read(response.getEntity()
                  .getContent());
            final EventResponse result = gson.fromJson(read, EventResponse.class);
            EntityUtils.consume(response.getEntity());
            return result;
         } finally {
            response.close();
         }
      } finally {
         httpclient.close();
      }
   }

   public static String read(InputStream input) throws IOException {
      try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
         return buffer.lines()
               .collect(Collectors.joining("\n"));
      }
   }
}
