package de.qyotta.neweventstore;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;

import de.qyotta.eventstore.model.EventResponse;

public class MainExample {

   private static ESHttpEventStore esHttpEventStore;
   private static String streamName;
   private static int retryWaitTime;

   public static void main(String[] args) throws ReadFailedException, MalformedURLException, InterruptedException {
      final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("admin", "tynW3Z9bWUeD4K8B3t4d"));

      esHttpEventStore = new ESHttpEventStore("", new URL("http://localhost:2123"), credentialsProvider);
      streamName = "$ce-mda";

      // readBackwards();
      readForward();
   }

   private static void readForward() throws ReadFailedException, InterruptedException {
      retryWaitTime = 1;

      final long start = System.currentTimeMillis();

      int nextEventNumber = 0;

      while (true) {
         try {
            final StreamEventsSlice slice = esHttpEventStore.readEventsForward(streamName, nextEventNumber, 500, "");

            nextEventNumber = slice.getNextEventNumber();

            final long end = System.currentTimeMillis();

            final double throughput = nextEventNumber / (double) ((end - start) / 1000);

            String x = "";

            if (slice.isEndOfStream()) {
               x += "[head of stream reached] ";
            }

            x += "Read " + slice.getNextEventNumber() + ". throughput:" + throughput + " events/s";

            System.out.println(x);
            retryWaitTime = 1;
         } catch (final Exception e) {
            System.err.println("Failed to process. Job ends now, but will be re-scheduled in " + retryWaitTime + " ms. Don't worry: " + e + " caused by: " + e.getCause()
                  .getMessage());
            Thread.sleep(retryWaitTime);
            retryWaitTime = retryWaitTime * 2;
         }

      }

   }

   private static void readBackwards() throws ReadFailedException {
      final EventResponse lastEvent = esHttpEventStore.readLastEvent(streamName);
      Long nextEventNumber = lastEvent.getContent()
            .getPositionEventNumber();
      while (true) {
         try {
            final StreamEventsSlice slice = esHttpEventStore.readEventsBackward(streamName, nextEventNumber.intValue(), 500, "");

            nextEventNumber = Long.valueOf(slice.getNextEventNumber());

            String x = "";

            if (slice.isEndOfStream()) {
               System.out.println("Head of stream reached ... end!");
               esHttpEventStore.close();
               return;
            }

            x += "Read from " + slice.getNextEventNumber() + " to " + slice.getFromEventNumber();

            System.out.println(x);
         } catch (final Exception e) {
            System.err.println("Failed to process. Job ends now, but will be re-scheduled. Don't worry:" + e);
         }
      }
   }

}
