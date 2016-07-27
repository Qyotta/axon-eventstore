package de.qyotta.eventstore.communication;

import static de.qyotta.eventstore.utils.Constants.CONTENT_TYPE_HEADER;
import static de.qyotta.eventstore.utils.Constants.CONTENT_TYPE_JSON;
import static de.qyotta.eventstore.utils.Constants.CONTENT_TYPE_JSON_EVENTS;
import static de.qyotta.eventstore.utils.Constants.ES_HARD_DELETE_HEADER;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.cache.HttpCacheContext;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.qyotta.eventstore.model.Event;
import de.qyotta.eventstore.utils.HttpCacheLoggingUtil;

@SuppressWarnings("nls")
public class EsWriterDefaultImpl implements ESWriter {
   private static final Logger LOGGER = LoggerFactory.getLogger(EsWriterDefaultImpl.class.getName());
   private final Gson gson;
   private final CloseableHttpClient httpclient;
   private final String name;

   public EsWriterDefaultImpl(final CloseableHttpClient httpclient) {
      this(EsWriterDefaultImpl.class.getSimpleName() + "_" + UUID.randomUUID(), httpclient);
   }

   public EsWriterDefaultImpl(String name, final CloseableHttpClient httpclient) {
      this.name = name;
      this.httpclient = httpclient;
      final GsonBuilder gsonBuilder = new GsonBuilder();
      gson = gsonBuilder.create();
   }

   @Override
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
            post.setEntity(new StringEntity(jsonString, ContentType.create(CONTENT_TYPE_JSON_EVENTS, Consts.UTF_8)));

            LOGGER.debug("Executing request " + read(post.getEntity()
                  .getContent()));

            final HttpCacheContext context = HttpCacheContext.create();
            CloseableHttpResponse response = null;
            try {
               response = httpclient.execute(post, context);

               HttpCacheLoggingUtil.logCacheResponseStatus(name, context.getCacheResponseStatus());

               if (HttpStatus.SC_CREATED != response.getStatusLine()
                     .getStatusCode()) {
                  throw new RuntimeException("Unexpected responsecode: " + response.getStatusLine()
                        .getStatusCode() + " for URL: " + url);
               }

            } catch (final Exception e) {
               final StringBuilder sb = new StringBuilder();
               sb.append("Details:");

               if (response != null) {
                  sb.append("code: ")
                        .append(response.getStatusLine()
                              .getStatusCode())
                        .append("\n");
                  sb.append("reason: ")
                        .append(response.getStatusLine()
                              .getReasonPhrase())
                        .append("\n");
                  for (final Header header : response.getAllHeaders()) {
                     sb.append("header " + header.getName() + " = " + header.getValue());
                     sb.append("\n");
                  }

                  sb.append("toString: ")
                        .append(response.toString())
                        .append("\n");

               }
               throw new RuntimeException("Could not appends events to stream-url: " + url + ": " + sb.toString(), e);
            } finally {
               if (response != null) {
                  response.close();
               }
            }
         } finally {
            httpclient.close();
         }
      } catch (

      final Exception e) {
         throw new RuntimeException("Could not appends events to stream-url: " + url, e);
      }
   }

   private static String read(InputStream input) throws IOException {
      try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
         return buffer.lines()
               .collect(Collectors.joining("\n"));
      }
   }

   @Override
   public void appendEvent(final String url, final Event event) {
      appendEvents(url, Arrays.asList(event));
   }

   @Override
   public void deleteStream(final String url, boolean deletePermanently) {
      try {
         try {
            final HttpDelete delete = new HttpDelete(url);
            delete.addHeader(ES_HARD_DELETE_HEADER, String.valueOf(deletePermanently));

            LOGGER.info("Executing request " + delete.getRequestLine());
            final CloseableHttpResponse response = httpclient.execute(delete);
            try {
               if (HttpStatus.SC_NO_CONTENT != response.getStatusLine()
                     .getStatusCode()) {
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

   @Override
   public void createLinkedProjection(String host, String projectionName, String... includedStreams) {
      final String url = host + "/projections/continuous?name=" + projectionName + "&emit=yes&checkpoints=yes&enabled=yes";
      try {
         try {
            final HttpPost post = new HttpPost(url);
            post.addHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON);
            post.setEntity(new StringEntity(getProjectionForStreams(includedStreams), ContentType.create(CONTENT_TYPE_JSON_EVENTS, Consts.UTF_8)));
            LOGGER.info("Executing request " + post.getRequestLine());
            final CloseableHttpResponse response = httpclient.execute(post);
            try {
               if (HttpStatus.SC_NO_CONTENT != response.getStatusLine()
                     .getStatusCode()) {
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

   private String getProjectionForStreams(String[] includedStreams) {
      final StringBuilder sb = new StringBuilder("fromStreams([");
      final Iterator<String> iterator = Arrays.asList(includedStreams)
            .iterator();
      while (iterator.hasNext()) {
         sb.append("'" + iterator.next() + "'");
         if (iterator.hasNext()) {
            sb.append(",");
         }
      }
      sb.append("])");
      return sb.toString();
   }

}
