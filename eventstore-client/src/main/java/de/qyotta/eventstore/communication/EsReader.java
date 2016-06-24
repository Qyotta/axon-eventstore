package de.qyotta.eventstore.communication;

import static de.qyotta.eventstore.utils.Constants.ACCEPT_EVENTSTORE_ATOM_JSON;
import static de.qyotta.eventstore.utils.Constants.ACCEPT_HEADER;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

import de.qyotta.eventstore.model.EventResponse;
import de.qyotta.eventstore.model.EventStreamFeed;
import de.qyotta.eventstore.model.EventStreamNotFoundException;
import de.qyotta.eventstore.model.SerializableEventData;

@SuppressWarnings("nls")
public class EsReader {
   private static final Logger LOGGER = Logger.getLogger(EsReader.class.getName());
   private final Gson gson;
   private final CloseableHttpClient httpclient;

   public EsReader(final CloseableHttpClient httpclient, JsonDeserializer<SerializableEventData> deserializer) {
      this.httpclient = httpclient;
      final GsonBuilder gsonBuilder = new GsonBuilder();
      gsonBuilder.registerTypeAdapter(SerializableEventData.class, deserializer);
      gson = gsonBuilder.create();
   }

   public EventStreamFeed readStream(final String url) {
      try {
         return load(url, EventStreamFeed.class);
      } catch (final IOException e) {
         throw new RuntimeException("Could not initialize EventStreamImpl from url: '" + url + "'.", e);
      }
   }

   public EventResponse readEvent(String url) {
      try {
         return load(url, EventResponse.class);
      } catch (final IOException e) {
         throw new RuntimeException("Could not load EventResponse from url: '" + url + "'.", e);
      }

   }

   private <T> T load(final String url, Class<T> type) throws IOException {
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
            final T result = gson.fromJson(new BufferedReader(new InputStreamReader(response.getEntity()
                  .getContent())), type);
            EntityUtils.consume(response.getEntity());
            return result;
         } finally {
            response.close();
         }
      } finally {
         httpclient.close();
      }
   }
}
