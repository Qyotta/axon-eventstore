package de.qyotta.eventstore.communication;

import static de.qyotta.eventstore.utils.Constants.CONTENT_TYPE_HEADER;
import static de.qyotta.eventstore.utils.Constants.CONTENT_TYPE_JSON_EVENTS;
import static de.qyotta.eventstore.utils.Constants.ES_HARD_DELETE_HEADER;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.qyotta.eventstore.model.Event;

@SuppressWarnings("nls")
public class EsWriter {
   private static final Logger LOGGER = Logger.getLogger(EsWriter.class.getName());
   private final Gson gson;
   private final CloseableHttpClient httpclient;

   public EsWriter(final CloseableHttpClient httpclient) {
      this.httpclient = httpclient;
      final GsonBuilder gsonBuilder = new GsonBuilder();
      gson = gsonBuilder.create();
   }

   public void appendEvents(final String url, final Collection<Event> collection) {
      try {
         try {
            final HttpPost post = new HttpPost(url);
            post.addHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON_EVENTS);
            final JsonArray body = new JsonArray();
            for (final Event e : collection) {
               final JsonObject o = new JsonObject();
               o.addProperty("eventId", e.getEventId());
               o.addProperty("eventType", e.getEventType());
               o.add("data", gson.fromJson(e.getData(), JsonObject.class));
               o.add("metadata", gson.fromJson(e.getMetadata(), JsonObject.class));
               body.add(o);
            }

            final String jsonString = gson.toJson(body);
            @SuppressWarnings("deprecation")
            final StringEntity requestEntity = new StringEntity(jsonString, CONTENT_TYPE_JSON_EVENTS, "utf-8");
            post.setEntity(requestEntity);

            LOGGER.warn("Executing request " + read(post.getEntity()
                  .getContent()));
            final CloseableHttpResponse response = httpclient.execute(post);
            try {
               if (!(HttpStatus.SC_CREATED == response.getStatusLine()
                     .getStatusCode())) {
                  throw new RuntimeException("Could not save stream feed from url: " + url);
               }
            } finally {
               response.close();
            }
         } finally {
            httpclient.close();
         }
      } catch (final IOException e) {
         throw new RuntimeException("Could not save stream feed from url: " + url, e);
      }
   }

   public static String read(InputStream input) throws IOException {
      try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
         return buffer.lines()
               .collect(Collectors.joining("\n"));
      }
   }

   public void appendEvent(final String url, final Event event) {
      appendEvents(url, Arrays.asList(event));
   }

   public void deleteStream(final String url, boolean deletePermanently) {
      try {
         try {
            final HttpDelete delete = new HttpDelete(url);
            delete.addHeader(ES_HARD_DELETE_HEADER, String.valueOf(deletePermanently));

            LOGGER.info("Executing request " + delete.getRequestLine());
            final CloseableHttpResponse response = httpclient.execute(delete);
            try {
               if (!(HttpStatus.SC_NO_CONTENT == response.getStatusLine()
                     .getStatusCode())) {
                  throw new RuntimeException("Could not delete stream with url: " + url);
               }
            } finally {
               response.close();
            }
         } finally {
            httpclient.close();
         }
      } catch (final IOException e) {
         throw new RuntimeException("Could not delete stream with url: " + url, e);
      }
   }
}
