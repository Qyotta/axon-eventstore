package de.qyotta.neweventstore;

import de.qyotta.eventstore.model.Entry;
import de.qyotta.eventstore.model.Event;
import de.qyotta.eventstore.model.EventResponse;
import de.qyotta.eventstore.utils.DefaultConnectionKeepAliveStrategy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("nls")
public final class ESHttpEventStore {

   private static final int DEFAUT_LONG_POLL = 30;

   private static final Logger LOG = LoggerFactory.getLogger(ESHttpEventStore.class);

   private final ThreadFactory threadFactory;

   private final URL url;

   private final CredentialsProvider credentialsProvider;

   private CloseableHttpAsyncClient httpclient;

   private boolean open;

   private final AtomFeedJsonReader atomFeedReader;

   private final int longPollSec;
   private final String identifier;
   private final String host;

   public ESHttpEventStore(final URL url, final CredentialsProvider credentialsProvider) {
      this("", null, url, credentialsProvider, DEFAUT_LONG_POLL);
   }

   public ESHttpEventStore(final String identifier, final URL url, final CredentialsProvider credentialsProvider) {
      this(identifier, null, url, credentialsProvider, DEFAUT_LONG_POLL);
   }

   public ESHttpEventStore(final String identifier, final ThreadFactory threadFactory, final URL url, final CredentialsProvider credentialsProvider, int longPollSec) {
      super();
      this.identifier = identifier;
      this.threadFactory = threadFactory;
      this.url = url;
      this.credentialsProvider = credentialsProvider;
      this.longPollSec = longPollSec;
      this.open = false;
      this.host = url.getHost() + ":" + url.getPort();
      atomFeedReader = new AtomFeedJsonReader();
   }

   private void open() {
      if (open) {
         // Ignore
         return;
      }
      final HttpAsyncClientBuilder builder = HttpAsyncClients.custom()
            .setMaxConnPerRoute(1000)
            .setMaxConnTotal(1000)
            .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())
            .setThreadFactory(threadFactory);
      if (credentialsProvider != null) {
         builder.setDefaultCredentialsProvider(credentialsProvider);
      }

      httpclient = builder.build();
      httpclient.start();
      this.open = true;
   }

   public void close() {
      if (!open) {
         // Ignore
         return;
      }
      try {
         httpclient.close();
      } catch (final IOException ex) {
         throw new RuntimeException("Cannot close http client", ex);
      }
      this.open = false;
   }

   public EventResponse readEvent(final String streamName, final int eventNumber) throws ReadFailedException {
      ensureOpen();

      final String msg = "readEvent(" + streamName + ", " + eventNumber + ")";
      try {
         final URI uri = new URIBuilder(url.toURI()).setPath("/streams/" + streamName + "/" + eventNumber)
               .build();
         return readEvent(uri, "");
      } catch (final URISyntaxException ex) {
         throw new ReadFailedException(streamName, msg, ex);
      }
   }

   private void ensureOpen() {
      if (!open) {
         open();
      }
   }

   public EventResponse readLastEvent(final String streamName) throws ReadFailedException {
      ensureOpen();

      final String msg = "readLastEvent(" + streamName + ")";
      try {
         final URI uri = new URIBuilder(url.toURI()).setPath("/streams/" + streamName + "/head/backward/1")
               .build();

         final List<Entry> entries = readFeed(streamName, uri, msg, "");
         final Entry entry = entries.get(0);

         return enrich(readEvent(new URI(entry.getId()), ""), entry);

      } catch (final URISyntaxException ex) {
         throw new ReadFailedException(streamName, msg, ex);
      }
   }

   public StreamEventsSlice readEventsForward(final String streamName, final int start, final int count, final String traceString) throws ReadFailedException {
      final Instant startTime = Instant.now();
      ensureOpen();

      final String msg = "readEventsForward(" + streamName + ", " + start + ", " + count + ")";
      LOG.debug("#<>#<># [" + traceString + "]" + msg);
      try {
         final URI uri = new URIBuilder(url.toURI()).setPath("/streams/" + streamName + "/" + start + "/forward/" + count)
               .build();

         LOG.debug("#<*>#<1># [" + traceString + "]" + msg + " " + uri.toString());

         final boolean reverseOrder = false;
         final boolean forward = true;

         final List<Entry> entries = readFeed(streamName, uri, msg, traceString);
         LOG.debug("#<*>#<2># [" + traceString + "] found " + entries.size() + " in feed for: " + uri.toString());
         final StreamEventsSlice slice = readEvents(forward, start, count, entries, reverseOrder, traceString);
         LOG.debug("#<*>#<3># [" + traceString + "] created slice from feed " + slice.getFromEventNumber() + "-" + slice.getNextEventNumber());
         return slice;
      } catch (final URISyntaxException ex) {
         throw new ReadFailedException(streamName, msg, ex);
      } finally {
         final Duration between = Duration.between(startTime, Instant.now());
         PrometheusMonitoringService.eventSliceDuration(between.toMillis(), streamName, identifier, host);
      }
   }

   public StreamEventsSlice readEventsBackward(final String streamName, final int start, final int count, final String traceString) throws ReadFailedException {
      ensureOpen();

      final String msg = "readEventsBackward(" + streamName + ", " + start + ", " + count + ")";
      try {
         final URI uri = new URIBuilder(url.toURI()).setPath("/streams/" + streamName + "/" + start + "/backward/" + count)
               .build();
         final boolean reverseOrder = true;
         final boolean forward = false;

         final List<Entry> entries = readFeed(streamName, uri, msg, traceString);
         final StreamEventsSlice slice = readEvents(forward, start, count, entries, reverseOrder, traceString);
         LOG.debug("#<>#<># [" + traceString + "]" + msg + " found slice from " + slice.getFromEventNumber() + "-" + slice.getNextEventNumber());
         return slice;
      } catch (final URISyntaxException ex) {
         throw new ReadFailedException(streamName, msg, ex);
      }
   }

   private List<Entry> readFeed(final String streamName, final URI uri, final String msg, final String traceString) throws ReadFailedException {
      LOG.debug("#<>#<># [" + traceString + "] reading feed from " + uri.toString());
      final HttpGet get = createHttpGet(uri);
      try {
         final Future<HttpResponse> future = httpclient.execute(get, null);
         final HttpResponse response = future.get();
         LOG.debug("#<>#<># [" + traceString + "]" + response.getStatusLine() + " for feed from " + uri.toString());
         final StatusLine statusLine = response.getStatusLine();
         if (statusLine.getStatusCode() == 200) {
            final HttpEntity entity = response.getEntity();
            try {
               final InputStream in = entity.getContent();
               try {
                  final List<Entry> entries = atomFeedReader.readAtomFeed(in);
                  LOG.debug("#<>#<># [" + traceString + "] created " + entries.size() + " entries from atomfeed");
                  return entries;
               } finally {
                  in.close();
               }
            } finally {
               EntityUtils.consume(entity);
            }
         }
         if (statusLine.getStatusCode() == 404) {
            // 404 Not Found
            LOG.debug("#<>#<># [" + traceString + "] " + msg + " RESPONSE: {}", response);
            throw new StreamNotFoundException(streamName);
         }
         if (statusLine.getStatusCode() == 410) {
            // Stream was hard deleted
            LOG.debug("#<>#<># [" + traceString + "] " + msg + " RESPONSE: {}", response);
            throw new StreamDeletedException(streamName);
         }
         throw new UnknownServerResponseException(streamName, " [Status=" + statusLine + "]");
      } catch (final Exception e) {
         throw new ReadFailedException(streamName, msg, e);
      } finally {
         get.reset();
      }
   }

   private StreamEventsSlice readEvents(final boolean forward, final int fromEventNumber, final int count, final List<Entry> entries, final boolean reverseOrder, final String traceString)
         throws ReadFailedException, URISyntaxException {
      final List<EventResponse> events = new ArrayList<>();
      if (reverseOrder) {
         for (int i = 0; i < entries.size(); i++) {
            final Entry entry = entries.get(i);
            events.add(enrich(readEvent(new URI(entry.getId()), traceString), entry));
         }
      } else {
         for (int i = entries.size() - 1; i >= 0; i--) {
            final Entry entry = entries.get(i);
            events.add(enrich(readEvent(new URI(entry.getId()), traceString), entry));
         }
      }
      final int nextEventNumber;
      final boolean endOfStream;
      if (forward) {
         nextEventNumber = fromEventNumber + events.size();
         endOfStream = count > events.size();
      } else {
         nextEventNumber = fromEventNumber - count < 0 ? 0 : fromEventNumber - count;
         endOfStream = fromEventNumber - count < 0;
      }

      return StreamEventsSlice.builder()
            .fromEventNumber(fromEventNumber)
            .nextEventNumber(nextEventNumber)
            .events(events)
            .endOfStream(endOfStream)
            .build();
   }

   private EventResponse enrich(EventResponse event, Entry entry) {
      final Event content = event.getContent();
      content.setEventId(entry.getEventId());
      content.setEventType(entry.getEventType());
      content.setEventNumber(entry.getEventNumber());
      content.setStreamId(entry.getStreamId());
      content.setIsLinkMetaData(entry.getIsLinkMetaData());
      content.setPositionEventNumber(entry.getPositionEventNumber());
      content.setPositionStreamId(entry.getPositionStreamId());
      content.setTitle(entry.getTitle());
      content.setId(entry.getId());
      content.setUpdated(entry.getUpdated());
      content.setUpdated(entry.getUpdated());
      content.setAuthor(entry.getAuthor());
      content.setSummary(entry.getSummary());
      return event;
   }

   private EventResponse readEvent(final URI uri, final String traceString) throws ReadFailedException {
      final Instant start = Instant.now();
      final String streamName = streamName(uri);
      try {

         LOG.debug("#<>#<># [" + traceString + "] reading event from " + uri);

         LOG.debug(uri.toString());
         final String msg = "readEvent(" + uri + ")";

         final HttpGet get = createHttpGet(uri);
         try {
            final Instant requestStart = Instant.now();
            final Future<HttpResponse> future = httpclient.execute(get, null);
            final HttpResponse response = future.get();
            final Duration duration = Duration.between(requestStart, Instant.now());
            PrometheusMonitoringService.eventRequestDuration(duration.toMillis(), streamName, identifier, host);
            final StatusLine statusLine = response.getStatusLine();
            LOG.debug("#<>#<># [" + traceString + "] reading event from " + uri + " with statusCode " + statusLine);
            if (statusLine.getStatusCode() == 200) {
               final HttpEntity entity = response.getEntity();
               try {
                  final InputStream in = entity.getContent();
                  try {
                     final EventResponse eventResponse = atomFeedReader.readEvent(in);
                     LOG.debug("#<>#<># [" + traceString + "] reading event from " + uri + " with response " + eventResponse);
                     return eventResponse;
                  } finally {
                     in.close();
                  }
               } finally {
                  EntityUtils.consume(entity);
               }
            }
            if (statusLine.getStatusCode() == 404) {
               // 404 Not Found
               LOG.debug(msg + " RESPONSE: {}", response);
               final int eventNumber = eventNumber(uri);
               throw new EventNotFoundException(streamName, eventNumber);
            }
            throw new ReadFailedException(streamName, msg + " [Status=" + statusLine + "]");

         } catch (final Exception e) {
            throw new ReadFailedException(streamName, msg, e);
         } finally {
            get.reset();
         }
      } finally {
         final Instant end = Instant.now();
         PrometheusMonitoringService.eventReadDuration(Duration.between(start, end)
               .toMillis(), streamName, identifier, host);
      }
   }

   private String streamName(final URI uri) {
      // http://127.0.0.1:2113/streams/append_diff_and_read_stream/2
      final String myurl = uri.toString();
      final int p1 = myurl.indexOf("/streams/");
      if (p1 == -1) {
         throw new IllegalStateException("Failed to extract '/streams/': " + uri);
      }
      final int p2 = myurl.lastIndexOf('/');
      if (p2 == -1) {
         throw new IllegalStateException("Failed to extract last '/': " + uri);
      }
      final String str = myurl.substring(p1 + 9, p2);
      return str;
   }

   private int eventNumber(final URI uri) {
      // http://127.0.0.1:2113/streams/append_diff_and_read_stream/2
      final String myurl = uri.toString();
      final int p = myurl.lastIndexOf('/');
      if (p == -1) {
         throw new IllegalStateException("Failed to extract event number: " + uri);
      }
      final String str = myurl.substring(p + 1);
      return Integer.valueOf(str);
   }

   private HttpGet createHttpGet(final URI uri) {
      final HttpGet request = new HttpGet(uri + "?embed=rich");
      request.setHeader("Accept", "application/vnd.eventstore.atom+json");
      request.setHeader("ES-LongPoll", String.valueOf(longPollSec));
      return request;
   }
}
