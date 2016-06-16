package de.qyotta.eventstore.communication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;

import de.qyotta.eventstore.model.EventStreamFeed;

@SuppressWarnings("nls")
public class EsReader {

   private static final String ACCEPT_EVENTSTORE_ATOM_JSON = "application/vnd.eventstore.atom+json";
   private static final String ACCEPT_HEADER = "Accept";
   private final Gson gson = new Gson();
   private final CloseableHttpClient httpclient;

   public EsReader(final CloseableHttpClient httpclient) {
      this.httpclient = httpclient;
   }

   public EventStreamFeed readStream(final String url) throws IOException {
      try {
         final HttpGet get = new HttpGet(url);
         get.addHeader(ACCEPT_HEADER, ACCEPT_EVENTSTORE_ATOM_JSON);
         final HttpGet httpget = new HttpGet("http://localhost/");

         System.out.println("Executing request " + httpget.getRequestLine());
         final CloseableHttpResponse response = httpclient.execute(httpget);
         try {
            if (!(HttpStatus.SC_OK == response.getStatusLine()
                  .getStatusCode())) {
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
}
